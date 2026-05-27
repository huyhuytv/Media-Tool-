package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.example.ui.components.VideoPlayer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(navController: NavController) {
    val context = LocalContext.current
    var hasUnsavedFile by remember { mutableStateOf(false) }
    var isRecording by remember { mutableStateOf(false) }
    var recordingTimeSec by remember { mutableStateOf(0) }
    var outputFile by remember { mutableStateOf<File?>(null) }
    
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    val coroutineScope = rememberCoroutineScope()

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/mp4")) { destUri ->
        destUri?.let { uri ->
            outputFile?.let { inFile ->
                coroutineScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    try {
                        context.contentResolver.openOutputStream(uri)?.use { outStream ->
                            inFile.inputStream().use { inStream ->
                                inStream.copyTo(outStream)
                            }
                        }
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            Toast.makeText(context, "Đã lưu âm thanh thành công!", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            Toast.makeText(context, "Lỗi khi lưu âm thanh: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isRecording) {
                try {
                    mediaRecorder?.stop()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            mediaRecorder?.release()
            mediaRecorder = null
        }
    }

    LaunchedEffect(isRecording) {
        if (isRecording) {
            while (true) {
                delay(1000L)
                recordingTimeSec++
            }
        }
    }

    fun startRecordingAction() {
        try {
            val dir = File(context.cacheDir, "recordings").apply { mkdirs() }
            val file = File(dir, "record_${System.currentTimeMillis()}.m4a")
            
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                MediaRecorder()
            }
            
            recorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            recorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            recorder.setAudioEncodingBitRate(com.example.core.SettingsManager.getAudioBitrateInt(context))
            recorder.setAudioSamplingRate(48000)
            recorder.setOutputFile(file.absolutePath)
            
            recorder.prepare()
            recorder.start()
            
            outputFile = file
            mediaRecorder = recorder
            isRecording = true
            recordingTimeSec = 0
            hasUnsavedFile = false
       } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Lỗi khi ghi âm: ${e.message}", Toast.LENGTH_SHORT).show()
       }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            startRecordingAction()
        } else {
            Toast.makeText(context, "Cần cấp quyền Microphone tĩnh", Toast.LENGTH_SHORT).show()
        }
    }

    fun stopRecordingAction() {
        if (isRecording) {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
            } catch (e: Exception) {
                e.printStackTrace()
            }
            isRecording = false
            hasUnsavedFile = true
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ghi âm đính kèm", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Chất lượng: ${com.example.core.SettingsManager.getAudioBitrateInt(context) / 1000}kbps | Định dạng lưu: M4A",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            Text(
                text = String.format("%02d:%02d", recordingTimeSec / 60, recordingTimeSec % 60),
                fontSize = 56.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .semantics { contentDescription = "Thời gian đã ghi: ${recordingTimeSec / 60} phút ${recordingTimeSec % 60} giây" }
            )

            Text(
                text = if (isRecording) "Đang ghi âm..." else if (hasUnsavedFile) "Đã ghi xong" else "Sẵn sàng ghi âm",
                textAlign = TextAlign.Center,
                color = if (isRecording) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )

            if (!isRecording && !hasUnsavedFile) {
                Button(
                    onClick = {
                        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                            startRecordingAction()
                        } else {
                            launcher.launch(Manifest.permission.RECORD_AUDIO)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) {
                    Text("BẮT ĐẦU GHI ÂM", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (isRecording) {
                FilledTonalButton(
                    onClick = { stopRecordingAction() },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("KẾT THÚC GHI", fontWeight = FontWeight.Bold)
                }
            }

            if (hasUnsavedFile && outputFile != null) {
                Column(modifier = Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Đường dẫn file lưu tại:\n${outputFile?.absolutePath}", style = MaterialTheme.typography.bodySmall)

                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Nghe lại file:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                            VideoPlayer(uri = Uri.fromFile(outputFile))
                        }
                    }

                    Button(
                        onClick = { saveLauncher.launch("recording_${System.currentTimeMillis()}.m4a") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    ) {
                        Text("Lưu file vào thiết bị", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            hasUnsavedFile = false
                            outputFile = null
                            recordingTimeSec = 0
                        },
                        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Xóa file vừa ghi âm và bắt đầu ghi lại từ đầu" }
                    ) {
                        Text("Xóa file này & Ghi âm lại", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            TextButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Quay lại Menu chính")
            }
        }
    }
}

