package com.nihongo.srs

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nihongo.srs.ui.exam.ExamScreen
import com.nihongo.srs.ui.exam.ExamViewModel
import com.nihongo.srs.ui.home.HomeScreen
import com.nihongo.srs.ui.kana.KanaScreen
import com.nihongo.srs.ui.progress.ProgressScreen
import com.nihongo.srs.ui.manage.ManageScreen
import com.nihongo.srs.ui.review.FlashcardScreen
import com.nihongo.srs.ui.review.ReviewViewModel
import com.nihongo.srs.ui.review.SessionMode
import com.nihongo.srs.ui.speak.SpeakScreen
import com.nihongo.srs.ui.study.StudyListScreen
import com.nihongo.srs.ui.theme.NihongoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NihongoTheme {
                Surface(Modifier.fillMaxSize()) { AppNavHost() }
            }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val CARDS = "cards/{${ReviewViewModel.ARG_MODE}}/{${ReviewViewModel.ARG_LEVEL}}"
    const val EXAM = "exam/{${ExamViewModel.ARG_SCOPE}}"
    const val MY_WORDS = "my_words"
    const val PROGRESS = "progress"
    const val KANA = "kana"
    const val MANAGE = "manage"
    const val SPEAK = "speak"
    fun cards(mode: SessionMode, level: String = "ALL") = "cards/${mode.name}/$level"
    fun exam(scope: com.nihongo.srs.ui.exam.ExamScope) = "exam/${scope.name}"
}

@androidx.compose.runtime.Composable
private fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartReview = { nav.navigate(Routes.cards(SessionMode.REVIEW)) },
                onLearnNew = { level -> nav.navigate(Routes.cards(SessionMode.LEARN, level)) },
                onMyWords = { nav.navigate(Routes.MY_WORDS) },
                onExam = { scope -> nav.navigate(Routes.exam(scope)) },
                onProgress = { nav.navigate(Routes.PROGRESS) },
                onKana = { nav.navigate(Routes.KANA) },
                onManage = { nav.navigate(Routes.MANAGE) },
                onSpeak = { nav.navigate(Routes.SPEAK) },
            )
        }
        composable(
            Routes.CARDS,
            arguments = listOf(
                navArgument(ReviewViewModel.ARG_MODE) { type = NavType.StringType },
                navArgument(ReviewViewModel.ARG_LEVEL) { type = NavType.StringType },
            )
        ) {
            // The nav argument reaches ReviewViewModel through its SavedStateHandle.
            FlashcardScreen(onBack = { nav.popBackStack() })
        }
        composable(
            Routes.EXAM,
            arguments = listOf(navArgument(ExamViewModel.ARG_SCOPE) { type = NavType.StringType })
        ) { ExamScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.PROGRESS) { ProgressScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.KANA) { KanaScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.MY_WORDS) { StudyListScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.MANAGE) { ManageScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SPEAK) { SpeakScreen(onBack = { nav.popBackStack() }) }
    }
}
