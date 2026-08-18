package com.bmo.mennu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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

    val sessionViewModel: SessionViewModel = hiltViewModel()
    val sessionInvalidated by sessionViewModel.sessionInvalidated.collectAsStateWithLifecycle()

    // Token/user salvos de um login anterior: pula a tela de login e vai direto
    // pra Home, mesmo offline. SessionViewModel valida a sessão contra o servidor
    // em paralelo (ver sessionInvalidated abaixo) — só desloga se o servidor
    // confirmar que a sessão morreu, nunca por falta de conexão.
    val startDestination = if (sessionViewModel.hasSavedSession) Screen.Home.route else Screen.Login.route

    LaunchedEffect(sessionInvalidated) {
        if (sessionInvalidated) {
            navController.navigate(Screen.Login.route) {
                popUpTo(navController.graph.id) { inclusive = true }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (bottomNavItems.any { it.screen.route == currentRoute }) {
                MennuBottomBar(navController = navController, currentRoute = currentRoute)
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
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
