package com.example.core.media

import android.content.Context
import android.net.Uri
import com.arthenica.ffmpegkit.FFmpegKit
import com.arthenica.ffmpegkit.FFmpegKitConfig
import com.arthenica.ffmpegkit.FFmpegSession
import com.arthenica.ffmpegkit.ReturnCode
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

/**
 * Lõi xử lý đa phương tiện.
 * Đã chuyển sang sử dụng FFmpegKit (phiên bản Full LTS) để đảm bảo chất lượng âm thanh 
 * được xử lý ở mức cao nhất, hỗ trợ nhiều codec và tùy chỉnh chuyên sâu.
 */
class MediaEngine(private val context: Context) {

    /**
     * Trạng thái tiến trình chạy lệnh FFmpeg.
     */
    sealed class ExecutionState {
        object Connecting : ExecutionState()
        data class Progress(val timeInMilliseconds: Long, val size: Long, val bitrate: Double) : ExecutionState()
        data class Success(val outputLog: String) : ExecutionState()
        data class Error(val returnCode: ReturnCode?, val failStackTrace: String?) : ExecutionState()
    }

    /**
     * Thực thi lệnh FFmpeg bất kỳ và trả về Flow trạng thái tiến trình.
     * Cung cấp khả năng tùy chỉnh lệnh chuyên sâu, ví dụ đảm bảo audio chất lượng cao:
     * -c:a aac -b:a 320k hoặc -c:a flac
     */
    fun executeFFmpegCommand(command: String): Flow<ExecutionState> = callbackFlow {
        trySend(ExecutionState.Connecting)

        val session: FFmpegSession = FFmpegKit.executeAsync(
            command,
            { session ->
                // onComplete Callback
                val returnCode = session.returnCode
                if (ReturnCode.isSuccess(returnCode)) {
                    val logs = session.logsAsString
                    trySend(ExecutionState.Success(logs))
                } else if (ReturnCode.isCancel(returnCode)) {
                    trySend(ExecutionState.Error(returnCode, "Canceled by user"))
                } else {
                    trySend(ExecutionState.Error(returnCode, session.failStackTrace ?: session.logsAsString))
                }
                close()
            },
            { log -> 
                // onLog Callback (nếu cần ghi log chi tiết)
            },
            { statistics -> 
                // onStatistics Callback
                trySend(ExecutionState.Progress(
                    timeInMilliseconds = statistics.time.toLong(),
                    size = statistics.size,
                    bitrate = statistics.bitrate
                ))
            }
        )

        awaitClose {
            FFmpegKit.cancel(session.sessionId)
        }
    }

    /**
     * Lấy đường dẫn SAF hợp lệ từ Content URI (ví dụ ContentResolver trả về) 
     * để tương thích trơn tru với các lệnh FFmpeg.
     */
    fun getSafParameter(uri: Uri, mode: String = "r"): String? {
        return FFmpegKitConfig.getSafParameterForRead(context, uri)
    }
}
