package com.nihongo.srs

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Insights
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.nihongo.srs.ui.ask.AskScreen
import com.nihongo.srs.ui.exam.ExamScope
import com.nihongo.srs.ui.exam.ExamScreen
import com.nihongo.srs.ui.exam.ExamViewModel
import com.nihongo.srs.ui.home.HomeScreen
import com.nihongo.srs.ui.kana.KanaScreen
import com.nihongo.srs.ui.manage.ManageScreen
import com.nihongo.srs.ui.news.ArticleScreen
import com.nihongo.srs.ui.news.ArticleViewModel
import com.nihongo.srs.ui.news.NewsListScreen
import com.nihongo.srs.ui.news.RadioScreen
import com.nihongo.srs.ui.players.BattleScreen
import com.nihongo.srs.ui.players.PlayersScreen
import com.nihongo.srs.ui.practice.PracticeScreen
import com.nihongo.srs.ui.progress.ProgressScreen
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
            NihongoTheme { AppRoot() }
        }
    }
}

private object Routes {
    const val HOME = "home"
    const val PRACTICE = "practice"
    const val WORDS = "words"
    const val PROGRESS = "progress"
    const val CARDS = "cards/{${ReviewViewModel.ARG_MODE}}/{${ReviewViewModel.ARG_LEVEL}}"
    const val EXAM = "exam/{${ExamViewModel.ARG_SCOPE}}"
    const val KANA = "kana"
    const val MANAGE = "manage"
    const val SPEAK = "speak"
    const val ASK = "ask"
    const val NEWS = "news"
    const val ARTICLE = "article/{${ArticleViewModel.ARG_LINK}}"
    const val RADIO = "radio"
    const val PLAYERS = "players"
    const val BATTLE = "battle"
    fun article(link: String) = "article/${Uri.encode(link)}"
    fun cards(mode: SessionMode, level: String = "ALL") = "cards/${mode.name}/$level"
    fun exam(scope: ExamScope) = "exam/${scope.name}"
}

private data class Tab(val route: String, val en: String, val bn: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", "হোম", Icons.Rounded.Home),
    Tab(Routes.PRACTICE, "Practice", "অনুশীলন", Icons.Rounded.School),
    Tab(Routes.WORDS, "Words", "শব্দ", Icons.AutoMirrored.Rounded.MenuBook),
    Tab(Routes.PROGRESS, "Progress", "অগ্রগতি", Icons.Rounded.Insights),
)

@Composable
private fun AppRoot() {
    val nav = rememberNavController()
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val showTabs = tabs.any { it.route == route }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.navigationBars,
        bottomBar = { if (showTabs) BottomTabs(nav, route) },
    ) { pad ->
        AppNavHost(nav, Modifier.padding(pad))
    }
}

@Composable
private fun BottomTabs(nav: NavHostController, route: String?) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface, tonalElevation = 0.dp) {
        tabs.forEach { tab ->
            NavigationBarItem(
                selected = route == tab.route,
                onClick = { nav.goToTab(tab.route) },
                icon = { Icon(tab.icon, null) },
                label = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(tab.en, style = MaterialTheme.typography.labelMedium)
                        Text(tab.bn, style = MaterialTheme.typography.labelSmall)
                    }
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier.testTag("tab_${tab.route}"),
            )
        }
    }
}

private fun NavHostController.goToTab(route: String) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = true }
    launchSingleTop = true
    restoreState = true
}

@Composable
private fun AppNavHost(nav: NavHostController, modifier: Modifier) {
    NavHost(navController = nav, startDestination = Routes.HOME, modifier = modifier.fillMaxSize()) {
        composable(Routes.HOME) {
            HomeScreen(
                onStartReview = { nav.navigate(Routes.cards(SessionMode.REVIEW)) },
                onLearnNew = { level -> nav.navigate(Routes.cards(SessionMode.LEARN, level)) },
                onExam = { scope -> nav.navigate(Routes.exam(scope)) },
                onSpeak = { nav.navigate(Routes.SPEAK) },
                onKana = { nav.navigate(Routes.KANA) },
                onAsk = { nav.navigate(Routes.ASK) },
                onNews = { nav.navigate(Routes.NEWS) },
                onRadio = { nav.navigate(Routes.RADIO) },
                onPlayers = { nav.navigate(Routes.PLAYERS) },
                onBattle = { nav.navigate(Routes.BATTLE) },
                onMyWords = { nav.goToTab(Routes.WORDS) },
                onProgress = { nav.goToTab(Routes.PROGRESS) },
            )
        }
        composable(Routes.PRACTICE) {
            PracticeScreen(
                onExam = { scope -> nav.navigate(Routes.exam(scope)) },
                onSpeak = { nav.navigate(Routes.SPEAK) },
                onKana = { nav.navigate(Routes.KANA) },
                onReview = { nav.navigate(Routes.cards(SessionMode.REVIEW)) },
                onAsk = { nav.navigate(Routes.ASK) },
                onNews = { nav.navigate(Routes.NEWS) },
                onRadio = { nav.navigate(Routes.RADIO) },
                onBattle = { nav.navigate(Routes.BATTLE) },
                onPlayers = { nav.navigate(Routes.PLAYERS) },
            )
        }
        composable(Routes.WORDS) { StudyListScreen(onManage = { nav.navigate(Routes.MANAGE) }) }
        composable(Routes.PROGRESS) { ProgressScreen() }
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
        composable(Routes.KANA) { KanaScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.MANAGE) { ManageScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.SPEAK) { SpeakScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.ASK) { AskScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.NEWS) {
            NewsListScreen(
                onBack = { nav.popBackStack() },
                onOpen = { nav.navigate(Routes.article(it.link)) },
                onRadio = { nav.navigate(Routes.RADIO) },
                onAddKey = { nav.navigate(Routes.ASK) },
            )
        }
        composable(
            Routes.ARTICLE,
            arguments = listOf(navArgument(ArticleViewModel.ARG_LINK) { type = NavType.StringType })
        ) { ArticleScreen(onBack = { nav.popBackStack() }, onAddKey = { nav.navigate(Routes.ASK) }) }
        composable(Routes.RADIO) { RadioScreen(onBack = { nav.popBackStack() }) }
        composable(Routes.PLAYERS) {
            PlayersScreen(onBack = { nav.popBackStack() }, onBattle = { nav.navigate(Routes.BATTLE) })
        }
        composable(Routes.BATTLE) {
            BattleScreen(onBack = { nav.popBackStack() }, onPlayers = { nav.navigate(Routes.PLAYERS) })
        }
    }
}
