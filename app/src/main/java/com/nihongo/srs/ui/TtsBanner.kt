package com.nihongo.srs.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.nihongo.srs.speech.JapaneseSpeaker

/** Shows a hint + install button when the phone has no Japanese TTS voice. */
@Composable
fun TtsBanner(status: JapaneseSpeaker.Status, modifier: Modifier = Modifier) {
    if (status != JapaneseSpeaker.Status.MISSING_VOICE &&
        status != JapaneseSpeaker.Status.UNAVAILABLE
    ) return
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        )
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "Japanese voice not installed. Install it free to hear pronunciation.\n" +
                    "জাপানি ভয়েস ইনস্টল নেই। উচ্চারণ শুনতে ফ্রি ইনস্টল করুন।",
                style = MaterialTheme.typography.bodyMedium
            )
            TextButton(onClick = {
                runCatching { context.startActivity(JapaneseSpeaker.installVoiceIntent()) }
            }) { Text("Install voice • ভয়েস ইনস্টল") }
        }
    }
}
