package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current

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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    var expandedVid by remember { mutableStateOf(false) }
                    val vidList = listOf("2 Mbps (Nhẹ, tiết kiệm)", "5 Mbps (Mặc định, chuẩn đẹp)", "10 Mbps (Chất lượng cao)", "20 Mbps (Rất nét, File lớn)", "50 Mbps (Studio/4K, File siêu lớn)")
                    
                    ExposedDropdownMenuBox(expanded = expandedVid, onExpandedChange = { expandedVid = it }, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = vidList.getOrNull(vidIndex) ?: vidList[1],
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Chất lượng Video đầu ra", color = Color(0xFF00A0FF), fontWeight = FontWeight.SemiBold) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedVid) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedVid, onDismissRequest = { expandedVid = false }) {
                            vidList.forEachIndexed { index, mode ->
                                DropdownMenuItem(text = { Text(mode) }, onClick = { vidIndex = index; expandedVid = false })
                            }
                        }
                    }

                    var expandedAud by remember { mutableStateOf(false) }
                    val audList = listOf("128k (Cơ bản)", "192k (Khá)", "256k (Chất lượng cao)", "320k (Studio/Chuyên nghiệp)", "Giữ nguyên bản gốc / Lossless")
                    
                    ExposedDropdownMenuBox(expanded = expandedAud, onExpandedChange = { expandedAud = it }, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = audList.getOrNull(audIndex) ?: audList[3],
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Chất lượng Âm thanh (Audio Bitrate)", color = Color(0xFF00AA00), fontWeight = FontWeight.SemiBold) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedAud) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedAud, onDismissRequest = { expandedAud = false }) {
                            audList.forEachIndexed { index, mode ->
                                DropdownMenuItem(text = { Text(mode) }, onClick = { audIndex = index; expandedAud = false })
                            }
                        }
                    }

                    var expandedFmt by remember { mutableStateOf(false) }
                    val fmtList = listOf("AAC (.m4a) - Mặc định, tốt", "MP3 (.mp3) - Phổ thông", "WAV (.wav) - Không nén, file rất lớn", "FLAC (.flac) - Không nén Lossless")
                    
                    ExposedDropdownMenuBox(expanded = expandedFmt, onExpandedChange = { expandedFmt = it }, modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = fmtList.getOrNull(fmtIndex) ?: fmtList[0],
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Định dạng xuất âm thanh (Khi trích xuất/Audio)", fontWeight = FontWeight.SemiBold) },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedFmt) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(expanded = expandedFmt, onDismissRequest = { expandedFmt = false }) {
                            fmtList.forEachIndexed { index, mode ->
                                DropdownMenuItem(text = { Text(mode) }, onClick = { fmtIndex = index; expandedFmt = false })
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Gỡ lỗi & Hỗ trợ", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    OutlinedButton(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            try {
                                val downloadsDir = context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS)
                                val logFile = java.io.File(downloadsDir, "audio_separator_log.txt")
                                if (logFile.exists()) {
                                    val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", logFile)
                                    val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(android.content.Intent.EXTRA_STREAM, uri)
                                        addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(android.content.Intent.createChooser(intent, "Chia sẻ Nhật ký"))
                                } else {
                                    android.widget.Toast.makeText(context, "Chưa có file nhật ký nào.", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                android.widget.Toast.makeText(context, "Lỗi khi chia sẻ: ${e.message}", android.widget.Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Text("Chia sẻ File Nhật ký (Log) Tách Audio")
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

