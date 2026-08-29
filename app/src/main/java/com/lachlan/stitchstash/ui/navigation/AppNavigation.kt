package com.lachlan.stitchstash.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lachlan.stitchstash.StitchStashApp
import com.lachlan.stitchstash.ui.components.TopLevelDestination
import com.lachlan.stitchstash.ui.finishcard.FinishCardGalleryScreen
import com.lachlan.stitchstash.ui.finishcard.FinishCardScreen
import com.lachlan.stitchstash.ui.home.HomeScreen
import com.lachlan.stitchstash.ui.log.LogCompletionScreen
import com.lachlan.stitchstash.ui.marketprep.MarketPrepScreen
import com.lachlan.stitchstash.ui.markets.MarketsScreen
import com.lachlan.stitchstash.ui.onboarding.OnboardingScreen
import com.lachlan.stitchstash.ui.patterns.AddPatternScreen
import com.lachlan.stitchstash.ui.patterns.EstimatePatternScreen
import com.lachlan.stitchstash.ui.patterns.PatternListScreen
import com.lachlan.stitchstash.ui.plan.PlanScreen
import com.lachlan.stitchstash.ui.settings.SettingsScreen
import com.lachlan.stitchstash.ui.stickers.StickerBookScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val ADD_PATTERN = "patterns/add"
    const val ESTIMATE_PATTERN = "patterns/estimate/{patternId}"
    const val LOG = "log"
    const val CARD_PREVIEW = "cards/{completionId}"

    fun estimatePattern(id: Long) = "patterns/estimate/$id"
    fun cardPreview(completionId: Long) = "cards/$completionId"
}

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val app = LocalContext.current.applicationContext as StitchStashApp
    val settings by app.repository.observeSettings().collectAsStateWithLifecycle(initialValue = null)

    val currentSettings = settings ?: run {
        LoadingScreen()
        return
    }

    val start = remember {
        if (currentSettings.onboardingComplete) TopLevelDestination.HOME.route else Routes.ONBOARDING
    }

    val navigateTopLevel: (TopLevelDestination) -> Unit = { dest ->
        nav.navigate(dest.route) {
            popUpTo(TopLevelDestination.HOME.route) {
                saveState = true
                inclusive = false
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = nav,
        startDestination = start,
        enterTransition = { fadeIn(tween(200)) },
        exitTransition = { fadeOut(tween(150)) },
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onComplete = {
                nav.navigate(TopLevelDestination.HOME.route) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }
        composable(TopLevelDestination.HOME.route) {
            HomeScreen(
                onLogFinish = { nav.navigate(Routes.LOG) },
                onNavigate = navigateTopLevel,
            )
        }
        composable(TopLevelDestination.PATTERNS.route) {
            PatternListScreen(
                onAddPattern = { nav.navigate(Routes.ADD_PATTERN) },
                onNavigate = navigateTopLevel,
                onEditEstimate = { id -> nav.navigate(Routes.estimatePattern(id)) },
            )
        }
        composable(Routes.ADD_PATTERN) {
            AddPatternScreen(
                onDone = { nav.popBackStack() },
                onBack = { nav.popBackStack() },
            )
        }
        composable(
            route = Routes.ESTIMATE_PATTERN,
            arguments = listOf(navArgument("patternId") { type = NavType.LongType }),
        ) { backStack ->
            EstimatePatternScreen(
                patternId = backStack.arguments?.getLong("patternId") ?: 0L,
                onBack = { nav.popBackStack() },
            )
        }
        composable(Routes.LOG) {
            LogCompletionScreen(
                onDone = { nav.popBackStack() },
                onBack = { nav.popBackStack() },
                onAddPattern = {
                    nav.navigate(Routes.ADD_PATTERN) {
                        popUpTo(Routes.LOG) { inclusive = true }
                    }
                },
                onCreateCard = { completionId ->
                    nav.navigate(Routes.cardPreview(completionId)) {
                        popUpTo(Routes.LOG) { inclusive = true }
                    }
                },
            )
        }
        composable(TopLevelDestination.PLAN.route) {
            PlanScreen(onNavigate = navigateTopLevel)
        }
        composable(TopLevelDestination.STICKERS.route) {
            StickerBookScreen(onNavigate = navigateTopLevel)
        }
        composable(TopLevelDestination.MARKETS.route) {
            MarketsScreen(onNavigate = navigateTopLevel)
        }
        composable(TopLevelDestination.MARKET_PREP.route) {
            MarketPrepScreen(onNavigate = navigateTopLevel)
        }
        composable(TopLevelDestination.SETTINGS.route) {
            SettingsScreen(onNavigate = navigateTopLevel)
        }
        composable(TopLevelDestination.CARDS.route) {
            FinishCardGalleryScreen(onNavigate = navigateTopLevel)
        }
        composable(
            route = Routes.CARD_PREVIEW,
            arguments = listOf(navArgument("completionId") { type = NavType.LongType }),
        ) { backStack ->
            FinishCardScreen(
                completionId = backStack.arguments?.getLong("completionId") ?: 0L,
                onBack = { nav.popBackStack() },
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}
