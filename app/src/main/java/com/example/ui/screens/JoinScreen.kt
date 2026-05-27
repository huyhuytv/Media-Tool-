package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.core.media.MediaEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mediaEngine = remember { MediaEngine(context) }
    
    val selectedUris = remember { mutableStateListOf<Uri>() }
    
    var isProcessing by remember { mutableStateOf(false) }
    var progressMsg by remember { mutableStateOf("") }
    var hasOutput by remember { mutableStateOf(false) }
    var outputPath by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        selectedUris.addAll(uris)
    }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/*")) { destUri ->
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
                            Toast.makeText(context, "Lỗi lưu file: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    fun startJoinAudio() {
        try {
            if (selectedUris.size < 2) {
                Toast.makeText(context, "Vui lòng chọn ít nhất 2 file để nối", Toast.LENGTH_SHORT).show()
                return
            }

            isProcessing = true
            progressMsg = "Đang chuẩn bị nối..."
            hasOutput = false

            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val safPaths = selectedUris.mapNotNull { mediaEngine.getSafParameter(it) }
                    if (safPaths.size != selectedUris.size) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Có lỗi khi đọc đường dẫn file gốc", Toast.LENGTH_SHORT).show()
                            isProcessing = false
                        }
                        return@launch
                    }

                    val outputDir = File(context.cacheDir, "join_outputs").apply { mkdirs() }
                    val ext = com.example.core.SettingsManager.getAudioFormatExt(context)
                    val outputFile = File(outputDir, "joined_audio_${System.currentTimeMillis()}.$ext")

                    val inputs = safPaths.joinToString(" ") { "-i \"$it\"" }
                    val filter = StringBuilder()
                    for (i in safPaths.indices) {
                        filter.append("[$i:a]aresample=48000,aformat=sample_fmts=fltp:channel_layouts=stereo[a$i];")
                    }
                    for (i in safPaths.indices) {
                        filter.append("[a$i]")
                    }
                    filter.append("concat=n=${safPaths.size}:v=0:a=1[outa]")
        
                    val acodec = com.example.core.SettingsManager.getAudioCodecArg(context)
                    val isLosslessSetting = com.example.core.SettingsManager.isAudioLossless(context)
                    
                    val abitrate = if (acodec.contains("flac") || acodec.contains("pcm")) {
                        "" 
                    } else {
                        if (isLosslessSetting) "-b:a ${com.example.core.SettingsManager.getAudioBitrateInt(context)/1000}k" else com.example.core.SettingsManager.getAudioBitrateArg(context)
                    }
        
                    val command = "-y $inputs -filter_complex \"$filter\" -map \"[outa]\" $acodec $abitrate \"${outputFile.absolutePath}\""

                    mediaEngine.executeFFmpegCommand(command).collect { state ->
                        withContext(Dispatchers.Main) {
                            when (state) {
                                is MediaEngine.ExecutionState.Connecting -> {
                                    progressMsg = "Đang khởi tạo FFmpeg..."
                                }
                                is MediaEngine.ExecutionState.Progress -> {
                                    progressMsg = "Đang xử lý: ${state.timeInMilliseconds} ms"
                                }
                                is MediaEngine.ExecutionState.Success -> {
                                    progressMsg = "Nối thành công!"
                                    isProcessing = false
                                    hasOutput = true
                                    outputPath = outputFile.absolutePath
                                    Toast.makeText(context, "Nối file thành công!", Toast.LENGTH_SHORT).show()
                                }
                                is MediaEngine.ExecutionState.Error -> {
                                    progressMsg = "Lỗi: ${state.returnCode}"
                                    isProcessing = false
                                    Toast.makeText(context, "Lỗi xảy ra trong FFmpeg", Toast.LENGTH_LONG).show()
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
        } catch(e: Throwable) {
            Toast.makeText(context, "Lỗi khi bắt đầu: ${e.message}", Toast.LENGTH_LONG).show()
            isProcessing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NỐI ĐA FILE AUDIO CHẤT LƯỢNG CAO", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val currentFormatStr = if (com.example.core.SettingsManager.isAudioLossless(context)) {
                "Gốc/High"
            } else {
                "${com.example.core.SettingsManager.getAudioFormatExt(context).uppercase()} ${com.example.core.SettingsManager.getAudioBitrateArg(context).replace("-b:a ", "")}"
            }

            Button(
                onClick = { /* Tính năng Video để sau */ },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text("Đang ở chế độ: NỐI ÂM THANH ($currentFormatStr)", textAlign = TextAlign.Center)
            }

            Button(onClick = { launcher.launch("audio/*") }, modifier = Modifier.fillMaxWidth()) {
                Text("Chọn Thêm File Audio (Chọn nhiều được)")
            }

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Danh sách file (${selectedUris.size}):", fontWeight = FontWeight.Bold)
                    if (selectedUris.isEmpty()) {
                        Text("Chưa chọn file nào.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        selectedUris.forEachIndexed { index, uri ->
                            Row(
                                modifier = Modifier.fillMaxWidth(), 
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                val name = getFileName(context, uri) ?: "Unknown"
                                Text(
                                    text = "${index + 1}. $name",
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { selectedUris.removeAt(index) },
                                    modifier = Modifier.semantics { contentDescription = "Xóa file $name khỏi danh sách" }
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            if (isProcessing || progressMsg.isNotEmpty()) {
                Text(
                    text = progressMsg, 
                    modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, 
                    textAlign = TextAlign.Center, 
                    fontWeight = FontWeight.Bold, 
                    color = MaterialTheme.colorScheme.primary
                )
                if (isProcessing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            Button(
                onClick = { startJoinAudio() },
                enabled = !isProcessing && selectedUris.size >= 2,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("BẮT ĐẦU NỐI FILE", color = Color(0xFFFF0000), fontWeight = FontWeight.Bold)
            }

            if (hasOutput) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Nối xong! File tạm:\n$outputPath", style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { 
                            val ext = outputPath.substringAfterLast(".", "m4a")
                            saveLauncher.launch("joined_audio_${System.currentTimeMillis()}.$ext")
                        }, modifier = Modifier.fillMaxWidth()) {
                            Text("Lưu file vào thiết bị")
                        }
                    }
                }
            }

            OutlinedButton(onClick = { selectedUris.clear(); hasOutput = false; outputPath = "" }, modifier = Modifier.fillMaxWidth()) {
                Text("Xóa danh sách hiện tại")
            }

            TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                Text("Quay lại")
            }
        }
    }
}

