package com.example.ui.screens

import android.net.Uri
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
import androidx.navigation.NavController
import com.example.core.media.MediaEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mediaEngine = remember { MediaEngine(context) }
    
    var selectedUri by remember { mutableStateOf<Uri?>(null) }
    var fileName by remember { mutableStateOf("Chưa chọn") }
    
    var isProcessing by remember { mutableStateOf(false) }
    var progressMsg by remember { mutableStateOf("") }
    var hasOutput by remember { mutableStateOf(false) }
    var outputPath by remember { mutableStateOf("") }
    
    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/x-subrip")) { destUri ->
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
                            Toast.makeText(context, "Đã lưu phụ đề thành công!", Toast.LENGTH_LONG).show()
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Lỗi khi lưu phụ đề: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedUri = it
            fileName = getFileName(context, it) ?: "video_file"
        }
    }
    
    fun startExtractSubtitles() {
        try {
            if (selectedUri == null) {
                Toast.makeText(context, "Vui lòng chọn video!", Toast.LENGTH_SHORT).show()
                return
            }
            
            isProcessing = true
            progressMsg = "Đang kiểm tra và trích xuất..."
            hasOutput = false
            
            coroutineScope.launch {
                try {
                    val safPath = mediaEngine.getSafParameter(selectedUri!!)
                    if (safPath == null) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Lỗi đọc file!", Toast.LENGTH_SHORT).show()
                            isProcessing = false
                        }
                        return@launch
                    }
                    
                    val outputDir = File(context.cacheDir, "subs_output").apply { mkdirs() }
                    val outputFile = File(outputDir, "subtitles_${System.currentTimeMillis()}.srt")
                    
                    // Lệnh lấy stream phụ đề đầu tiên (0:s:0)
                    val command = "-y -i \"$safPath\" -map 0:s:0 -c:s srt \"${outputFile.absolutePath}\""
                    
                    mediaEngine.executeFFmpegCommand(command).collect { state ->
                        withContext(Dispatchers.Main) {
                            when (state) {
                                is MediaEngine.ExecutionState.Connecting -> progressMsg = "Đang bắt đầu..."
                                is MediaEngine.ExecutionState.Progress -> progressMsg = "Đang xử lý..."
                                is MediaEngine.ExecutionState.Success -> {
                                    isProcessing = false
                                    if (outputFile.exists() && outputFile.length() > 0) {
                                        progressMsg = "Trích xuất thành công!"
                                        hasOutput = true
                                        outputPath = outputFile.absolutePath
                                        Toast.makeText(context, "Đã trích xuất phụ đề!", Toast.LENGTH_SHORT).show()
                                    } else {
                                        progressMsg = "Lỗi: Không tìm thấy phụ đề trong video."
                                        outputFile.delete()
                                        Toast.makeText(context, "Video không chứa phụ đề!", Toast.LENGTH_LONG).show()
                                    }
                                }
                                is MediaEngine.ExecutionState.Error -> {
                                    isProcessing = false
                                    progressMsg = "Lập tức hoàn tất (Video không có phụ đề hoặc không hỗ trợ)."
                                    Toast.makeText(context, "Video này không có luồng phụ đề (Subtitle Stream) đính kèm!", Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                } catch(e: Throwable) {
                    withContext(Dispatchers.Main) {
                        progressMsg = "Ngoại lệ: ${e.message}"
                        isProcessing = false
                        Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        } catch(e: Throwable) {
            Toast.makeText(context, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
            isProcessing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trích xuất Phụ đề", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Tính năng này sẽ dò tìm trong Video xem có kênh Phụ đề (Subtitle Stream) nào được đính kèm gốc hay không. Nếu có, nó sẽ trích xuất ra file .SRT riêng biệt.", 
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )

            Button(onClick = { launcher.launch("video/*") }, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Chọn video từ thiết bị để trích xuất phụ đề" }) {
                Text("Chọn Video")
            }

            Text("Video đã chọn: $fileName", fontWeight = FontWeight.SemiBold, modifier = Modifier.semantics { contentDescription = "Video đang được chọn: $fileName" })

            if (isProcessing || progressMsg.isNotEmpty()) {
                Text(
                    text = progressMsg, 
                    modifier = Modifier.fillMaxWidth(), 
                    textAlign = TextAlign.Center, 
                    fontWeight = FontWeight.Bold, 
                    color = if (progressMsg.contains("Lỗi") || progressMsg.contains("không có")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                if (isProcessing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            Button(
                onClick = { startExtractSubtitles() },
                enabled = !isProcessing && selectedUri != null,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer, 
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("TRÍCH XUẤT", fontWeight = FontWeight.Bold)
            }

            if (hasOutput) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Phụ đề được lưu tại (định dạng SRT):", fontWeight = FontWeight.Bold)
                        Text(outputPath, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(vertical = 8.dp))
                        
                        Button(onClick = { 
                            saveLauncher.launch("subtitles_${System.currentTimeMillis()}.srt")
                        }, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Lưu file phụ đề định dạng SRT đã giải xuất vào thư mục của thiết bị" }) {
                            Text("Lưu phụ đề vào thiết bị")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                Text("Quay lại")
            }
        }
    }
}

