package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinScreen(navController: NavController) {
    var hasOutput by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("NỐI NHIỀU FILE (AUDIO / VIDEO)", fontSize = 18.sp, fontWeight = FontWeight.Bold) },
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
            Button(
                onClick = { },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = Color(0xFF00A0FF)
                )
            ) {
                Text("Đang ở chế độ: NỐI AUDIO (Bấm để đổi sang Video)", textAlign = TextAlign.Center)
            }

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Chưa có file nào được chọn.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    
                    Divider(modifier = Modifier.padding(vertical = 8.dp))
                    
                    Text(text = "Sẵn sàng", fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = 0f)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "Thời gian: 00:00.000 / 00:00.000", style = MaterialTheme.typography.bodyMedium)
                    Slider(value = 0f, onValueChange = {}, modifier = Modifier.fillMaxWidth())
                }
            }

            Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                Text("Chọn file để nối (Nhiều file)")
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilledTonalButton(onClick = { }, modifier = Modifier.weight(1f)) {
                    Text("Giảm lặp")
                }
                FilledTonalButton(onClick = { }, modifier = Modifier.weight(1f)) {
                    Text("Tăng lặp")
                }
            }

            Button(
                onClick = { hasOutput = true },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("BẮT ĐẦU NỐI FILE", color = Color(0xFFFF0000), fontWeight = FontWeight.Bold)
            }

            if (hasOutput) {
                Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                    Text("Phát file đã nối")
                }

                Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                    Text("Lưu file đã nối")
                }
            }

            OutlinedButton(onClick = { hasOutput = false }, modifier = Modifier.fillMaxWidth()) {
                Text("Xóa danh sách hiện tại")
            }

            TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                Text("Quay lại")
            }
        }
    }
}

