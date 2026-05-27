package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.media3.exoplayer.ExoPlayer
import com.example.core.media.MediaEngine
import com.example.ui.components.VideoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrimScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mediaEngine = remember { MediaEngine(context) }
    
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("Chưa chọn") }
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    
    var startMs by remember { mutableStateOf("0") }
    var endMs by remember { mutableStateOf("0") }
    
    var isProcessing by remember { mutableStateOf(false) }
    var progressMsg by remember { mutableStateOf("") }
    var hasOutput by remember { mutableStateOf(false) }
    var outputPath by remember { mutableStateOf("") }
    
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("*/*")) { destUri ->
        destUri?.let { uri ->
            if (outputPath.isNotEmpty()) {
                coroutineScope.launch(Dispatchers.IO) {
                    try {
                        val inFile = File(outputPath)
                        context.contentResolver.openOutputStream(uri)?.use { outStream ->
                            inFile.inputStream().use { inStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Đã lưu file thành công!", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Lỗi khi lưu file: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            fileName = getFileName(context, it) ?: "Unknown"
        }
    }
    
    fun getSeconds(msString: String): Double {
        return msString.toLongOrNull()?.let { it / 1000.0 } ?: 0.0
    }

    fun startTrim() {
        try {
            val uri = selectedUri
            if (uri == null) {
                Toast.makeText(context, "Vui lòng chọn file", Toast.LENGTH_SHORT).show()
                return
            }
            
            val startSec = getSeconds(startMs)
            val endSec = getSeconds(endMs)
            
            if (endSec <= startSec && endSec > 0) {
                Toast.makeText(context, "Mốc kết thúc phải lớn hơn bắt đầu", Toast.LENGTH_SHORT).show()
                return
            }
            
            val safPath = mediaEngine.getSafParameter(uri)
            if (safPath == null) {
                Toast.makeText(context, "Không thể đọc file", Toast.LENGTH_SHORT).show()
                return
            }
            
            val outputDir = File(context.cacheDir, "trim_outputs").apply { mkdirs() }
            val isAudio = fileName.endsWith(".mp3", true) || fileName.endsWith(".m4a", true) || 
                          fileName.endsWith(".wav", true) || fileName.endsWith(".flac", true) || 
                          fileName.endsWith(".ogg", true) || fileName.endsWith(".aac", true)
            
            val ext = if (isAudio) {
                if (com.example.core.SettingsManager.isAudioLossless(context)) {
                    if (fileName.contains(".")) fileName.substringAfterLast(".") else "m4a"
                } else {
                    com.example.core.SettingsManager.getAudioFormatExt(context)
                }
            } else {
                if (fileName.contains(".")) fileName.substringAfterLast(".") else "mp4"
            }
            val outputFile = File(outputDir, "trimmed_${System.currentTimeMillis()}.$ext")
            
            val duration = if (endSec > 0) endSec - startSec else 0.0
            val durationArg = if (duration > 0) "-t $duration" else ""
                          
            // Với audio, render lại hoàn thiện chuẩn từ Settings.
            val codecArg = if (isAudio) {
                if (com.example.core.SettingsManager.isAudioLossless(context)) {
                    "-c copy" // Cắt copy cho audio nếu chọn lossless (Tốc độ siêu nhanh, giữ nguyên chất lượng gốc)
                } else {
                    val acodec = com.example.core.SettingsManager.getAudioCodecArg(context)
                    val abitrate = com.example.core.SettingsManager.getAudioBitrateArg(context)
                    // Flac/Wav không cấu hình bitrate vì đã xuất không nén (lossless by nature)
                    if (acodec.contains("flac") || acodec.contains("pcm")) {
                        acodec
                    } else {
                        "$acodec $abitrate"
                    }
                }
            } else {
                "-c copy" // Video tạm thời giữ nguyên
            }
            
            val command = "-y -ss $startSec -i \"$safPath\" $durationArg $codecArg \"${outputFile.absolutePath}\""
            
            isProcessing = true
            progressMsg = "Đang xử lý..."
            hasOutput = false
            
            coroutineScope.launch {
                try {
                    mediaEngine.executeFFmpegCommand(command).collect { state ->
                        withContext(Dispatchers.Main) {
                            when (state) {
                                is MediaEngine.ExecutionState.Connecting -> {
                                    progressMsg = "Đang kết nối..."
                                }
                                is MediaEngine.ExecutionState.Progress -> {
                                    progressMsg = "Đang cắt: ${state.timeInMilliseconds}ms"
                                }
                                is MediaEngine.ExecutionState.Success -> {
                                    progressMsg = "Cắt thành công!"
                                    isProcessing = false
                                    hasOutput = true
                                    outputPath = outputFile.absolutePath
                                    Toast.makeText(context, "Cắt thành công!", Toast.LENGTH_SHORT).show()
                                }
                                is MediaEngine.ExecutionState.Error -> {
                                    progressMsg = "Lỗi: ${state.returnCode}"
                                    isProcessing = false
                                    Toast.makeText(context, "Lỗi cắt file", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                } catch(e: Throwable) {
                    withContext(Dispatchers.Main) {
                        progressMsg = "Ngoại lệ: ${e.message}"
                        isProcessing = false
                        Toast.makeText(context, "Lỗi Coroutine: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch (e: Throwable) {
            Toast.makeText(context, "Lỗi khi bắt đầu: ${e.message}", Toast.LENGTH_LONG).show()
            isProcessing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CẮT ĐA ĐOẠN AUDIO / VIDEO", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(onClick = { launcher.launch("*/*") }, modifier = Modifier.fillMaxWidth()) {
                Text("Chọn file cần cắt")
            }

            Text(text = "File: $fileName", modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant)

            if (selectedUri != null) {
                VideoPlayer(
                    uri = selectedUri!!,
                    onPlayerReady = { player -> exoPlayer = player }
                )
            }

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Mốc bắt đầu (ms)")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = startMs,
                            onValueChange = { startMs = it.filter { char -> char.isDigit() } },
                            modifier = Modifier.weight(1f),
                            label = { Text("Bắt đầu (mili giây)") },
                            placeholder = { Text("0") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
                        )
                        Button(onClick = { 
                            exoPlayer?.let { player -> startMs = player.currentPosition.toString() }
                        }, modifier = Modifier.semantics { contentDescription = "Lấy mốc thời gian bắt đầu đang phát trên trình phát video" }) {
                            Text("Lấy mốc hiện tại")
                        }
                    }

                    Text(text = "Mốc kết thúc (ms, để 0 lấy đến hết)")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = endMs,
                            onValueChange = { endMs = it.filter { char -> char.isDigit() } },
                            modifier = Modifier.weight(1f),
                            label = { Text("Kết thúc (mili giây)") },
                            placeholder = { Text("0") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done)
                        )
                        Button(onClick = {
                            exoPlayer?.let { player -> endMs = player.currentPosition.toString() }
                        }, modifier = Modifier.semantics { contentDescription = "Lấy mốc thời gian kết thúc đang phát trên trình phát video" }) {
                            Text("Lấy mốc hiện tại")
                        }
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            if (isProcessing || progressMsg.isNotEmpty()) {
                Text(text = progressMsg, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (isProcessing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            Button(
                onClick = { startTrim() },
                enabled = !isProcessing && selectedUri != null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
            ) {
                Text("BẮT ĐẦU CẮT FILE", color = Color(0xFFFF0000), fontWeight = FontWeight.Bold)
            }

            if (hasOutput) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Đã cắt xong! File lưu tạm tại:\n$outputPath", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { 
                            val ext = if (outputPath.contains(".")) outputPath.substringAfterLast(".") else "mp4"
                            saveLauncher.launch("trimmed_${System.currentTimeMillis()}.$ext")
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Lưu file vào thiết bị")
                        }
                    }
                }
            }

            TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                Text("Quay lại")
            }
        }
    }
}

fun getFileName(context: android.content.Context, uri: Uri): String? {
    var result: String? = null
    try {
        if (uri.scheme == "content") {
            val cursor = context.contentResolver.query(uri, null, null, null, null)
            try {
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index != -1) {
                        result = cursor.getString(index)
                    }
                }
            } finally {
                cursor?.close()
            }
        }
        if (result == null) {
            result = uri.path
            val cut = result?.lastIndexOf('/') ?: -1
            if (cut != -1) {
                result = result?.substring(cut + 1)
            }
        }
    } catch (e: Exception) {
        result = "unknown_file_${System.currentTimeMillis()}"
    }
    return result
}
