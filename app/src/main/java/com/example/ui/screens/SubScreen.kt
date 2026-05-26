package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubScreen(navController: NavController) {
    var autoDuck by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Đọc & Trích xuất Phụ đề", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("[1] CHỌN VIDEO", color = Color(0xFF00A0FF), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Chọn Video")
            }
            Text("Video: Chưa chọn", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))

            Text("[2] CHỌN PHỤ ĐỀ ĐỂ ĐỌC", color = Color(0xFF00AA00), fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Chọn File Phụ đề (.srt / .vtt)")
            }
            Text("Phụ đề: Chưa chọn", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(bottom = 8.dp))

            Text("--- ĐIỀU KHIỂN TRÌNH PHÁT ---", color = Color(0xFFFF8800), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            Button(onClick = { }, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text("Phát Video (Chỉ âm thanh)")
            }
            
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Thời gian: 00:00 / 00:00")
                    Slider(value = 0f, onValueChange = {}, modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp))
                    Text("Âm lượng Video: 100%")
                    Slider(value = 1f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
                }
            }

            Text("--- CÀI ĐẶT GIỌNG ĐỌC (TTS) ---", color = Color(0xFFFF8800), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 4.dp)) {
                Checkbox(checked = autoDuck, onCheckedChange = { autoDuck = it })
                Text("Auto-Duck (Tự động nhỏ tiếng Video khi đọc Phụ đề)", fontSize = 14.sp)
            }
            Text("Tốc độ đọc: 1.0x", modifier = Modifier.padding(horizontal = 16.dp))
            Slider(value = 0.5f, onValueChange = {}, modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp))

            Text("--- TRÍCH XUẤT PHỤ ĐỀ CÓ SẴN ---", color = Color(0xFFDD0000), fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
            ) {
                Text("TRÍCH XUẤT PHỤ ĐỀ TỪ VIDEO")
            }
            Text("Sẵn sàng", modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))

            OutlinedButton(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Xóa & Đặt lại")
            }
            TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                Text("Quay lại")
            }
        }
    }
}

