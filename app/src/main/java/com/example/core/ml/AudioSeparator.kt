package com.example.core.ml

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.ReturnCode
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.FileWriter
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.coroutineContext

sealed class SeparationState {
    data class Progress(val value: Float) : SeparationState()
    data class Success(val vocalsFile: File, val musicFile: File) : SeparationState()
}

class AudioSeparator(private val context: Context, private val modelFile: File) {

    companion object {
        private const val TAG = "AudioSeparator"
        private const val SAMPLE_RATE = 44100
        private const val CHANNELS = 2
        // Chunk size: Demucs ONNX model expects EXACTLY 343980 frames (approx 7.8 seconds).
        private const val CHUNK_FRAMES = 343980
        private const val BYTES_PER_FRAME = CHANNELS * 2 // s16le = 2 bytes / sample
    }

    private fun log(message: String) {
        Log.d(TAG, message)
        try {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null && (downloadsDir.exists() || downloadsDir.mkdirs())) {
                val logFile = File(downloadsDir, "audio_separator_log.txt")
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                val logMessage = "[$timestamp] $message\n"
                val writer = FileWriter(logFile, true)
                writer.append(logMessage)
                writer.flush()
                writer.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing log to file: ${e.message}", e)
        }
    }

    private fun logError(message: String, error: Throwable? = null) {
        Log.e(TAG, message, error)
        try {
            val downloadsDir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            if (downloadsDir != null && (downloadsDir.exists() || downloadsDir.mkdirs())) {
                val logFile = File(downloadsDir, "audio_separator_log.txt")
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                val logMessage = "[$timestamp] ERROR: $message | Exception: ${error?.message}\n${error?.stackTraceToString() ?: ""}\n"
                val writer = FileWriter(logFile, true)
                writer.append(logMessage)
                writer.flush()
                writer.close()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing log to file: ${e.message}", e)
        }
    }

    suspend fun separate(inputUri: Uri): Flow<SeparationState> = flow {
    	emit(SeparationState.Progress(0.01f)) // Start

        // 1. Prepare temp files
        val cacheDir = context.cacheDir
        val tempRawMix = File(cacheDir, "mix.raw")
        val tempRawVocals = File(cacheDir, "vocals.raw")
        val tempRawMusic = File(cacheDir, "music.raw")
        
        log("Bắt đầu xử lý tách audio. Model: ${modelFile.absolutePath}, InputUri: $inputUri")
        try {
            // Delete previous log file to start fresh if needed, or keep appending. 
            // We append. Let's just log start.
            
            tempRawMix.delete()
            tempRawVocals.delete()
            tempRawMusic.delete()

            // 2. Decode input to raw s16 PCM (s16le)
            emit(SeparationState.Progress(0.05f))
            val inputPath = com.arthenica.ffmpegkit.FFmpegKitConfig.getSafParameterForRead(context, inputUri)
            val decodeCmd = "-y -i \"$inputPath\" -f s16le -ac $CHANNELS -ar $SAMPLE_RATE \"${tempRawMix.absolutePath}\""
            log("Chạy FFmpeg decode: $decodeCmd")
            val decodeSession = FFmpegKit.execute(decodeCmd)
            
            if (!ReturnCode.isSuccess(decodeSession.returnCode)) {
                val logs = decodeSession.allLogsAsString
                logError("Lỗi giải mã. Logs: $logs")
                throw Exception("Lỗi khi giải mã audio (FFmpeg). Chi tiết: $logs")
            }
            log("FFmpeg decode thành công. Size: ${tempRawMix.length()} bytes")

            emit(SeparationState.Progress(0.1f)) // Decode complete

            // 3. Process with ONNX
            val env = OrtEnvironment.getEnvironment()
            val sessionOptions = OrtSession.SessionOptions().apply {
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
                setIntraOpNumThreads(4) // Limit threads to avoid CPU starvation and OOM on mobile
            }
            
            val session = env.createSession(modelFile.absolutePath, sessionOptions)

            try {
                log("Khởi tạo model ONNX thành công.")
                val totalBytes = tempRawMix.length()
                val totalFrames = totalBytes / BYTES_PER_FRAME
                log("Tổng số frames audio gốc: $totalFrames (Total bytes: $totalBytes)")
                
                var processedFrames = 0L

                val inputStream = DataInputStream(FileInputStream(tempRawMix))
                val vocalsOut = DataOutputStream(FileOutputStream(tempRawVocals))
                val musicOut = DataOutputStream(FileOutputStream(tempRawMusic))

                // Overlap 25% to smooth cuts
                val overlapSize = (CHUNK_FRAMES * 0.25f).toInt()
                val stepSize = CHUNK_FRAMES - overlapSize

                val chunkBufferBytes = ByteArray(CHUNK_FRAMES * BYTES_PER_FRAME)
                val chunkBufferFloat = FloatArray(CHUNK_FRAMES * CHANNELS)
                
                // Buffer lưu các đoạn âm thanh nối (Overlap)
                val outVocalsOverlap = FloatArray(overlapSize * CHANNELS)
                val outBeatOverlap = FloatArray(overlapSize * CHANNELS)

                val vocalsMerged = ByteBuffer.allocate(CHUNK_FRAMES * BYTES_PER_FRAME).order(ByteOrder.LITTLE_ENDIAN)
                val musicMerged = ByteBuffer.allocate(CHUNK_FRAMES * BYTES_PER_FRAME).order(ByteOrder.LITTLE_ENDIAN)

                val inputName = session.inputNames.iterator().next()
                val inInfo = session.inputInfo[inputName]?.info as? ai.onnxruntime.TensorInfo
                val expectedShape = inInfo?.shape
                
                log("Input Expected Shape: ${expectedShape?.joinToString(", ") ?: "Unknown"}")
                
                var inShape = longArrayOf(1, CHANNELS.toLong(), CHUNK_FRAMES.toLong())
                var inCAxis = 1
                var inFAxis = 2
                
                if (expectedShape != null && expectedShape.size == 3 && expectedShape[1] > 100 && expectedShape[2] == 2L) {
                    inShape = longArrayOf(1, CHUNK_FRAMES.toLong(), CHANNELS.toLong())
                    inFAxis = 1
                    inCAxis = 2
                    log("CẢNH BÁO: Model Input Shape [1, frames, channels]. Đã tự xoay trục.")
                }

                val inStrides = LongArray(3)
                inStrides[2] = 1L
                inStrides[1] = inShape[2]
                inStrides[0] = inShape[1] * inShape[2]

                // Tái sử dụng Buffer để giảm GC Overhead & cấp phát RAM liên tục
                val inputBuffer = FloatBuffer.allocate(CHANNELS * CHUNK_FRAMES)

                var isFirstChunk = true
                var chunkIndex = 0

                while (coroutineContext.isActive) {
                    val framesToRead = if (isFirstChunk) CHUNK_FRAMES else stepSize
                    val bytesToRead = framesToRead * BYTES_PER_FRAME
                    
                    var bytesRead = 0
                    while (bytesRead < bytesToRead) {
                        val read = inputStream.read(chunkBufferBytes, bytesRead, bytesToRead - bytesRead)
                        if (read == -1) break
                        bytesRead += read
                    }
                    
                    val actualFramesRead = bytesRead / BYTES_PER_FRAME
                    log("Chunk $chunkIndex: Đọc được $actualFramesRead frames ($bytesRead bytes)")

                    if (actualFramesRead == 0 && !isFirstChunk) {
                        log("EOF: Xả nốt đoạn overlap cuối cùng (size $overlapSize frames)")
                        // EOF: Xả nốt đoạn overlap cuối cùng.
                        vocalsMerged.clear()
                        musicMerged.clear()
                        for(i in 0 until overlapSize * CHANNELS) {
                            val frameIdxInOverlap = i / CHANNELS
                            val rightWeight = (overlapSize - frameIdxInOverlap).toFloat() / overlapSize
                            val invWeight = if (rightWeight > 0.001f) 1.0f / rightWeight else 1.0f
                            
                            val v_val = outVocalsOverlap[i] * invWeight
                            val m_val = outBeatOverlap[i] * invWeight
                            
                            val vShort = (v_val * 32768.0f).toInt().coerceIn(-32768, 32767).toShort()
                            val mShort = (m_val * 32768.0f).toInt().coerceIn(-32768, 32767).toShort()
                            
                            vocalsMerged.putShort(vShort)
                            musicMerged.putShort(mShort)
                        }
                        vocalsOut.write(vocalsMerged.array(), 0, overlapSize * BYTES_PER_FRAME)
                        musicOut.write(musicMerged.array(), 0, overlapSize * BYTES_PER_FRAME)
                        break
                    }
                    if (actualFramesRead == 0 && isFirstChunk) break // File rỗng

                    if (!isFirstChunk) {
                        // Dịch dữ liệu cũ sang trái
                        System.arraycopy(chunkBufferFloat, stepSize * CHANNELS, chunkBufferFloat, 0, overlapSize * CHANNELS)
                    }

                    val shortBuffer = ByteBuffer.wrap(chunkBufferBytes, 0, bytesRead)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .asShortBuffer()

                    val offset = if (isFirstChunk) 0 else overlapSize * CHANNELS
                    for (i in 0 until actualFramesRead * CHANNELS) {
                        chunkBufferFloat[offset + i] = shortBuffer.get(i) / 32768.0f
                    }

                    val validFramesInChunk = if (isFirstChunk) actualFramesRead else (overlapSize + actualFramesRead)
                    val isFullRead = (isFirstChunk && actualFramesRead == CHUNK_FRAMES) || (!isFirstChunk && actualFramesRead == stepSize)
                    val framesToWrite = if (isFullRead) stepSize else validFramesInChunk
                    
                    inputBuffer.clear()
                    for (ch in 0 until CHANNELS) {
                        for (f in 0 until validFramesInChunk) {
                            val idx = ch * inStrides[inCAxis] + f * inStrides[inFAxis]
                            inputBuffer.put(idx.toInt(), chunkBufferFloat[f * CHANNELS + ch])
                        }
                    }
                    inputBuffer.rewind()

                    val inputTensor = OnnxTensor.createTensor(env, inputBuffer, inShape)
                    var result: ai.onnxruntime.OrtSession.Result? = null
                    
                    try {
                        val inputMap = mapOf(inputName to inputTensor)

                        // Inference
                        log("Chunk $chunkIndex: Bắt đầu Inference ONNX...")
                        result = session.run(inputMap)
                        log("Chunk $chunkIndex: ONNX Inference hoàn tất.")
                        
                        val outOnnxTensor = result.get(0) as OnnxTensor
                        val outInfo = outOnnxTensor.info as ai.onnxruntime.TensorInfo
                        val outShape = outInfo.shape
                        
                        if (chunkIndex == 0) {
                            log("Output Tensor Shape: ${outShape.joinToString(", ")}")
                            try {
                                val memUsage = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
                                log("Memory usage: ${memUsage / 1024 / 1024} MB")
                            } catch (e: Exception) {}
                        }

                        var sAxis = 1
                        var cAxis = 2
                        var fAxis = 3
                        
                        if (outShape.size == 4 && outShape[2] > 100 && outShape[3] == 2L) {
                            cAxis = 3
                            fAxis = 2
                            if (chunkIndex == 0) log("CẢNH BÁO: Model Output Shape [1, nguồn, frames, channels]. Đã tự động xoay.")
                        }

                        val outStrides = LongArray(outShape.size)
                        var currentStr = 1L
                        for(i in outShape.indices.reversed()) {
                            outStrides[i] = currentStr
                            currentStr *= outShape[i]
                        }

                        val outBuffer = outOnnxTensor.floatBuffer
                        val sourceCount = outShape[sAxis].toInt()
                        val vocalIdx = if (sourceCount >= 4) 3 else sourceCount - 1
                        
                        vocalsMerged.clear()
                        musicMerged.clear()

                        for (f in 0 until framesToWrite) {
                            for (ch in 0 until CHANNELS) {
                                val vOffset = vocalIdx * outStrides[sAxis] + ch * outStrides[cAxis] + f * outStrides[fAxis]
                                var v_val = outBuffer.get(vOffset.toInt())
                                var m_val = 0f
                                
                                for (s in 0 until sourceCount) {
                                    if (s != vocalIdx) {
                                        val sOffset = s * outStrides[sAxis] + ch * outStrides[cAxis] + f * outStrides[fAxis]
                                        m_val += outBuffer.get(sOffset.toInt())
                                    }
                                }
                                
                                // Crossfade Overlap-add
                                if (f < overlapSize && !isFirstChunk) {
                                    val weight = f.toFloat() / overlapSize
                                    v_val = v_val * weight + outVocalsOverlap[f * CHANNELS + ch]
                                    m_val = m_val * weight + outBeatOverlap[f * CHANNELS + ch]
                                }

                                // Chuyển về Int16 Interleaved
                                val vShort = (v_val * 32768.0f).toInt().coerceIn(-32768, 32767).toShort()
                                val mShort = (m_val * 32768.0f).toInt().coerceIn(-32768, 32767).toShort()
                                
                                vocalsMerged.putShort(vShort)
                                musicMerged.putShort(mShort)
                            }
                        }

                        vocalsOut.write(vocalsMerged.array(), 0, framesToWrite * BYTES_PER_FRAME)
                        musicOut.write(musicMerged.array(), 0, framesToWrite * BYTES_PER_FRAME)
                        log("Chunk $chunkIndex: Ghi $framesToWrite frames ra file.")
                        
                        // Lưu Overlap cho chunk kế tiếp
                        if (isFullRead) {
                            for (f in framesToWrite until CHUNK_FRAMES) {
                                for (ch in 0 until CHANNELS) {
                                    val vOffset = vocalIdx * outStrides[sAxis] + ch * outStrides[cAxis] + f * outStrides[fAxis]
                                    var v_val = outBuffer.get(vOffset.toInt())
                                    var m_val = 0f
                                    for (s in 0 until sourceCount) {
                                        if (s != vocalIdx) {
                                            val sOffset = s * outStrides[sAxis] + ch * outStrides[cAxis] + f * outStrides[fAxis]
                                            m_val += outBuffer.get(sOffset.toInt())
                                        }
                                    }

                                    val rightWeight = (CHUNK_FRAMES - f).toFloat() / overlapSize
                                    v_val *= rightWeight
                                    m_val *= rightWeight

                                    val overIdx = f - framesToWrite
                                    outVocalsOverlap[overIdx * CHANNELS + ch] = v_val
                                    outBeatOverlap[overIdx * CHANNELS + ch] = m_val
                                }
                            }
                        }
                    } finally {
                        result?.close()
                        inputTensor.close()
                    }

                    processedFrames += actualFramesRead
                    val progressRatio = if (totalFrames > 0) processedFrames.toFloat() / totalFrames.toFloat() else 1.0f
                    val progress = 0.12f + 0.78f * progressRatio
                    emit(SeparationState.Progress(progress.coerceAtMost(0.88f)))
                    
                    isFirstChunk = false
                    chunkIndex++
                    
                    if (!isFullRead) {
                        log("Hoàn tất duyệt file ở frame cuối cùng.")
                        break
                    }
                }

                inputStream.close()
                vocalsOut.close()
                musicOut.close()

            } catch (e: Exception) {
                logError("Lỗi khi tách: ${e.message}", e)
                throw Exception("Lỗi khi xử lý mô hình AI: ${e.message}", e)
            } finally {
                session.close()
            }

            emit(SeparationState.Progress(0.9f)) // Encoding 

            // 4. Encode raw PCM back to mp3 (High Quality 320kbps)
            val outVocals = File(context.filesDir, "vocals_${System.currentTimeMillis()}.mp3")
            val outMusic = File(context.filesDir, "music_${System.currentTimeMillis()}.mp3")

            log("Bắt đầu Encode raw PCM sang MP3.")
            val encVocalCmd = "-y -f s16le -ac $CHANNELS -ar $SAMPLE_RATE -i \"${tempRawVocals.absolutePath}\" -c:a libmp3lame -b:a 320k \"${outVocals.absolutePath}\""
            val encMusicCmd = "-y -f s16le -ac $CHANNELS -ar $SAMPLE_RATE -i \"${tempRawMusic.absolutePath}\" -c:a libmp3lame -b:a 320k \"${outMusic.absolutePath}\""

            val res1 = FFmpegKit.execute(encVocalCmd)
            val res2 = FFmpegKit.execute(encMusicCmd)

            if (!ReturnCode.isSuccess(res1.returnCode) || !ReturnCode.isSuccess(res2.returnCode)) {
                logError("Lỗi export FFmpeg - Vocals: ${res1.allLogsAsString}")
                logError("Lỗi export FFmpeg - Music: ${res2.allLogsAsString}")
                throw Exception("Lỗi khi xuất file mp3")
            }
            log("Hoàn tất tách audio. Vocals: ${outVocals.absolutePath}, Music: ${outMusic.absolutePath}")

            emit(SeparationState.Progress(1.0f))
            emit(SeparationState.Success(outVocals, outMusic))

        } finally {
            // 5. Cleanup
            tempRawMix.delete()
            tempRawVocals.delete()
            tempRawMusic.delete()
        }
    }.flowOn(Dispatchers.IO)
}
