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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var tabIndex by remember { mutableStateOf(0) }
    var useDefaultConfig by remember { mutableStateOf(true) }

    // State loaded from persistency
    var vidIndex by remember { mutableStateOf(com.example.core.SettingsManager.getVidQualityIndex(context)) }
    var audIndex by remember { mutableStateOf(com.example.core.SettingsManager.getAudBitrateIndex(context)) }
    var fmtIndex by remember { mutableStateOf(com.example.core.SettingsManager.getAudFormatIndex(context)) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cài đặt Chung", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            TabRow(selectedTabIndex = tabIndex) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = { tabIndex = 0 },
                    text = { Text("Chất lượng & Xuất") }
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = { tabIndex = 1 },
                    text = { Text("Cấu hình chung") }
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (tabIndex == 0) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Chất lượng Video đầu ra:", color = Color(0xFF00A0FF), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        
                        var expandedVid by remember { mutableStateOf(false) }
                        val vidList = listOf("2 Mbps (Nhẹ, tiết kiệm)", "5 Mbps (Mặc định, chuẩn đẹp)", "10 Mbps (Chất lượng cao)", "20 Mbps (Rất nét, File lớn)", "50 Mbps (Studio/4K, File siêu lớn)")
                        
                        ExposedDropdownMenuBox(expanded = expandedVid, onExpandedChange = { expandedVid = it }, modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = vidList.getOrNull(vidIndex) ?: vidList[1],
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVid) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = expandedVid, onDismissRequest = { expandedVid = false }) {
                                vidList.forEachIndexed { index, mode ->
                                    DropdownMenuItem(text = { Text(mode) }, onClick = { vidIndex = index; expandedVid = false })
                                }
                            }
                        }

                        Text("Chất lượng Âm thanh (Audio Bitrate):", color = Color(0xFF00AA00), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        var expandedAud by remember { mutableStateOf(false) }
                        val audList = listOf("128k (Cơ bản)", "192k (Khá)", "256k (Chất lượng cao)", "320k (Studio/Chuyên nghiệp)", "Giữ nguyên bản gốc / Lossless")
                        
                        ExposedDropdownMenuBox(expanded = expandedAud, onExpandedChange = { expandedAud = it }, modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = audList.getOrNull(audIndex) ?: audList[3],
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAud) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = expandedAud, onDismissRequest = { expandedAud = false }) {
                                audList.forEachIndexed { index, mode ->
                                    DropdownMenuItem(text = { Text(mode) }, onClick = { audIndex = index; expandedAud = false })
                                }
                            }
                        }

                        Text("Định dạng xuất âm thanh (Khi trích xuất/Audio):", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                        var expandedFmt by remember { mutableStateOf(false) }
                        val fmtList = listOf("AAC (.m4a) - Mặc định, tốt", "MP3 (.mp3) - Phổ thông", "WAV (.wav) - Không nén, file rất lớn", "FLAC (.flac) - Không nén Lossless")
                        
                        ExposedDropdownMenuBox(expanded = expandedFmt, onExpandedChange = { expandedFmt = it }, modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = fmtList.getOrNull(fmtIndex) ?: fmtList[0],
                                onValueChange = {},
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFmt) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(expanded = expandedFmt, onDismissRequest = { expandedFmt = false }) {
                                fmtList.forEachIndexed { index, mode ->
                                    DropdownMenuItem(text = { Text(mode) }, onClick = { fmtIndex = index; expandedFmt = false })
                                }
                            }
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = useDefaultConfig, onCheckedChange = { useDefaultConfig = it })
                        Text("Dùng cấu hình mặc định khi ghép/cắt", fontWeight = FontWeight.SemiBold)
                    }

                    if (useDefaultConfig) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Âm lượng Gốc/Video: 100%")
                                Slider(value = 1f, onValueChange = {}, modifier = Modifier.fillMaxWidth())

                                Text("Âm lượng Nền mặc định: 25%")
                                Slider(value = 0.25f, onValueChange = {}, modifier = Modifier.fillMaxWidth())

                                OutlinedTextField(value = "0", onValueChange = {}, label = { Text("Fade In chung (ms) - (Cắt, Gốc)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = "0", onValueChange = {}, label = { Text("Fade Out chung (ms) - (Cắt, Gốc)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                                OutlinedTextField(value = "0", onValueChange = {}, label = { Text("Fade In nền (ms) - (Chỉ ghép nhạc)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = "0", onValueChange = {}, label = { Text("Fade Out nền (ms) - (Chỉ ghép nhạc)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = false, onCheckedChange = {})
                                    Text("Lặp nhạc nền")
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = false, onCheckedChange = {})
                                    Text("Auto-Duck (Nhỏ nhạc khi có giọng)")
                                }

                                Text("Mức giảm nền: 60%")
                                Slider(value = 0.6f, onValueChange = {}, modifier = Modifier.fillMaxWidth())

                                Text("Ngưỡng giọng: 1500")
                                Slider(value = 0.5f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { 
                    com.example.core.SettingsManager.setVidQualityIndex(context, vidIndex)
                    com.example.core.SettingsManager.setAudBitrateIndex(context, audIndex)
                    com.example.core.SettingsManager.setAudFormatIndex(context, fmtIndex)
                    navController.popBackStack()
                }, modifier = Modifier.fillMaxWidth()) {
                    Text("LƯU CÀI ĐẶT & THOÁT")
                }
                TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                    Text("HỦY & QUAY LẠI")
                }
            }
        }
    }
}

