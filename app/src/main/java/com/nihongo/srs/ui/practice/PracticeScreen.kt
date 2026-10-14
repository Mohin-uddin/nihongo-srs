package com.nihongo.srs.ui.practice

import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.nihongo.srs.NihongoApp
import com.nihongo.srs.data.AppPrefs
import com.nihongo.srs.reminder.ReminderScheduler
import com.nihongo.srs.ui.components.AppScreen
import com.nihongo.srs.ui.components.IconBadge
import com.nihongo.srs.ui.components.MenuTile
import com.nihongo.srs.ui.components.ScreenPadding
import com.nihongo.srs.ui.components.SectionTitle
import com.nihongo.srs.ui.components.SoftCard
import com.nihongo.srs.ui.exam.ExamScope
import com.nihongo.srs.ui.home.ReminderUi
import com.nihongo.srs.ui.theme.Extra
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Daily reminder setting for the More tab. */
class MoreViewModel(private val app: NihongoApp, private val prefs: AppPrefs) : ViewModel() {
    private val _reminder = MutableStateFlow(ReminderUi(prefs.reminderEnabled, prefs.reminderHour, prefs.reminderMinute))
    val reminder: StateFlow<ReminderUi> = _reminder.asStateFlow()

    fun setReminder(enabled: Boolean, hour: Int = prefs.reminderHour, minute: Int = prefs.reminderMinute) {
        prefs.reminderEnabled = enabled
        prefs.reminderHour = hour
        prefs.reminderMinute = minute
        _reminder.value = ReminderUi(enabled, hour, minute)
        ReminderScheduler.schedule(app, enabled, hour, minute, replace = true)
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as NihongoApp
                MoreViewModel(app, app.prefs)
            }
        }
    }
}

/** The "আরও" (More) tab: extra practice, progress and settings, each with one clear line. */
@Composable
fun PracticeScreen(
    onExam: (ExamScope) -> Unit,
    onSpeak: () -> Unit,
    onKana: () -> Unit,
    onAsk: () -> Unit,
    onProgress: () -> Unit,
    onManage: () -> Unit,
    onGuide: () -> Unit,
    vm: MoreViewModel = viewModel(factory = MoreViewModel.Factory),
) {
    val reminder by vm.reminder.collectAsStateWithLifecycle()
    val context = LocalContext.current

    AppScreen(title = "আরও", subtitle = "More · বাড়তি অনুশীলন, অগ্রগতি ও সেটিংস") { pad ->
        Column(
            Modifier.padding(pad).fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = ScreenPadding, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionTitle("কথা বলা ও পড়া", "Speak and read")
            MenuTile(Icons.Rounded.Forum, "প্রতিদিনের কথা", "Conversation · শুনুন, বলুন, মাইকে যাচাই",
                onSpeak, Modifier.testTag("practice_speak"),
                tint = MaterialTheme.colorScheme.secondary, tintBackground = MaterialTheme.colorScheme.secondaryContainer)
            MenuTile(Icons.Rounded.AutoAwesome, "বাংলিশে জিজ্ঞেস করুন", "Ask in Banglish · লিখুন, জাপানি দেখুন",
                onAsk, Modifier.testTag("practice_ask"),
                tint = MaterialTheme.colorScheme.secondary, tintBackground = MaterialTheme.colorScheme.secondaryContainer)
            MenuTile(Icons.Rounded.Translate, "হিরাগানা ও কাতাকানা", "Kana · জাপানি অক্ষর ও কুইজ",
                onKana, Modifier.testTag("practice_kana"),
                tint = Extra.streak, tintBackground = Extra.streak.copy(alpha = 0.15f))

            SectionTitle("পরীক্ষা", "Exams")
            MenuTile(Icons.Rounded.AllInclusive, "সব শব্দের পরীক্ষা", "Full exam · এ পর্যন্ত শেখা সব শব্দ থেকে",
                { onExam(ExamScope.ALL) }, Modifier.testTag("practice_exam_all"),
                tint = Extra.correct, tintBackground = Extra.correctSoft)

            SectionTitle("আমার হিসাব", "Progress and settings")
            MenuTile(Icons.Rounded.Insights, "আমার অগ্রগতি", "Progress · কত শিখলাম, কত দিন পড়লাম",
                onProgress, Modifier.testTag("more_progress"))
            MenuTile(Icons.Rounded.EditNote, "নিজের শব্দ যোগ করুন", "Add words · নতুন শব্দ লিখে রাখুন",
                onManage, Modifier.testTag("more_manage"))
            SoftCard(padding = 16.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Rounded.NotificationsActive, MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f).clickable(enabled = reminder.enabled) {
                        TimePickerDialog(context, { _, h, m -> vm.setReminder(true, h, m) },
                            reminder.hour, reminder.minute, false).show()
                    }) {
                        Text("প্রতিদিনের রিমাইন্ডার", style = MaterialTheme.typography.titleMedium)
                        Text(if (reminder.enabled) "প্রতিদিন ${reminder.label} · সময় বদলাতে চাপুন" else "বন্ধ",
                            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = reminder.enabled, onCheckedChange = { vm.setReminder(it) })
                }
            }
            MenuTile(Icons.AutoMirrored.Rounded.HelpOutline, "কোন পেজে কী আছে", "Guide · অ্যাপের গাইড আবার দেখুন",
                onGuide, Modifier.testTag("more_guide"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant, tintBackground = MaterialTheme.colorScheme.surfaceContainerHigh)
            Spacer(Modifier.height(8.dp))
        }
    }
}
