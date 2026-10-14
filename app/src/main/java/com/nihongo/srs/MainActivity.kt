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
import com.nihongo.srs.ui.home.HomeScreen
import com.nihongo.srs.ui.manage.ManageScreen
import com.nihongo.srs.ui.review.FlashcardScreen
import com.nihongo.srs.ui.review.ReviewViewModel
import com.nihongo.srs.ui.review.SessionMode
import com.nihongo.srs.ui.speak.SpeakScreen
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
    const val CARDS = "cards/{${ReviewViewModel.ARG_MODE}}"
    const val MANAGE = "manage"
    const val SPEAK = "speak"
    fun cards(mode: SessionMode) = "cards/${mode.name}"
}

@androidx.compose.runtime.Composable
private fun AppNavHost() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartReview = { nav.navigate(Routes.cards(SessionMode.REVIEW)) },
                onLearnNew = { nav.navigate(Routes.cards(SessionMode.LEARN)) },
                onManage = { nav.navigate(Routes.MANAGE) },
                onSpeak = { nav.navigate(Routes.SPEAK) },
            )
        }
        composable(
            Routes.CARDS,
            arguments = listOf(navArgument(ReviewViewModel.ARG_MODE) { type = NavType.StringType })
        ) {
            // The nav argument reaches ReviewViewModel through its SavedStateHandle.
            FlashcardScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.MANAGE) { ManageScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SPEAK) { SpeakScreen(onBack = { nav.popBackStack() }) }
    }
}
