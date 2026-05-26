package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun MainScreen(
    onNavigateToRecord: () -> Unit,
    onNavigateToTrim: () -> Unit,
    onNavigateToJoin: () -> Unit,
    onNavigateToMix: () -> Unit,
    onNavigateToImg2Vid: () -> Unit,
    onNavigateToSub: () -> Unit,
    onNavigateToOther: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "CÔNG CỤ XỬ LÝ MEDIA (AUDIO & VIDEO)",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00A0FF),
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .semantics { 
                        heading()
                        contentDescription = "Tiêu đề màn hình: Công cụ xử lý media, audio và video"
                    }
            )

            AccessibleButton(
                text = "🎙 GHI ÂM TRỰC TIẾP",
                contentDesc = "Mở mục ghi âm trực tiếp",
                onClick = onNavigateToRecord,
                colors = ButtonDefaults.buttonColors(contentColor = Color(0xFFFF0000))
            )

            Spacer(modifier = Modifier.height(12.dp))

            AccessibleButton(
                text = "✂ CẮT ĐA ĐOẠN",
                contentDesc = "Mở công cụ cắt audio hoặc video nhiều đoạn",
                onClick = onNavigateToTrim
            )

            Spacer(modifier = Modifier.height(12.dp))

            AccessibleButton(
                text = "🔗 NỐI NHIỀU FILE",
                contentDesc = "Mở công cụ nối nhiều tệp lại với nhau",
                onClick = onNavigateToJoin
            )

            Spacer(modifier = Modifier.height(12.dp))

            AccessibleButton(
                text = "🎵 GHÉP NHẠC & TỰ ĐỘNG GIẢM NỀN",
                contentDesc = "Mở công cụ ghép nhạc và tự động giảm âm lượng nền",
                onClick = onNavigateToMix,
                colors = ButtonDefaults.buttonColors(contentColor = Color(0xFFDD0000))
            )

            Spacer(modifier = Modifier.height(12.dp))

            AccessibleButton(
                text = "⚙ TÍNH NĂNG KHÁC (HIỆU ỨNG/TRÍCH XUẤT)",
                contentDesc = "Mở danh sách các tính năng khác như thêm hiệu ứng hoặc trích xuất",
                onClick = onNavigateToOther
            )

            Spacer(modifier = Modifier.height(12.dp))

            AccessibleButton(
                text = "🖼 GHÉP ẢNH VÀO ÂM THANH",
                contentDesc = "Mở công cụ ghép hình ảnh vào mốc thời gian của âm thanh để tạo video",
                onClick = onNavigateToImg2Vid,
                colors = ButtonDefaults.buttonColors(contentColor = Color(0xFF00AA00))
            )

            Spacer(modifier = Modifier.height(12.dp))

            AccessibleButton(
                text = "📝 ĐỌC & TRÍCH XUẤT PHỤ ĐỀ",
                contentDesc = "Mở công cụ hỗ trợ đọc và trích xuất phụ đề",
                onClick = onNavigateToSub,
                colors = ButtonDefaults.buttonColors(contentColor = Color(0xFF8800FF))
            )

            Spacer(modifier = Modifier.height(24.dp))

            AccessibleButton(
                text = "🛠 CÀI ĐẶT CHUNG",
                contentDesc = "Mở cấu hình cài đặt chung của ứng dụng",
                onClick = onNavigateToSettings
            )
        }
    }
}

@Composable
fun AccessibleButton(
    text: String,
    contentDesc: String,
    onClick: () -> Unit,
    colors: ButtonColors = ButtonDefaults.buttonColors(),
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        colors = colors,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp) // Minimum touch target size for accessibility
            .semantics(mergeDescendants = true) {
                contentDescription = contentDesc
                role = Role.Button
            }
    ) {
        // Clear semantics so TalkBack won't read the raw text with weird emojis
        Text(
            text = text, 
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}
