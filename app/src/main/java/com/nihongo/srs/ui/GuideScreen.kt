package com.nihongo.srs.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Newspaper
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material.icons.rounded.Today
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.nihongo.srs.ui.components.BigButton
import com.nihongo.srs.ui.components.SoftCard
import com.nihongo.srs.ui.theme.Extra

/**
 * Shown on first launch (and from More → Guide): your name, and what each tab is for,
 * in plain Bangla, so nobody has to guess which page does what.
 */
@Composable
fun GuideScreen(initialName: String, onDone: (name: String) -> Unit) {
    var name by remember { mutableStateOf(initialName) }
    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("ようこそ! স্বাগতম!", style = MaterialTheme.typography.headlineLarge)
        Text(
            "এই অ্যাপে প্রতিদিন অল্প অল্প করে জাপানি শিখবেন। নিচে লেখা আছে কোন পেজে কী।",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            name, { name = it.take(20) }, singleLine = true,
            label = { Text("আপনার নাম (স্কোরবোর্ডে দেখাবে)") },
            modifier = Modifier.fillMaxWidth().testTag("guide_name"),
        )
        Text("নিচের ৫টি ট্যাব", style = MaterialTheme.typography.titleLarge)
        GuideRow(Icons.Rounded.Today, MaterialTheme.colorScheme.primary, "আজ",
            "প্রতিদিন এখানে শুরু করুন। ৩টি কাজ: পুরনো শব্দ রিভিউ, ১০টি নতুন শব্দ, ছোট পরীক্ষা।")
        GuideRow(Icons.Rounded.Newspaper, MaterialTheme.colorScheme.secondary, "খবর",
            "জাপানের আজকের খবর। যেকোনো শব্দে চাপ দিলে বাংলা ও ইংরেজি মানে আসবে। রেডিও খবরও শুনতে পারবেন।")
        GuideRow(Icons.Rounded.SportsEsports, Extra.streak, "খেলা",
            "কে কত পয়েন্ট পেল তার স্কোরবোর্ড, আর দুজনে মিলে কুইজ যুদ্ধ।")
        GuideRow(Icons.AutoMirrored.Rounded.MenuBook, Extra.correct, "শব্দ",
            "সব শব্দের বই: মানে, উচ্চারণ আর উদাহরণ বাক্য। বাংলায় খুঁজতেও পারবেন।")
        GuideRow(Icons.Rounded.Menu, MaterialTheme.colorScheme.onSurfaceVariant, "আরও",
            "কানা অক্ষর, কথা বলার অনুশীলন, বাংলিশে প্রশ্ন, অগ্রগতি আর রিমাইন্ডার।")
        SoftCard(color = MaterialTheme.colorScheme.primaryContainer, border = false, padding = 16.dp) {
            Text("কীভাবে মনে থাকে", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer)
            Text(
                "নতুন শব্দ ১ দিন পর, তারপর ৩ দিন পর, তারপর ৭ দিন পর আবার আসবে। ভুলে গেলে আবার কাল থেকে। " +
                    "তাই প্রতিদিন \"আজ\" ট্যাবের কাজগুলো শেষ করুন।",
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
        BigButton("শুরু করি", "Let's start", onClick = { onDone(name) }, modifier = Modifier.testTag("guide_start"))
    }
}

@Composable
private fun GuideRow(icon: ImageVector, tint: Color, title: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(tint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = tint) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
