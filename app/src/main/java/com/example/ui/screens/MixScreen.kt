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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixScreen(navController: NavController) {
    var muteVideo by remember { mutableStateOf(false) }
    var loopBg by remember { mutableStateOf(false) }
    var autoDuck by remember { mutableStateOf(false) }
    var hasOutput by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ghép Nhạc Đa Luồng", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = Color(0xFF00A0FF)
                )
            ) {
                Text("Đang ở chế độ: GHÉP AUDIO (Bấm để đổi sang Video)", textAlign = TextAlign.Center)
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("MỞ LIVE CONSOLE", fontSize = 12.sp, color = Color(0xFF00A0FF), textAlign = TextAlign.Center)
                }
                OutlinedButton(
                    onClick = { },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("PAN (TRÁI/PHẢI)", fontSize = 12.sp, color = Color(0xFFFF8800), textAlign = TextAlign.Center)
                }
            }

            Text(
                text = "[1] FILE GỐC",
                color = Color(0xFFDD0000),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 8.dp)
            )

            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Chọn File gốc")
            }

            Text("Gốc: Chưa chọn", color = MaterialTheme.colorScheme.onSurfaceVariant)

            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Mốc Bắt đầu Gốc (ms)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Lấy mốc bắt đầu hiện tại")
            }

            OutlinedTextField(
                value = "",
                onValueChange = {},
                placeholder = { Text("Mốc Kết thúc Gốc (ms)") },
                modifier = Modifier.fillMaxWidth()
            )

            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Lấy mốc kết thúc hiện tại")
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = muteVideo, onCheckedChange = { muteVideo = it })
                Text("Tắt âm thanh gốc của Video", fontSize = 14.sp)
            }

            Text("Âm lượng Gốc: 100%")
            Slider(value = 1f, onValueChange = {}, valueRange = 0f..1.5f, modifier = Modifier.fillMaxWidth())

            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Phát File Gốc")
            }
            
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("00:00 / 00:00")
                    Slider(value = 0f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
                }
            }

            Text(
                text = "[2] DANH SÁCH NHẠC NỀN",
                color = Color(0xFF00AA00),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 16.dp)
            )

            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Thêm Nhạc nền (Có thể chọn nhiều)")
            }

            Text("Chưa có bản nhạc nền nào.", color = Color(0xFF888888))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = loopBg, onCheckedChange = { loopBg = it })
                Text("Lặp lại nhạc nền", fontSize = 14.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = autoDuck, onCheckedChange = { autoDuck = it })
                Text("Auto-Ducking (Nhỏ nhạc khi có giọng)", fontSize = 14.sp)
            }

            Divider(modifier = Modifier.padding(vertical = 4.dp))
            
            Text(text = "Sẵn sàng", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = 0f)

            Button(
                onClick = { hasOutput = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("BẮT ĐẦU GHÉP", color = Color(0xFFFF0000), fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = { },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Nghe thử sơ bộ")
            }

            if (hasOutput) {
                Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                    Text("Lưu File")
                }
            }

            OutlinedButton(onClick = { hasOutput = false }, modifier = Modifier.fillMaxWidth()) {
                Text("Xóa cấu hình")
            }

            TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                Text("Quay lại")
            }
        }
    }
}
