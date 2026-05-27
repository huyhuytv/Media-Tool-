package com.example.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.core.media.MediaEngine
import com.example.ui.components.VideoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Img2VidScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mediaEngine = remember { MediaEngine(context) }
    
    var audioUri by remember { mutableStateOf<Uri?>(null) }
    var audioName by remember { mutableStateOf("Chưa chọn") }
    
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var expanded by remember { mutableStateOf(false) }
    var ratioIndex by remember { mutableStateOf(0) }
    val ratios = listOf("Ngang 16:9", "Dọc 9:16", "Vuông 1:1")
    
    var isProcessing by remember { mutableStateOf(false) }
    var progressMsg by remember { mutableStateOf("") }
    var outputUri by remember { mutableStateOf<Uri?>(null) }

    val saveLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("video/mp4")) { destUri ->
        destUri?.let { uri ->
            outputUri?.let { outUri ->
                val outputPathStr = outUri.path ?: ""
                if (outputPathStr.isNotEmpty()) {
                    coroutineScope.launch(Dispatchers.IO) {
                        try {
                            val inFile = File(outputPathStr)
                            context.contentResolver.openOutputStream(uri)?.use { outStream ->
                                inFile.inputStream().use { inStream ->
                                    inStream.copyTo(outStream)
                                }
                            }
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Đã lưu video thành công!", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Lỗi khi lưu video: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            audioUri = uri
            audioName = getFileName(context, uri) ?: "audio_file"
            outputUri = null
        }
    }
    
    val imagesLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        selectedImageUris = selectedImageUris + uris
        outputUri = null
    }

    fun startCreateVideo() {
        if (audioUri == null) {
            Toast.makeText(context, "Vui lòng chọn âm thanh", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedImageUris.isEmpty()) {
            Toast.makeText(context, "Vui lòng chọn ít nhất 1 ảnh", Toast.LENGTH_SHORT).show()
            return
        }
        
        isProcessing = true
        progressMsg = "Đang chuẩn bị..."
        outputUri = null
        
        coroutineScope.launch {
            val audioSaf = mediaEngine.getSafParameter(audioUri!!)
            if (audioSaf == null) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Lỗi đọc file âm thanh", Toast.LENGTH_SHORT).show()
                    isProcessing = false
                }
                return@launch
            }
            
            val outputDir = File(context.cacheDir, "img2vid").apply { mkdirs() }
            val outputFile = File(outputDir, "video_${System.currentTimeMillis()}.mp4")
            
            val scaleFilter = when (ratioIndex) {
                0 -> "scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2"
                1 -> "scale=720:1280:force_original_aspect_ratio=decrease,pad=720:1280:(ow-iw)/2:(oh-ih)/2"
                else -> "scale=720:720:force_original_aspect_ratio=decrease,pad=720:720:(ow-iw)/2:(oh-ih)/2"
            }
            
            val vPreset = com.example.core.SettingsManager.getVideoPresetArg(context)
            val vBitrate = com.example.core.SettingsManager.getVideoBitrateArg(context)
            // Lấy audio setting. MP4 container + libx264 tương thích tốt nhất với AAC.
            // Nếu là lossless hoặc định dạng không nén (flac, wav), ta ép sang AAC 320kbps để đảm bảo file MP4 hoạt động trơn tru trên mọi loại thiết bị.
            val rawCodec = com.example.core.SettingsManager.getAudioCodecArg(context)
            val usesIncompatibleCodec = rawCodec.contains("flac") || rawCodec.contains("pcm")
            val isLossless = com.example.core.SettingsManager.isAudioLossless(context)
            
            val aCodec = if (isLossless || usesIncompatibleCodec) "-c:a aac" else rawCodec
            val aBitrate = if (isLossless || usesIncompatibleCodec) "-b:a ${com.example.core.SettingsManager.getAudioBitrateInt(context)/1000}k" else com.example.core.SettingsManager.getAudioBitrateArg(context)

            val command = if (selectedImageUris.size == 1) {
                val imgSaf = mediaEngine.getSafParameter(selectedImageUris.first())
                "-y -loop 1 -framerate 1 -i \"$imgSaf\" -i \"$audioSaf\" -vf \"$scaleFilter\" -c:v libx264 $vPreset $vBitrate -tune stillimage $aCodec $aBitrate -pix_fmt yuv420p -shortest \"${outputFile.absolutePath}\""
            } else {
                val concatFile = File(context.cacheDir, "img_concat.txt")
                val writer = FileWriter(concatFile)
                selectedImageUris.forEach { uri ->
                    val saf = mediaEngine.getSafParameter(uri)
                    saf?.let {
                        writer.write("file '${it.replace("'", "'\\''")}'\n")
                        writer.write("duration 3\n")
                    }
                }
                val lastSaf = mediaEngine.getSafParameter(selectedImageUris.last())
                lastSaf?.let {
                    writer.write("file '${it.replace("'", "'\\''")}'\n")
                }
                writer.close()
                
                "-y -stream_loop -1 -f concat -safe 0 -i \"${concatFile.absolutePath}\" -i \"$audioSaf\" -vf \"$scaleFilter\" -c:v libx264 $vPreset $vBitrate -tune stillimage $aCodec $aBitrate -pix_fmt yuv420p -shortest \"${outputFile.absolutePath}\""
            }
            
            mediaEngine.executeFFmpegCommand(command).collect { state ->
                withContext(Dispatchers.Main) {
                    when (state) {
                        is MediaEngine.ExecutionState.Connecting -> progressMsg = "Đang bắt đầu..."
                        is MediaEngine.ExecutionState.Progress -> progressMsg = "Đang xử lý tạo video..."
                        is MediaEngine.ExecutionState.Success -> {
                            progressMsg = "Hoàn thành!"
                            isProcessing = false
                            outputUri = Uri.fromFile(outputFile)
                            Toast.makeText(context, "Tạo video thành công!", Toast.LENGTH_SHORT).show()
                        }
                        is MediaEngine.ExecutionState.Error -> {
                            progressMsg = "Lỗi tạo video!"
                            isProcessing = false
                            Toast.makeText(context, "Lỗi: ${state.returnCode}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ghép ảnh vào âm thanh", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = { audioLauncher.launch("audio/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Chọn Âm thanh")
                }
                Text("File đã chọn: $audioName", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = { imagesLauncher.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                    Text("Thêm Ảnh (Nhiều file)")
                }
                Text("Đã chọn ${selectedImageUris.size} ảnh. (Mỗi ảnh hiển thị 3s nếu chọn nhiều)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }

            if (selectedImageUris.isNotEmpty()) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(selectedImageUris) { index, uri ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Ảnh ${index + 1}: ${getFileName(context, uri)}", modifier = Modifier.weight(1f))
                                    IconButton(
                                        onClick = { 
                                            selectedImageUris = selectedImageUris.toMutableList().apply { removeAt(index) } 
                                        },
                                        modifier = Modifier.semantics { contentDescription = "Xóa ảnh thứ ${index + 1} khỏi danh sách" }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = ratios[ratioIndex],
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    label = { Text("Tỉ lệ khung hình") }
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    ratios.forEachIndexed { index, ratio ->
                        DropdownMenuItem(
                            text = { Text(ratio) },
                            onClick = {
                                ratioIndex = index
                                expanded = false
                            }
                        )
                    }
                }
            }

            if (isProcessing || progressMsg.isNotEmpty()) {
                Text(text = progressMsg, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                if (isProcessing) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }

            Button(
                onClick = { startCreateVideo() },
                enabled = !isProcessing && audioUri != null && selectedImageUris.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("BẮT ĐẦU TẠO VIDEO", fontWeight = FontWeight.Bold)
            }

            if (outputUri != null) {
                Button(onClick = { 
                     saveLauncher.launch("video_${System.currentTimeMillis()}.mp4")
                }, modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Lưu video vừa tạo được bằng ảnh và âm thanh vào thiết bị" }) {
                    Text("Lưu video vào thiết bị")
                }
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Trình phát:", fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 8.dp))
                        VideoPlayer(uri = outputUri!!)
                    }
                }
            }

            TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                Text("Quay lại")
            }
        }
    }
}

