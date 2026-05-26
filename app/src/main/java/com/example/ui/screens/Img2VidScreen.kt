package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Img2VidScreen(navController: NavController) {
    var hasOutput by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var ratioIndex by remember { mutableStateOf(0) }
    val ratios = listOf("Ngang 16:9", "Dọc 9:16", "Vuông 1:1")

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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                    Text("Chọn Âm thanh")
                }
                Text("Chưa chọn", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                    Text("Thêm Ảnh (Nhiều file)")
                }
                // List container would go here
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

            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Sẵn sàng", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 8.dp))
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), progress = 0f)
                }
            }

            Button(
                onClick = { hasOutput = true },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            ) {
                Text("BẮT ĐẦU TẠO VIDEO", color = Color(0xFFFF0000), fontWeight = FontWeight.Bold)
            }

            if (hasOutput) {
                Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                    Text("Phát kết quả")
                }
                Button(onClick = { }, modifier = Modifier.fillMaxWidth()) {
                    Text("Lưu Video")
                }
            }

            OutlinedButton(onClick = { hasOutput = false }, modifier = Modifier.fillMaxWidth()) {
                Text("Xóa tất cả")
            }

            TextButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth()) {
                Text("Quay lại")
            }
        }
    }
}

