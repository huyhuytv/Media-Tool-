package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherScreen(navController: NavController) {
    var isVideoMode by remember { mutableStateOf(false) }
    var modeIndex by remember { mutableStateOf(0) }
    var expandedMode by remember { mutableStateOf(false) }

    val audioModes = listOf("Xử lý Hiệu ứng", "Chuyển đổi định dạng")
    val videoModes = listOf("Hiệu ứng (Giữ Video)", "Trích xuất Âm thanh", "Tắt tiếng Video", "Trích xuất Ảnh (Thumbnail)", "Nén dung lượng Video")
    val currentModes = if (isVideoMode) videoModes else audioModes

    // States for effects
    var enableTimeMocks by remember { mutableStateOf(false) }
    var enableNorm by remember { mutableStateOf(false) }
    var enableNg by remember { mutableStateOf(false) }
    var enableSpeedPitch by remember { mutableStateOf(false) }
    var enablePan by remember { mutableStateOf(false) }
    var enableAutoPan by remember { mutableStateOf(false) }
    var enableEcho by remember { mutableStateOf(false) }
    var enableReverb by remember { mutableStateOf(false) }
    var enableComp by remember { mutableStateOf(false) }
    var enableEq by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tính năng khác (Hiệu ứng/Trích xuất)", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
            Button(
                onClick = {
                    isVideoMode = !isVideoMode
                    modeIndex = 0
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = Color(0xFF00A0FF))
            ) {
                Text(if (isVideoMode) "Đang ở chế độ: VIDEO (Bấm để đổi sang Audio)" else "Đang ở chế độ: AUDIO (Bấm để đổi sang Video)")
            }

            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text(if (isVideoMode) "Chọn file Video" else "Chọn file Âm thanh")
            }

            Text("File: Chưa chọn", color = MaterialTheme.colorScheme.onSurfaceVariant)

            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("▶ Nghe thử file trước xử lý")
            }

            Text("Thời gian: 00:00 / 00:00")

            ExposedDropdownMenuBox(
                expanded = expandedMode,
                onExpandedChange = { expandedMode = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = currentModes.getOrNull(modeIndex) ?: "",
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedMode) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    label = { Text("Chế độ xử lý") }
                )
                ExposedDropdownMenu(expanded = expandedMode, onDismissRequest = { expandedMode = false }) {
                    currentModes.forEachIndexed { index, mode ->
                        DropdownMenuItem(
                            text = { Text(mode) },
                            onClick = {
                                modeIndex = index
                                expandedMode = false
                            }
                        )
                    }
                }
            }

            if ((!isVideoMode && modeIndex == 1) || (isVideoMode && modeIndex == 1)) {
                // layout_ext_fmt
                val exts = listOf("M4A (Nhẹ)", "WAV (Chất lượng cao)", "MP3 (Giả lập đuôi)")
                var extIndex by remember { mutableStateOf(0) }
                var expandedExt by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedExt,
                    onExpandedChange = { expandedExt = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = exts[extIndex],
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedExt) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        label = { Text("Định dạng xuất") }
                    )
                    ExposedDropdownMenu(expanded = expandedExt, onDismissRequest = { expandedExt = false }) {
                        exts.forEachIndexed { index, mode ->
                            DropdownMenuItem(text = { Text(mode) }, onClick = { extIndex = index; expandedExt = false })
                        }
                    }
                }
            }

            if (isVideoMode && modeIndex == 3) {
                // layout_extract_img
                Text("Nhập các mốc thời gian (giây) để cắt ảnh. Cách nhau bằng dấu phẩy.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = "", onValueChange = {}, placeholder = { Text("Ví dụ: 1.5, 5, 12") }, modifier = Modifier.fillMaxWidth())
                Text("* Ảnh sẽ tự động lưu vào thư mục Download.", fontSize = 12.sp, color = Color(0xFF00AA00))
            }

            if (isVideoMode && modeIndex == 4) {
                // layout_compress_vid
                Text("Độ phân giải đầu ra:")
                val resList = listOf("Giữ nguyên độ phân giải", "Giảm xuống 720p", "Giảm xuống 480p")
                var resIndex by remember { mutableStateOf(0) }
                var expandedRes by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expandedRes,
                    onExpandedChange = { expandedRes = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = resList[resIndex],
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedRes) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expandedRes, onDismissRequest = { expandedRes = false }) {
                        resList.forEachIndexed { index, mode ->
                            DropdownMenuItem(text = { Text(mode) }, onClick = { resIndex = index; expandedRes = false })
                        }
                    }
                }
                Text("Chất lượng nén: 70%")
                Slider(value = 60f, onValueChange = {}, valueRange = 0f..90f, modifier = Modifier.fillMaxWidth())
            }

            // Effects section
            if ((!isVideoMode && modeIndex == 0) || (isVideoMode && (modeIndex == 0 || modeIndex == 1))) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = enableTimeMocks, onCheckedChange = { enableTimeMocks = it })
                    Text("⏱️ Bật chế độ cài đặt thời gian cho từng hiệu ứng", color = Color(0xFF00A0FF), fontSize = 14.sp)
                }

                @Composable
                fun TimeBlock() {
                    if (enableTimeMocks) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(value = "", onValueChange = {}, modifier = Modifier.weight(1f), label = { Text("Từ (ms)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                            OutlinedTextField(value = "", onValueChange = {}, modifier = Modifier.weight(1f), label = { Text("Đến (ms)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            Button(onClick = { }, modifier = Modifier.weight(1f)) { Text("Lấy mốc Bắt đầu", fontSize = 12.sp) }
                            Button(onClick = { }, modifier = Modifier.weight(1f)) { Text("Lấy mốc Kết thúc", fontSize = 12.sp) }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = enableNorm, onCheckedChange = { enableNorm = it })
                            Text("Bật chuẩn hóa âm lượng", fontWeight = FontWeight.SemiBold)
                        }
                        if (enableNorm) {
                            TimeBlock()
                            Text("Peak mục tiêu: 100%")
                            Slider(value = 1f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = enableNg, onCheckedChange = { enableNg = it })
                            Text("Bật Noise Gate (Cổng triệt ồn)", fontWeight = FontWeight.SemiBold)
                        }
                        if (enableNg) {
                            TimeBlock()
                            Text("Chế độ triệt ồn:")
                            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("Preset hiện tại") }
                            OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("⚙️ THIẾT LẬP THỦ CÔNG") }
                        }
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = enableSpeedPitch, onCheckedChange = { enableSpeedPitch = it })
                            Text("Bật Thay đổi Tốc độ & Độ cao", fontWeight = FontWeight.SemiBold)
                        }
                        if (enableSpeedPitch) {
                            TimeBlock()
                            Text("Chế độ thuật toán:")
                            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("Preset hiện tại") }
                            if (!isVideoMode) {
                                Text("Tốc độ (Speed): 1.00x")
                                Slider(value = 0.5f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
                            }
                            Text("Độ cao (Pitch): 1.00x")
                            Slider(value = 0.5f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
                            OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("⚙️ THIẾT LẬP NÂNG CAO ĐỔI GIỌNG") }
                        }
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = enablePan, onCheckedChange = { enablePan = it })
                            Text("Bật Pan tĩnh", fontWeight = FontWeight.SemiBold)
                        }
                        if (enablePan) {
                            TimeBlock()
                            Text("Pan: Giữa")
                            Slider(value = 0.5f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = enableAutoPan, onCheckedChange = { enableAutoPan = it })
                            Text("Bật Auto Pan (Hiệu ứng đảo tai)", fontWeight = FontWeight.SemiBold)
                        }
                        if (enableAutoPan) {
                            TimeBlock()
                            Text("Chu kỳ Auto Pan: 4000 ms")
                            Slider(value = 0.5f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = enableEcho, onCheckedChange = { enableEcho = it })
                            Text("Bật tiếng vang (Echo)", fontWeight = FontWeight.SemiBold)
                        }
                        if (enableEcho) {
                            TimeBlock()
                            Text("Độ trễ vang: 300 ms")
                            Slider(value = 0.5f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
                            Text("Độ ngân dài: 0.5")
                            Slider(value = 0.5f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = enableReverb, onCheckedChange = { enableReverb = it })
                            Text("Bật Reverb (Vang phòng thu)", fontWeight = FontWeight.SemiBold)
                        }
                        if (enableReverb) {
                            TimeBlock()
                            Text("Kích thước phòng: 50%")
                            Slider(value = 0.5f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
                            Text("Hấp thụ (Damping): 50%")
                            Slider(value = 0.5f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
                            Text("Mức Reverb (Wet): 30%")
                            Slider(value = 0.3f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = enableComp, onCheckedChange = { enableComp = it })
                            Text("Bật Compressor (Nén âm lượng)", fontWeight = FontWeight.SemiBold)
                        }
                        if (enableComp) {
                            TimeBlock()
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = false, onCheckedChange = { })
                                Text("Chế độ Limiter (chặn cứng)")
                            }
                            Text("Ngưỡng (Threshold): -10 dB")
                            Slider(value = 0.5f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
                            Text("Tỉ lệ nén (Ratio): 4:1")
                            Slider(value = 0.5f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
                            Text("Attack: 10 ms")
                            Slider(value = 0.5f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
                            Text("Release: 100 ms")
                            Slider(value = 0.5f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
                            Text("Bù Gain (Makeup): 0 dB")
                            Slider(value = 0f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = enableEq, onCheckedChange = { enableEq = it })
                            Text("Bật EQ (Equalizer 5 dải tần)", fontWeight = FontWeight.SemiBold)
                        }
                        if (enableEq) {
                            TimeBlock()
                            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("Preset hiện tại") }
                            Text("60Hz: 0 dB"); Slider(value = 15f, onValueChange = {}, valueRange = 0f..30f, modifier = Modifier.fillMaxWidth())
                            Text("230Hz: 0 dB"); Slider(value = 15f, onValueChange = {}, valueRange = 0f..30f, modifier = Modifier.fillMaxWidth())
                            Text("910Hz: 0 dB"); Slider(value = 15f, onValueChange = {}, valueRange = 0f..30f, modifier = Modifier.fillMaxWidth())
                            Text("3.6kHz: 0 dB"); Slider(value = 15f, onValueChange = {}, valueRange = 0f..30f, modifier = Modifier.fillMaxWidth())
                            Text("14kHz: 0 dB"); Slider(value = 15f, onValueChange = {}, valueRange = 0f..30f, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }

                Button(onClick = { }, modifier = Modifier.fillMaxWidth()) { Text("Chế độ kênh: Giữ nguyên") }
            }

            Text("Sẵn sàng", modifier = Modifier.fillMaxWidth().padding(top = 16.dp), fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = 0f)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { }, modifier = Modifier.weight(1f)) {
                    Text("✂️ Tạo mẫu 10s & Nghe thử", textAlign = TextAlign.Center)
                }
                // Hidden button regen
            }

            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
            ) {
                Text("BẮT ĐẦU XỬ LÝ TOÀN BỘ", color = Color(0xFFFF0000), fontWeight = FontWeight.Bold)
            }

            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Lưu file kết quả")
            }
            OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Xóa file hiện tại")
            }
            TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                Text("Quay lại")
            }
        }
    }
}

