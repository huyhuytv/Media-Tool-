package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrimScreen(navController: NavController) {
    var hasOutput by remember { mutableStateOf(false) }
    var bookmarksVisible by remember { mutableStateOf(false) } // Represents "visibility" of the spinner

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CẮT ĐA ĐOẠN AUDIO / VIDEO", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
            // Hàng nút chức năng chính
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(contentColor = Color(0xFF00A0FF))
                ) {
                    Text("Chế độ: CẮT AUDIO", textAlign = TextAlign.Center)
                }
                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Chất lượng: Cao", textAlign = TextAlign.Center)
                }
            }

            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Chọn file cần cắt")
            }

            Text(text = "File: Chưa chọn", modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Điều khiển phát
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { }, modifier = Modifier.weight(1f)) {
                    Text("Phát file gốc")
                }
                Button(onClick = { }, modifier = Modifier.weight(1f)) {
                    Text("Xem thông tin file")
                }
            }

            // Thanh thời gian và vị trí hiện tại
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Vị trí hiện tại: 00:00", style = MaterialTheme.typography.bodyMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Thời gian: 00:00 / 00:00", style = MaterialTheme.typography.bodyMedium)
                    Slider(value = 0f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
                    Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                        Text("Lấy mốc thời gian hiện tại")
                    }
                }
            }

            // Nhập mốc bắt đầu / kết thúc
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Mốc bắt đầu (ms, ví dụ: 5000, 10000, 15000)")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Nhập mốc bắt đầu") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next)
                        )
                        Button(onClick = { }) {
                            Text("Lấy mốc bắt đầu hiện tại")
                        }
                    }

                    Text(text = "Mốc kết thúc (ms, ví dụ: 10000, 20000, 25000)")
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Nhập mốc kết thúc") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Done)
                        )
                        Button(onClick = { }) {
                            Text("Lấy mốc kết thúc hiện tại")
                        }
                    }
                }
            }

            // Nút hoàn tác và làm lại
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { }, modifier = Modifier.weight(1f)) {
                    Text("Hoàn tác")
                }
                OutlinedButton(onClick = { }, modifier = Modifier.weight(1f)) {
                    Text("Làm lại")
                }
            }

            // CÁC TÍNH NĂNG CẮT TRỰC TIẾP MỚI
            Text(
                text = "CẮT TRỰC TIẾP",
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                color = Color(0xFF00A0FF),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))
                ) {
                    Text("Chọn thời gian cắt trực tiếp", textAlign = TextAlign.Center)
                }
                Button(
                    onClick = { },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Mốc thời gian có sẵn", textAlign = TextAlign.Center)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { }, modifier = Modifier.weight(1f)) {
                    Text("Chia thành đoạn nhỏ", textAlign = TextAlign.Center)
                }
                FilledTonalButton(onClick = { }, modifier = Modifier.weight(1f)) {
                    Text("Cắt đoạn cách đều", textAlign = TextAlign.Center)
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { }, modifier = Modifier.weight(1f)) {
                    Text("Cắt theo phần trăm", textAlign = TextAlign.Center)
                }
                FilledTonalButton(onClick = { }, modifier = Modifier.weight(1f)) {
                    Text("Cắt bỏ đầu cuối", textAlign = TextAlign.Center)
                }
            }

            // Bookmark
            Text(
                text = "BOOKMARK - Đánh dấu vị trí quan trọng",
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                color = Color(0xFFFF6600),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { }, modifier = Modifier.weight(1f)) {
                    Text("Thêm bookmark tại vị trí hiện tại", textAlign = TextAlign.Center)
                }
                Button(onClick = { }, modifier = Modifier.weight(1f)) {
                    Text("Quản lý bookmark", textAlign = TextAlign.Center)
                }
            }

            if (bookmarksVisible) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded },
                        modifier = Modifier.weight(2f)
                    ) {
                        OutlinedTextField(
                            value = "-- Chọn bookmark để di chuyển đến --",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                            singleLine = true
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            DropdownMenuItem(text = { Text("-- Chọn bookmark để di chuyển đến --") }, onClick = { expanded = false })
                        }
                    }
                    Button(onClick = { }, modifier = Modifier.weight(1f)) {
                        Text("Di chuyển đến")
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { }, modifier = Modifier.weight(1f)) {
                    Text("Lấy bookmark làm mốc bắt đầu", textAlign = TextAlign.Center)
                }
                OutlinedButton(onClick = { }, modifier = Modifier.weight(1f)) {
                    Text("Lấy bookmark làm mốc kết thúc", textAlign = TextAlign.Center)
                }
            }

            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Tự động cắt theo các bookmark")
            }

            // Hiệu ứng
            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Hiệu ứng Fade (mờ dần đầu/cuối)")
            }

            Divider(modifier = Modifier.padding(vertical = 8.dp))

            // Trạng thái và tiến trình
            Text(text = "Sẵn sàng", modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center, fontWeight = FontWeight.Bold)
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = 0f)

            // Nút xử lý chính
            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Nghe thử các đoạn sẽ cắt")
            }

            Button(
                onClick = { hasOutput = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.errorContainer, contentColor = MaterialTheme.colorScheme.onErrorContainer)
            ) {
                Text("BẮT ĐẦU CẮT FILE", color = Color(0xFFFF0000), fontWeight = FontWeight.Bold)
            }

            if (hasOutput) {
                Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                    Text("Lưu file đã cắt")
                }
                Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                    Text("Xuất sang định dạng khác")
                }
            }

            OutlinedButton(onClick = { hasOutput = false }, modifier = Modifier.fillMaxWidth()) {
                Text("Xóa file hiện tại")
            }

            TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                Text("Quay lại")
            }
        }
    }
}

