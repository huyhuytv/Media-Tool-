package com.example.core.ml

import android.content.Context
import android.net.Uri
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
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
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
        private const val BYTES_PER_FRAME = CHANNELS * 4
    }

    suspend fun separate(inputUri: Uri): Flow<SeparationState> = flow {
    	emit(SeparationState.Progress(0.01f)) // Start

        // 1. Prepare temp files
        val cacheDir = context.cacheDir
        val tempRawMix = File(cacheDir, "mix.raw")
        val tempRawVocals = File(cacheDir, "vocals.raw")
        val tempRawMusic = File(cacheDir, "music.raw")
        
        try {
            tempRawMix.delete()
            tempRawVocals.delete()
            tempRawMusic.delete()

            // 2. Decode input to raw float PCM (f32le)
            emit(SeparationState.Progress(0.05f))
            val inputPath = com.arthenica.ffmpegkit.FFmpegKitConfig.getSafParameterForRead(context, inputUri)
            val decodeCmd = "-y -i \"$inputPath\" -f f32le -ac $CHANNELS -ar $SAMPLE_RATE \"${tempRawMix.absolutePath}\""
            val decodeSession = FFmpegKit.execute(decodeCmd)
            
            if (!ReturnCode.isSuccess(decodeSession.returnCode)) {
                val logs = decodeSession.allLogsAsString
                throw Exception("Lỗi khi giải mã audio (FFmpeg). Chi tiết: $logs")
            }

            emit(SeparationState.Progress(0.1f)) // Decode complete

            // 3. Process with ONNX
            val env = OrtEnvironment.getEnvironment()
            val sessionOptions = OrtSession.SessionOptions().apply {
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.BASIC_OPT)
                setIntraOpNumThreads(4) // Limit threads to avoid CPU starvation and OOM on mobile
            }
            // Optional: Optimize for memory/speed
            
            val session = env.createSession(modelFile.absolutePath, sessionOptions)

            try {
                val totalBytes = tempRawMix.length()
                val totalFrames = totalBytes / BYTES_PER_FRAME
                
                // 3.1 COMPUTE GLOBAL MEAN AND STD (Single pass fast computation)
                emit(SeparationState.Progress(0.12f))
                var globalMonoSum = 0.0
                var globalMonoVarSum = 0.0
                var globalFramesCount = 0L
                DataInputStream(FileInputStream(tempRawMix)).use { statStream ->
                    val statBuf = ByteArray(8192 * BYTES_PER_FRAME)
                    while (true) {
                        val read = statStream.read(statBuf)
                        if (read <= 0) break
                        val framesReadStat = read / BYTES_PER_FRAME
                        val fbuf = ByteBuffer.wrap(statBuf, 0, read).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer()
                        for (i in 0 until framesReadStat) {
                            val mono = (fbuf.get(i * CHANNELS + 0) + fbuf.get(i * CHANNELS + 1)) / 2.0
                            globalMonoSum += mono
                            globalMonoVarSum += mono * mono
                        }
                        globalFramesCount += framesReadStat
                    }
                }
                
                val globalMean = if (globalFramesCount > 0) globalMonoSum / globalFramesCount else 0.0
                var globalVariance = if (globalFramesCount > 0) (globalMonoVarSum / globalFramesCount) - (globalMean * globalMean) else 1.0
                if (globalVariance < 0) globalVariance = 0.0
                val stdF = Math.sqrt(globalVariance).toFloat()
                val meanF = globalMean.toFloat()

                var processedFrames = 0L

                val inputStream = DataInputStream(FileInputStream(tempRawMix))
                val vocalsOut = DataOutputStream(FileOutputStream(tempRawVocals))
                val musicOut = DataOutputStream(FileOutputStream(tempRawMusic))

                // Sửa lỗi #2: Thêm thông số gối (Overlap) để không bị đứt quãng (Crossfade 25%)
                val overlapSize = (CHUNK_FRAMES * 0.25f).toInt()
                val stepSize = CHUNK_FRAMES - overlapSize

                val chunkBufferBytes = ByteArray(CHUNK_FRAMES * BYTES_PER_FRAME)
                val chunkBuffer = FloatArray(CHUNK_FRAMES * CHANNELS)
                
                // Buffer lưu các đoạn âm thanh nối (Overlap)
                val outVocalsOverlap = FloatArray(overlapSize * CHANNELS)
                val outMusicOverlap = FloatArray(overlapSize * CHANNELS)

                val vocalsMerged = ByteBuffer.allocate(CHUNK_FRAMES * BYTES_PER_FRAME).order(ByteOrder.LITTLE_ENDIAN)
                val musicMerged = ByteBuffer.allocate(CHUNK_FRAMES * BYTES_PER_FRAME).order(ByteOrder.LITTLE_ENDIAN)
                
                val tensorBuffer = FloatBuffer.allocate(CHANNELS * CHUNK_FRAMES)

                var isFirstChunk = true

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

                    if (actualFramesRead == 0 && !isFirstChunk) {
                        // EOF: Xả nốt đoạn overlap cuối cùng
                        vocalsMerged.clear()
                        musicMerged.clear()
                        for(i in 0 until overlapSize * CHANNELS) {
                            vocalsMerged.putFloat(outVocalsOverlap[i])
                            musicMerged.putFloat(outMusicOverlap[i])
                        }
                        vocalsOut.write(vocalsMerged.array(), 0, overlapSize * BYTES_PER_FRAME)
                        musicOut.write(musicMerged.array(), 0, overlapSize * BYTES_PER_FRAME)
                        break
                    }
                    if (actualFramesRead == 0 && isFirstChunk) break // File rỗng

                    if (!isFirstChunk) {
                        // Dịch dữ liệu cũ sang trái
                        System.arraycopy(chunkBuffer, stepSize * CHANNELS, chunkBuffer, 0, overlapSize * CHANNELS)
                    }

                    val floatBuffer = ByteBuffer.wrap(chunkBufferBytes, 0, bytesRead)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .asFloatBuffer()

                    val offset = if (isFirstChunk) 0 else overlapSize * CHANNELS
                    for (i in 0 until actualFramesRead * CHANNELS) {
                        chunkBuffer[offset + i] = floatBuffer.get(i)
                    }

                    val validFramesInChunk = if (isFirstChunk) actualFramesRead else (overlapSize + actualFramesRead)
                    
                    // Chuẩn hóa và làm phẳng sang Tensor
                    tensorBuffer.clear()
                    for (ch in 0 until CHANNELS) {
                        for (f in 0 until validFramesInChunk) {
                            val originalVal = chunkBuffer[f * CHANNELS + ch]
                            val normalizedVal = (originalVal - meanF) / (stdF + 1e-5f)
                            tensorBuffer.put(ch * CHUNK_FRAMES + f, normalizedVal)
                        }
                        // Zero padding nếu file bị thiếu
                        for (f in validFramesInChunk until CHUNK_FRAMES) {
                            tensorBuffer.put(ch * CHUNK_FRAMES + f, 0f)
                        }
                    }

                    // Create ONNX Tensor
                    // Shape: [1 (batch), 2 (channels), frames]
                    val shape = longArrayOf(1, CHANNELS.toLong(), CHUNK_FRAMES.toLong())
                    val inputTensor = OnnxTensor.createTensor(env, tensorBuffer, shape)

                    val inputName = session.inputNames.iterator().next()
                    val inputMap = mapOf(inputName to inputTensor)

                    // Run inference
                    val result = session.run(inputMap)
                    
                    val outputTensor = result.get(0).value as Array<*> // [1][4][2][framesRead]
                    val batchOut = outputTensor[0] as Array<*> // [4][2][framesRead]

                    // Assuming Stem order: 0=drums, 1=bass, 2=other, 3=vocals
                    val drums = batchOut[0] as Array<FloatArray>
                    val bass = batchOut[1] as Array<FloatArray>
                    val other = batchOut[2] as Array<FloatArray>
                    val vocals = batchOut[3] as Array<FloatArray>
                    
                    val isFullRead = (isFirstChunk && actualFramesRead == CHUNK_FRAMES) || (!isFirstChunk && actualFramesRead == stepSize)
                    val framesToWrite = if (isFullRead) stepSize else validFramesInChunk

                    vocalsMerged.clear()
                    musicMerged.clear()

                    for (f in 0 until framesToWrite) {
                        for (ch in 0 until CHANNELS) {
                            // Un-normalize the output từ mô hình.
                            // FIX ERROR 4 & 5: Dùng công thức đúng. Tránh cộng Mean quá nhiều lần!
                            var m_val = (drums[ch][f] + bass[ch][f] + other[ch][f]) * stdF + meanF
                            var v_val = vocals[ch][f] * stdF + meanF
                            
                            // FIX ERROR 2: Crossfade Overlap-add để chống méo đoạn nối
                            if (f < overlapSize && !isFirstChunk) {
                                val weight = f.toFloat() / overlapSize
                                m_val = m_val * weight + outMusicOverlap[f * CHANNELS + ch]
                                v_val = v_val * weight + outVocalsOverlap[f * CHANNELS + ch]
                            }

                            // FIX ERROR 3: Soft Clipping (Tanh) chống san phẳng gai âm thanh, giảm méo điện tử
                            val vClipped = kotlin.math.tanh(v_val.toDouble()).toFloat()
                            val mClipped = kotlin.math.tanh(m_val.toDouble()).toFloat()
                            
                            vocalsMerged.putFloat(vClipped)
                            musicMerged.putFloat(mClipped)
                        }
                    }

                    vocalsOut.write(vocalsMerged.array(), 0, framesToWrite * BYTES_PER_FRAME)
                    musicOut.write(musicMerged.array(), 0, framesToWrite * BYTES_PER_FRAME)
                    
                    // Lưu vùng chéo Overlap cho phần sau
                    if (isFullRead) {
                        for (f in framesToWrite until CHUNK_FRAMES) {
                            for (ch in 0 until CHANNELS) {
                                var m_val = (drums[ch][f] + bass[ch][f] + other[ch][f]) * stdF + meanF
                                var v_val = vocals[ch][f] * stdF + meanF

                                val rightWeight = (CHUNK_FRAMES - f).toFloat() / overlapSize
                                m_val *= rightWeight
                                v_val *= rightWeight

                                val overIdx = f - framesToWrite
                                outMusicOverlap[overIdx * CHANNELS + ch] = m_val
                                outVocalsOverlap[overIdx * CHANNELS + ch] = v_val
                            }
                        }
                    }

                    inputTensor.close()
                    result.close()

                    processedFrames += actualFramesRead
                    val progress = 0.12f + 0.78f * (processedFrames.toFloat() / totalFrames.toFloat())
                    emit(SeparationState.Progress(progress.coerceAtMost(0.88f)))
                    
                    isFirstChunk = false
                }

                inputStream.close()
                vocalsOut.close()
                musicOut.close()
                session.close()

            } catch (e: Exception) {
                session.close()
                Log.e(TAG, "Lỗi khi tách: ${e.message}")
                throw Exception("Lỗi khi xử lý mô hình AI: ${e.message}", e)
            }

            emit(SeparationState.Progress(0.9f)) // Encoding 

            // 4. Encode raw PCM back to mp3 (High Quality 320kbps)
            val outVocals = File(context.filesDir, "vocals_${System.currentTimeMillis()}.mp3")
            val outMusic = File(context.filesDir, "music_${System.currentTimeMillis()}.mp3")

            val encVocalCmd = "-y -f f32le -ac $CHANNELS -ar $SAMPLE_RATE -i \"${tempRawVocals.absolutePath}\" -c:a libmp3lame -b:a 320k \"${outVocals.absolutePath}\""
            val encMusicCmd = "-y -f f32le -ac $CHANNELS -ar $SAMPLE_RATE -i \"${tempRawMusic.absolutePath}\" -c:a libmp3lame -b:a 320k \"${outMusic.absolutePath}\""

            val res1 = FFmpegKit.execute(encVocalCmd)
            val res2 = FFmpegKit.execute(encMusicCmd)

            if (!ReturnCode.isSuccess(res1.returnCode) || !ReturnCode.isSuccess(res2.returnCode)) {
                throw Exception("Lỗi khi xuất file mp3")
            }

            emit(SeparationState.Progress(1.0f))
            emit(SeparationState.Success(outVocals, outMusic))

        } finally {
            // 5. Cleanup temp raw files absolutely
            tempRawMix.delete()
            tempRawVocals.delete()
            tempRawMusic.delete()
        }
    }.flowOn(Dispatchers.IO)
}
