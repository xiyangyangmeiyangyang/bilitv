package com.openclaw.bilitv

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.openclaw.bilitv.data.repository.BiliRepository
import com.openclaw.bilitv.ui.screens.DetailScreen
import com.openclaw.bilitv.ui.screens.CategoryDirectoryScreen
import com.openclaw.bilitv.ui.screens.CategoryVideoScreen
import com.openclaw.bilitv.ui.screens.HomeScreen
import com.openclaw.bilitv.ui.screens.PlayerScreen
import com.openclaw.bilitv.ui.screens.SearchScreen
import com.openclaw.bilitv.ui.screens.SettingsScreen
import com.openclaw.bilitv.ui.screens.UpSpaceScreen
import com.openclaw.bilitv.ui.theme.BiliTvTheme
import com.openclaw.bilitv.ui.player.PlayerKeyDispatcher

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BiliRepository.initialize(applicationContext)
        setContent {
            BiliTvApp()
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (PlayerKeyDispatcher.dispatch(event)) {
            return true
        }
        if (
            event.action == KeyEvent.ACTION_DOWN &&
            event.repeatCount == 0 &&
            (event.keyCode == KeyEvent.KEYCODE_ESCAPE || event.keyCode == KeyEvent.KEYCODE_BUTTON_B)
        ) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.dispatchKeyEvent(event)
    }
}

@Composable
fun BiliTvApp() {
    BiliTvTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = "home") {
            composable("home") {
                HomeScreen(navController)
            }
            composable("search") {
                SearchScreen(navController)
            }
            composable("settings") {
                SettingsScreen(navController)
            }
            composable("categories") {
                CategoryDirectoryScreen(navController)
            }
            composable(
                route = "category/{rid}/{name}",
                arguments = listOf(
                    navArgument("rid") { type = NavType.IntType },
                    navArgument("name") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                CategoryVideoScreen(
                    rid = backStackEntry.arguments?.getInt("rid") ?: 0,
                    name = backStackEntry.arguments?.getString("name").orEmpty(),
                    navController = navController
                )
            }
            composable(
                route = "detail/{videoId}",
                arguments = listOf(navArgument("videoId") { type = NavType.StringType })
            ) { backStackEntry ->
                DetailScreen(
                    videoId = backStackEntry.arguments?.getString("videoId").orEmpty(),
                    navController = navController
                )
            }
            composable(
                route = "player/{videoId}",
                arguments = listOf(navArgument("videoId") { type = NavType.StringType })
            ) { backStackEntry ->
                PlayerScreen(
                    videoId = backStackEntry.arguments?.getString("videoId").orEmpty(),
                    navController = navController
                )
            }
            composable(
                route = "up/{videoId}",
                arguments = listOf(navArgument("videoId") { type = NavType.StringType })
            ) { backStackEntry ->
                UpSpaceScreen(
                    videoId = backStackEntry.arguments?.getString("videoId").orEmpty(),
                    navController = navController
                )
            }
        }
    }
}
