package com.example.ui.screens

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.MediaItem
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.core.media.MediaEngine
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class BgAudioItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val uri: Uri,
    val name: String,
    val starts: String = "",
    val ends: String = "",
    val volume: Float = 0.3f,
    val pan: Int = 50 // 0=Trái, 50=Giữa, 100=Phải
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixScreen(navController: NavController) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val mediaEngine = remember { MediaEngine(context) }
    
    var isMixModeVideo by remember { mutableStateOf(false) }
    
    // Base File
    var baseUri by remember { mutableStateOf<Uri?>(null) }
    var baseName by remember { mutableStateOf("Chưa chọn") }
    var baseStarts by remember { mutableStateOf("") }
    var baseEnds by remember { mutableStateOf("") }
    var muteBaseVideo by remember { mutableStateOf(false) }
    var baseVolume by remember { mutableStateOf(1.0f) }
    var basePan by remember { mutableStateOf(50) }
    
    // Background Audios
    val bgAudios = remember { mutableStateListOf<BgAudioItem>() }
    var loopBg by remember { mutableStateOf(false) }
    var autoDuck by remember { mutableStateOf(false) }
    
    var isProcessing by remember { mutableStateOf(false) }
    var progressMsg by remember { mutableStateOf("Sẵn sàng") }
    var hasOutput by remember { mutableStateOf(false) }
    var outputPath by remember { mutableStateOf("") }
    
    // Players for Preview
    var basePlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var isBasePlaying by remember { mutableStateOf(false) }
    
    // Clean up players on dispose
    DisposableEffect(Unit) {
        onDispose {
            basePlayer?.release()
        }
    }

    val launcherBase = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            baseUri = uri
            baseName = getFileName(context, uri) ?: "base_file"
            
            // Release existing player so it reloads the new file
            basePlayer?.release()
            basePlayer = null
            isBasePlaying = false
        }
    }

    val launcherBg = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        uris.forEach { uri ->
            if (bgAudios.size < 5) {
                val name = getFileName(context, uri) ?: "audio_${bgAudios.size + 1}"
                bgAudios.add(BgAudioItem(uri = uri, name = name))
            } else {
                Toast.makeText(context, "Chỉ được chọn tối đa 5 nhạc nền", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
    
    fun toggleBasePlayer() {
        if (baseUri == null) {
            Toast.makeText(context, "Chưa chọn file gốc", Toast.LENGTH_SHORT).show()
            return
        }
        if (isBasePlaying && basePlayer != null) {
            basePlayer?.pause()
            isBasePlaying = false
        } else {
            if (basePlayer == null) {
                basePlayer = ExoPlayer.Builder(context).build().apply {
                    setMediaItem(MediaItem.fromUri(baseUri!!))
                    prepare()
                    addListener(object : androidx.media3.common.Player.Listener {
                        override fun onPlaybackStateChanged(state: Int) {
                            if (state == androidx.media3.common.Player.STATE_ENDED) {
                                isBasePlaying = false
                            }
                        }
                    })
                }
            }
            val vol = if (muteBaseVideo) 0f else baseVolume
            basePlayer?.volume = vol
            
            // Try to parse starts as Long
            try {
                val startsSplit = baseStarts.split(",").filter { it.isNotBlank() }
                if (startsSplit.isNotEmpty()) {
                    val s = startsSplit[0].trim().toLongOrNull() ?: 0L
                    if (s >= 0) basePlayer?.seekTo(s)
                }
            } catch (e: Exception) {}
            
            basePlayer?.play()
            isBasePlaying = true
        }
    }

    fun getCurrentPlayerMs(): Int {
        return try {
            basePlayer?.currentPosition?.toInt() ?: 0
        } catch (e: Exception) { 0 }
    }

    fun startProcessing() {
        try {
            if (baseUri == null || bgAudios.isEmpty()) {
                Toast.makeText(context, "Cần 1 File gốc và ít nhất 1 Nhạc nền", Toast.LENGTH_SHORT).show()
                return
            }
            
            isProcessing = true
            progressMsg = "Đang chuẩn bị ghép..."
            hasOutput = false
            
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val baseSaf = mediaEngine.getSafParameter(baseUri!!)
                    if (baseSaf == null) {
                        withContext(Dispatchers.Main) { Toast.makeText(context, "Lỗi đọc file gốc", Toast.LENGTH_SHORT).show(); isProcessing=false }
                        return@launch
                    }
                    
                    val ext = if (isMixModeVideo) "mp4" else com.example.core.SettingsManager.getAudioFormatExt(context)
                    val outputDir = File(context.cacheDir, "mix_outputs").apply { mkdirs() }
                    val outputFile = File(outputDir, "mixed_${System.currentTimeMillis()}.$ext")
                    
                    val amixDuration = if (isMixModeVideo || loopBg) "first" else "longest"
                    
                    // Build filter_complex string
                    val filter = java.lang.StringBuilder()
                    
                    // Build inputs with pre-trimming using -ss and -to for exact A/V sync without complex filter logic
                    val inputArgs = java.lang.StringBuilder()
                    
                    val baseStartSec = baseStarts.split(",").firstOrNull()?.trim()?.toDoubleOrNull()?.div(1000.0) ?: 0.0
                    val baseEndSec = baseEnds.split(",").firstOrNull()?.trim()?.toDoubleOrNull()?.div(1000.0) ?: 0.0
                    if (baseStartSec > 0f) inputArgs.append("-ss $baseStartSec ")
                    if (baseEndSec > baseStartSec) inputArgs.append("-to $baseEndSec ")
                    inputArgs.append("-i \"$baseSaf\" ")

                    bgAudios.forEach { bg ->
                        val bgSaf = mediaEngine.getSafParameter(bg.uri)
                        if (bgSaf != null) {
                            val bgStartSec = bg.starts.split(",").firstOrNull()?.trim()?.toDoubleOrNull()?.div(1000.0) ?: 0.0
                            val bgEndSec = bg.ends.split(",").firstOrNull()?.trim()?.toDoubleOrNull()?.div(1000.0) ?: 0.0
                            
                            if (loopBg) inputArgs.append("-stream_loop -1 ")
                            if (bgStartSec > 0f) inputArgs.append("-ss $bgStartSec ")
                            if (bgEndSec > bgStartSec) inputArgs.append("-to $bgEndSec ")
                            inputArgs.append("-i \"$bgSaf\" ")
                        }
                    }

                    val numInputs = 1 + bgAudios.size
                    if (muteBaseVideo) {
                        filter.append("[0:a]volume=0,aresample=48000,aformat=sample_fmts=fltp:channel_layouts=stereo[a0]; ")
                    } else {
                        val v = baseVolume
                        val Lvol = if (basePan <= 50) 1f else (100 - basePan) / 50f
                        val Rvol = if (basePan >= 50) 1f else basePan / 50f
                        
                        filter.append("[0:a]aresample=48000,aformat=sample_fmts=fltp:channel_layouts=stereo,volume=${v},pan=stereo|c0=${Lvol}*c0|c1=${Rvol}*c1[a0]; ")
                    }
                    
                    bgAudios.forEachIndexed { i, bg ->
                        val v = bg.volume
                        val Lvol = if (bg.pan <= 50) 1f else (100 - bg.pan) / 50f
                        val Rvol = if (bg.pan >= 50) 1f else bg.pan / 50f
                        
                        filter.append("[${i+1}:a]aresample=48000,aformat=sample_fmts=fltp:channel_layouts=stereo,volume=${v},pan=stereo|c0=${Lvol}*c0|c1=${Rvol}*c1[a${i+1}]; ")
                    }
                    
                    if (autoDuck && !muteBaseVideo && bgAudios.isNotEmpty()) {
                        // Ducking (Sidechain compression)
                        for (i in 1..bgAudios.size) {
                            filter.append("[a$i]")
                        }
                        if (bgAudios.size > 1) {
                            // Let the background mix be 'longest' so it doesn't get cut off prematurely before ducking
                            filter.append("amix=inputs=${bgAudios.size}:duration=longest:dropout_transition=2:normalize=0[bg_mixed]; ")
                        } else {
                            filter.append("volume=1[bg_mixed]; ")
                        }
                        
                        // Tách âm thanh gốc thành 2 đường: 1 để ghép đầu ra, 1 để làm sidechain điều khiển
                        filter.append("[a0]asplit=2[base_out][base_sc]; ")
                        
                        // Áp dụng hiệu ứng sidechaincompress (Bóp âm lượng bg_mixed dựa trên base_sc)
                        filter.append("[bg_mixed][base_sc]sidechaincompress=threshold=0.08:ratio=5.0:attack=100:release=1000[bg_ducked]; ")
                        
                        // Ghép đường âm thanh gốc (base_out) với nhạc nền đã được tự động ducking (bg_ducked)
                        // final duration=first ensures the output follows the length of base_out (video)
                        filter.append("[base_out][bg_ducked]amix=inputs=2:duration=${amixDuration}:dropout_transition=2:normalize=0[mixed_uncapped]; ")
                    } else {
                        for (i in 0 until numInputs) {
                            filter.append("[a$i]")
                        }
                        // normalize=0 ngăn chặn việc FFmpeg tự động giảm âm lượng tổng thể xuống (vd 3 input thì chia 3) gây nhỏ tiếng nghiêm trọng
                        filter.append("amix=inputs=$numInputs:duration=${amixDuration}:dropout_transition=2:normalize=0[mixed_uncapped]; ")
                    }
                    // Thêm Limiter ở bước cuối cùng để chống xé tiếng (clipping) nếu tổng âm lượng vượt quá 0dB
                    filter.append("[mixed_uncapped]alimiter=limit=-0.1dB:level_in=1:level_out=1[outa]")
                    
                    val acodec = if (isMixModeVideo) "-c:a aac" else com.example.core.SettingsManager.getAudioCodecArg(context)
                    val abitrateArg = com.example.core.SettingsManager.getAudioBitrateArg(context)
                    val abitrate = if (isMixModeVideo || !(acodec.contains("flac") || acodec.contains("pcm"))) abitrateArg else ""
                    
                    val vcodec = if (isMixModeVideo) "-c:v copy" else ""
                    val maps = if (isMixModeVideo) "-map 0:v? -map \"[outa]\"" else "-map \"[outa]\""
                    
                    val command = "-y $inputArgs -filter_complex \"$filter\" $maps $vcodec $acodec $abitrate \"${outputFile.absolutePath}\""
                    
                    android.util.Log.e("MixScreen", "Executing Mix Command: $command")
                    
                    mediaEngine.executeFFmpegCommand(command).collect { state ->
                        withContext(Dispatchers.Main) {
                            when (state) {
                                is MediaEngine.ExecutionState.Connecting -> progressMsg = "Khởi tạo..."
                                is MediaEngine.ExecutionState.Progress -> progressMsg = "Đang xử lý: ${state.timeInMilliseconds}ms"
                                is MediaEngine.ExecutionState.Success -> {
                                    progressMsg = "Ghép thành công!"
                                    isProcessing = false
                                    hasOutput = true
                                    outputPath = outputFile.absolutePath
                                }
                                is MediaEngine.ExecutionState.Error -> {
                                    progressMsg = "Lỗi FFmpeg."
                                    isProcessing = false
                                    Toast.makeText(context, "Ghép thất bại!", Toast.LENGTH_SHORT).show()
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
                title = { Text("GHÉP NHẠC ĐA LUỒNG", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
            Button(
                onClick = { isMixModeVideo = !isMixModeVideo },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Text(
                    text = if (isMixModeVideo) "Đang ở chế độ: GHÉP VIDEO (Bấm để đổi sang Audio)" else "Đang ở chế độ: GHÉP AUDIO (Bấm để đổi sang Video)",
                    textAlign = TextAlign.Center
                )
            }

            // [1] BASE FILE
            Text(if (isMixModeVideo) "[1] VIDEO GỐC" else "[1] FILE ÂM THANH GỐC", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
            
            Button(onClick = { 
                if (isMixModeVideo) launcherBase.launch("video/*") else launcherBase.launch("audio/*") 
            }, modifier = Modifier.fillMaxWidth()) {
                Text(if (isMixModeVideo) "Chọn Video gốc" else "Chọn Âm thanh gốc")
            }
            
            Text("Gốc: $baseName", fontWeight = FontWeight.Bold)
            
            OutlinedTextField(
                value = baseStarts,
                onValueChange = { baseStarts = it.filter { char -> char.isDigit() } },
                label = { Text("Mốc Bắt đầu Gốc (mili giây)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Button(onClick = { 
                baseStarts = getCurrentPlayerMs().toString()
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Lấy mốc bắt đầu hiện tại")
            }
            
            OutlinedTextField(
                value = baseEnds,
                onValueChange = { baseEnds = it.filter { char -> char.isDigit() } },
                label = { Text("Mốc Kết thúc Gốc (mili giây)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            Button(onClick = { 
                baseEnds = getCurrentPlayerMs().toString()
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Lấy mốc kết thúc hiện tại")
            }

            if (isMixModeVideo) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = muteBaseVideo, onCheckedChange = { muteBaseVideo = it }, modifier = Modifier.semantics { contentDescription = "Tắt âm thanh gốc của Video" })
                    Text("Tắt âm thanh gốc của Video")
                }
            }

            Text("Âm lượng Gốc: ${(baseVolume * 100).toInt()}%")
            Slider(
                value = baseVolume, 
                onValueChange = { baseVolume = it; if (basePlayer != null) basePlayer?.volume = it }, 
                valueRange = 0f..1.5f,
                modifier = Modifier.semantics {
                    contentDescription = "Thanh trượt âm lượng file gốc"
                    stateDescription = "${(baseVolume * 100).toInt()} phần trăm"
                }
            )
            
            Text("Pan Trái/Phải (Gốc): $basePan (0=Trái, 50=Giữa, 100=Phải)")
            Slider(
                value = basePan.toFloat(), 
                onValueChange = { basePan = it.toInt() }, 
                valueRange = 0f..100f,
                modifier = Modifier.semantics {
                    contentDescription = "Thanh trượt cân bằng trái phải file gốc"
                    stateDescription = "$basePan"
                }
            )

            Button(onClick = { toggleBasePlayer() }, modifier = Modifier.fillMaxWidth()) {
                Text(if (isBasePlaying) "Tạm dừng File Gốc" else "Phát File Gốc")
            }
            
            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // [2] BG AUDIOS
            Text("[2] DANH SÁCH NHẠC NỀN", color = Color(0xFF00AA00), fontWeight = FontWeight.Bold)
            
            Button(onClick = { launcherBg.launch("audio/*") }, modifier = Modifier.fillMaxWidth()) {
                Text("Thêm Nhạc nền (Được chọn nhiều)")
            }
            
            if (bgAudios.isEmpty()) {
                Text("Chưa có bản nhạc nền nào.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            
            bgAudios.forEachIndexed { index, audio ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("--- NHẠC ${index + 1}: ${audio.name} ---", color = Color(0xFF00AA00), modifier = Modifier.weight(1f), fontWeight = FontWeight.Bold)
                            IconButton(onClick = { bgAudios.removeAt(index) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        
                        OutlinedTextField(
                            value = audio.starts,
                            onValueChange = { newValue -> bgAudios[index] = audio.copy(starts = newValue.filter { it.isDigit() }) },
                            label = { Text("Bắt đầu (mili giây)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = audio.ends,
                            onValueChange = { newValue -> bgAudios[index] = audio.copy(ends = newValue.filter { it.isDigit() }) },
                            label = { Text("Kết thúc (mili giây)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        
                        Text("Âm lượng: ${(audio.volume * 100).toInt()}%")
                        Slider(
                            value = audio.volume, 
                            onValueChange = { bgAudios[index] = audio.copy(volume = it) }, 
                            valueRange = 0f..1.5f,
                            modifier = Modifier.semantics {
                                contentDescription = "Thanh trượt âm lượng nhạc nền ${index + 1}"
                                stateDescription = "${(audio.volume * 100).toInt()} phần trăm"
                            }
                        )
                        
                        Text("Pan L/R: ${audio.pan} (0=L, 100=R)")
                        Slider(
                            value = audio.pan.toFloat(), 
                            onValueChange = { bgAudios[index] = audio.copy(pan = it.toInt()) }, 
                            valueRange = 0f..100f,
                            modifier = Modifier.semantics {
                                contentDescription = "Thanh trượt cân bằng kênh nhạc nền ${index + 1}"
                                stateDescription = "${audio.pan}"
                            }
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = loopBg, onCheckedChange = { loopBg = it }, modifier = Modifier.semantics { contentDescription = "Lặp lại nhạc nền" })
                Text("Lặp lại nhạc nền")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = autoDuck, onCheckedChange = { autoDuck = it }, modifier = Modifier.semantics { contentDescription = "Tự động nhỏ nhạc nền khi có tiếng nói" })
                Text("Auto-Ducking (Tự động nhỏ nhạc nền khi có tiếng)")
            }

            Text(text = progressMsg, modifier = Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            if (isProcessing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Button(
                onClick = { startProcessing() },
                enabled = !isProcessing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
            ) {
                Text("BẮT ĐẦU GHÉP NHẠC", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
            
            if (hasOutput) {
                Button(onClick = { 
                    val ext = if (isMixModeVideo) "mp4" else outputPath.substringAfterLast(".", "m4a")
                    saveLauncher.launch("mixed_result_${System.currentTimeMillis()}.$ext")
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("Lưu File đã ghép vào thiết bị")
                }
            }
            
            OutlinedButton(onClick = { 
                baseUri = null; baseName = "Chưa chọn"; bgAudios.clear(); hasOutput = false
                basePlayer?.release()
                basePlayer = null
                isBasePlaying = false
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Xóa cấu hình")
            }
            
            TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                Text("Quay lại")
            }
        }
    }
}

