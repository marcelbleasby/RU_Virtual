package com.bmo.mennu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bmo.mennu.ui.card.CardScreen
import com.bmo.mennu.ui.cardapio.CardapioScreen
import com.bmo.mennu.ui.home.HomeScreen
import com.bmo.mennu.ui.login.LoginScreen
import com.bmo.mennu.ui.navigation.MennuBottomBar
import com.bmo.mennu.ui.navigation.Screen
import com.bmo.mennu.ui.navigation.bottomNavItems
import com.bmo.mennu.ui.qrcode.QrCodeScreen
import com.bmo.mennu.ui.theme.MennuTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        setContent {
            MennuTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    Scaffold(
        bottomBar = {
            if (bottomNavItems.any { it.screen.route == currentRoute }) {
                MennuBottomBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Login.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Login.route) { LoginScreen(navController) }
            composable(Screen.Home.route) { HomeScreen(navController) }
            composable(Screen.Cardapio.route) { CardapioScreen(navController = navController) }
            composable(Screen.Cartao.route) { CardScreen(navController = navController) }
            composable(Screen.QrCode.route) { QrCodeScreen() }
        }
    }
}
