package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import com.example.core.ml.AudioSeparator
import com.example.core.ml.DownloadState
import com.example.core.ml.ModelDownloader
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StemScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val downloader = remember { ModelDownloader(context) }
    val modelUrl = "https://huggingface.co/MrCitron/demucs-v4-onnx/resolve/main/htdemucs_ft.onnx"
    val modelName = "htdemucs_ft.onnx"
    
    var downloadState by remember { mutableStateOf<DownloadState>(DownloadState.Idle) }
    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var separationProgress by remember { mutableFloatStateOf(0f) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var resultVocalsUri by remember { mutableStateOf<Uri?>(null) }
    var resultMusicUri by remember { mutableStateOf<Uri?>(null) }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                selectedAudioUri = uri
                resultVocalsUri = null
                resultMusicUri = null
                errorMsg = null
            }
        }
    )
    
    // Check if model already downloaded
    LaunchedEffect(Unit) {
        if (downloader.isModelDownloaded(modelName)) {
            downloadState = DownloadState.Success(java.io.File(context.filesDir, "models/$modelName"))
        }
    }

    fun shareAudio(uri: Uri, title: String) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "audio/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(shareIntent, title)
        context.startActivity(chooser)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tách Nhạc và Lời (On-Device)") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Trở về")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            when (val state = downloadState) {
                is DownloadState.Idle -> {
                    Text(
                        "Tính năng này cần tải mô hình AI để xử lý ngoại tuyến (khoảng vài trăm MB).",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Button(onClick = {
                        coroutineScope.launch {
                            downloader.downloadModel(modelUrl, modelName).collect { state ->
                                downloadState = state
                            }
                        }
                    }) {
                        Text("Tải Mô Hình AI")
                    }
                }
                is DownloadState.Downloading -> {
                    Text("Đang tải mô hình...")
                    Spacer(modifier = Modifier.height(16.dp))
                    if (state.progress >= 0f) {
                        LinearProgressIndicator(
                            progress = { state.progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            text = "${(state.progress * 100).toInt()}%",
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        CircularProgressIndicator()
                        Text("Đang kết nối...", modifier = Modifier.padding(top = 8.dp))
                    }
                }
                is DownloadState.Success -> {
                    if (isProcessing) {
                        Text("Đang dùng AI để tách nhạc... Vui lòng không thoát")
                        Spacer(modifier = Modifier.height(16.dp))
                        LinearProgressIndicator(
                            progress = { separationProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("${(separationProgress * 100).toInt()}%", modifier = Modifier.padding(top = 8.dp))
                    } else if (resultVocalsUri != null && resultMusicUri != null) {
                        Text(
                            "Tách thành công!",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        Button(onClick = { shareAudio(resultVocalsUri!!, "Chia sẻ Lời (Vocals)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                            Text("🎬 Tùy chọn xuất: Lời (Vocals)")
                        }
                        Button(onClick = { shareAudio(resultMusicUri!!, "Chia sẻ Nhạc (Beat)") }, modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
                            Text("🎸 Tùy chọn xuất: Nhạc (Beat)")
                        }
                        OutlinedButton(onClick = { 
                            resultVocalsUri = null
                            resultMusicUri = null
                            selectedAudioUri = null
                        }) {
                            Text("Tách video/bài hát khác")
                        }
                    } else {
                        Text(
                            "Mô hình AI đã được cài đặt.",
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        
                        Button(onClick = { audioPicker.launch("*/*") }) {
                            Text(if (selectedAudioUri == null) "Chọn File Âm Thanh/Video" else "Đã chọn file: ${selectedAudioUri?.lastPathSegment}")
                        }
                        
                        if (selectedAudioUri != null) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(onClick = {
                                isProcessing = true
                                errorMsg = null
                                coroutineScope.launch {
                                    try {
                                        val separator = AudioSeparator(context, state.file)
                                        separator.separate(selectedAudioUri!!).collect { state ->
                                            when(state) {
                                                is com.example.core.ml.SeparationState.Progress -> {
                                                    separationProgress = state.value
                                                }
                                                is com.example.core.ml.SeparationState.Success -> {
                                                    resultVocalsUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", state.vocalsFile)
                                                    resultMusicUri = FileProvider.getUriForFile(context, "${context.packageName}.provider", state.musicFile)
                                                }
                                            }
                                        }
                                    } catch (e: Exception) {
                                        errorMsg = e.message
                                    } finally {
                                        isProcessing = false
                                    }
                                }
                            }) {
                                Text("Bắt Đầu Tách")
                            }
                        }

                        if (errorMsg != null) {
                            Text(
                                "Lỗi: $errorMsg",
                                color = MaterialTheme.colorScheme.error,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 24.dp)
                            )
                        }
                        
                        Text(
                            "Lưu ý: Quá trình phân tích tốn khá nhiều RAM (khoảng 1.5GB - 2GB) và có thể diễn ra chậm tùy mức độ dài của bài hát.",
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 24.dp)
                        )
                    }
                }
                is DownloadState.Error -> {
                    Text(
                        "Lỗi: ${state.message}",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    Button(onClick = {
                        coroutineScope.launch {
                            downloader.downloadModel(modelUrl, modelName).collect { state ->
                                downloadState = state
                            }
                        }
                    }) {
                        Text("Thử Lại")
                    }
                }
            }
        }
    }
}
