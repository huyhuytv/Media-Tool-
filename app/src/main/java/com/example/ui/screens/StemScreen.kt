package com.example.ui.screens

import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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
    var resultVocalsFile by remember { mutableStateOf<File?>(null) }
    var resultMusicFile by remember { mutableStateOf<File?>(null) }
    var fileToSave by remember { mutableStateOf<File?>(null) }

    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
        onResult = { uri ->
            if (uri != null) {
                selectedAudioUri = uri
                resultVocalsUri = null
                resultMusicUri = null
                resultVocalsFile = null
                resultMusicFile = null
                errorMsg = null
            }
        }
    )

    val saveFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("audio/wav"),
        onResult = { uri ->
            if (uri != null && fileToSave != null) {
                coroutineScope.launch {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            fileToSave!!.inputStream().use { input ->
                                input.copyTo(output)
                            }
                        }
                        Toast.makeText(context, "Đã lưu thành công", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Lỗi khi lưu: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                    fileToSave = null
                }
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
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        AudioPreviewCard(
                            title = "Lời Hát (Vocals)",
                            audioFile = resultVocalsFile,
                            icon = Icons.Default.Person,
                            onShare = { shareAudio(resultVocalsUri!!, "Chia sẻ Lời (Vocals)") },
                            onSaveClick = { 
                                fileToSave = resultVocalsFile
                                saveFileLauncher.launch("Vocals_${System.currentTimeMillis()}.wav") 
                            }
                        )
                        AudioPreviewCard(
                            title = "Nhạc Nền (Beat)",
                            audioFile = resultMusicFile,
                            icon = Icons.Default.MusicNote,
                            onShare = { shareAudio(resultMusicUri!!, "Chia sẻ Nhạc (Beat)") },
                            onSaveClick = {
                                fileToSave = resultMusicFile
                                saveFileLauncher.launch("Beat_${System.currentTimeMillis()}.wav")
                            }
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedButton(onClick = { 
                            resultVocalsUri = null
                            resultMusicUri = null
                            resultVocalsFile = null
                            resultMusicFile = null
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
                                                    resultVocalsFile = state.vocalsFile
                                                    resultMusicFile = state.musicFile
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

@Composable
fun AudioPreviewCard(
    title: String,
    audioFile: java.io.File?,
    icon: ImageVector,
    onShare: () -> Unit,
    onSaveClick: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var duration by remember { mutableIntStateOf(1) }

    DisposableEffect(audioFile) {
        var mp: MediaPlayer? = null
        if (audioFile != null && audioFile.exists()) {
            mp = MediaPlayer().apply {
                try {
                    setDataSource(audioFile.absolutePath)
                    prepare()
                    duration = this.duration
                    setOnCompletionListener { 
                        isPlaying = false
                        progress = 0f
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            mediaPlayer = mp
        }
        onDispose {
            mp?.release()
            mediaPlayer = null
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            mediaPlayer?.let { mp ->
                if (duration > 0 && mp.isPlaying) {
                    progress = mp.currentPosition.toFloat() / duration.toFloat()
                }
            }
            kotlinx.coroutines.delay(100)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(16.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (isPlaying) {
                        mediaPlayer?.pause()
                        isPlaying = false
                    } else {
                        mediaPlayer?.start()
                        isPlaying = true
                    }
                }, modifier = Modifier.semantics {
                    contentDescription = if (isPlaying) "Tạm dừng phát $title" else "Phát thử âm thanh $title"
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                }
                
                Slider(
                    value = progress,
                    onValueChange = { newValue ->
                        progress = newValue
                        mediaPlayer?.let { mp ->
                            mp.seekTo((newValue * duration).toInt())
                        }
                    },
                    modifier = Modifier.weight(1f).semantics {
                        contentDescription = "Tua âm thanh đoạn $title"
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onShare, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Chia sẻ")
                }
                FilledTonalButton(onClick = onSaveClick, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Lưu về máy")
                }
            }
        }
    }
}

