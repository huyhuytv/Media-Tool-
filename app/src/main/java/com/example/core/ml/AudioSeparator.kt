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
        // Chunk size: 4 seconds of 44100Hz audio. Reduced to 4 seconds to prevent OOM on mobile.
        private const val CHUNK_FRAMES = 44100 * 4
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
            }
            // Optional: Optimize for memory/speed
            
            val session = env.createSession(modelFile.absolutePath, sessionOptions)

            try {
                val totalBytes = tempRawMix.length()
                val totalFrames = totalBytes / BYTES_PER_FRAME
                var processedFrames = 0L

                val inputStream = DataInputStream(FileInputStream(tempRawMix))
                val vocalsOut = DataOutputStream(FileOutputStream(tempRawVocals))
                val musicOut = DataOutputStream(FileOutputStream(tempRawMusic))

                // Buffer for reading interleaved floats
                val inputBufferBytes = ByteArray(CHUNK_FRAMES * BYTES_PER_FRAME)
                val tensorBuffer = FloatBuffer.allocate(CHANNELS * CHUNK_FRAMES)

                while (coroutineContext.isActive) {
                    // Read a chunk
                    var bytesRead = 0
                    while (bytesRead < inputBufferBytes.size) {
                        val read = inputStream.read(inputBufferBytes, bytesRead, inputBufferBytes.size - bytesRead)
                        if (read == -1) break
                        bytesRead += read
                    }

                    if (bytesRead == 0) break

                    val framesRead = bytesRead / BYTES_PER_FRAME
                    val floatBuffer = ByteBuffer.wrap(inputBufferBytes, 0, bytesRead)
                        .order(ByteOrder.LITTLE_ENDIAN)
                        .asFloatBuffer()

                    // De-interleave: L, R, L, R -> LLLL..., RRRR...
                    tensorBuffer.clear()
                    for (ch in 0 until CHANNELS) {
                        for (f in 0 until framesRead) {
                            tensorBuffer.put(ch * framesRead + f, floatBuffer.get(f * CHANNELS + ch))
                        }
                    }

                    // Create ONNX Tensor
                    // Shape: [1 (batch), 2 (channels), frames]
                    val shape = longArrayOf(1, CHANNELS.toLong(), framesRead.toLong())
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

                    // Interleave and write back
                    val vocalsMerged = ByteBuffer.allocate(framesRead * BYTES_PER_FRAME).order(ByteOrder.LITTLE_ENDIAN)
                    val musicMerged = ByteBuffer.allocate(framesRead * BYTES_PER_FRAME).order(ByteOrder.LITTLE_ENDIAN)

                    for (f in 0 until framesRead) {
                        for (ch in 0 until CHANNELS) {
                            val v = vocals[ch][f]
                            val m = drums[ch][f] + bass[ch][f] + other[ch][f]
                            
                            vocalsMerged.putFloat(v)
                            musicMerged.putFloat(m)
                        }
                    }

                    vocalsOut.write(vocalsMerged.array(), 0, framesRead * BYTES_PER_FRAME)
                    musicOut.write(musicMerged.array(), 0, framesRead * BYTES_PER_FRAME)

                    inputTensor.close()
                    result.close()

                    processedFrames += framesRead
                    val progress = 0.1f + 0.8f * (processedFrames.toFloat() / totalFrames.toFloat())
                    emit(SeparationState.Progress(progress))
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

            // 4. Encode raw PCM back to mp3
            val outVocals = File(context.filesDir, "vocals_${System.currentTimeMillis()}.mp3")
            val outMusic = File(context.filesDir, "music_${System.currentTimeMillis()}.mp3")

            val encVocalCmd = "-y -f f32le -ac $CHANNELS -ar $SAMPLE_RATE -i \"${tempRawVocals.absolutePath}\" -c:a libmp3lame -q:a 2 \"${outVocals.absolutePath}\""
            val encMusicCmd = "-y -f f32le -ac $CHANNELS -ar $SAMPLE_RATE -i \"${tempRawMusic.absolutePath}\" -c:a libmp3lame -q:a 2 \"${outMusic.absolutePath}\""

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
