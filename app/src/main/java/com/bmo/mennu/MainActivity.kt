package com.bmo.mennu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.key
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.bmo.mennu.ui.sessao.SessaoViewModel
import com.bmo.mennu.ui.sessao.ContextoScreen
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
fun AppNavigation(viewModel: SessaoViewModel = hiltViewModel()) {
    val sessao by viewModel.sessao.collectAsStateWithLifecycle()
    val carregando by viewModel.carregando.collectAsStateWithLifecycle()
    val erro by viewModel.erro.collectAsStateWithLifecycle()
    val escolhendo by viewModel.escolhendo.collectAsStateWithLifecycle()
    val restaurando by viewModel.restaurando.collectAsStateWithLifecycle()
    if (sessao.token != null && (restaurando && !sessao.validada || sessao.validada && sessao.selecionado == null || escolhendo || carregando)) {
        ContextoScreen(sessao, carregando, erro, viewModel::selecionar, viewModel::atualizar, viewModel::sair)
        return
    }
    // Recriar navegação cancela ViewModels/cargas da seleção anterior.
    key(if (sessao.validada) sessao.token else null, if (sessao.validada) sessao.selecionado?.empresaId else null, if (sessao.validada) sessao.selecionado?.unidadeId else null) {
        val navController = rememberNavController()
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

        Scaffold(
            topBar = {
                if (sessao.validada && sessao.token != null) TextButton(onClick = viewModel::trocar) {
                    Text("${sessao.selecionado?.unidadeNome ?: "Unidade"} · Trocar unidade")
                }
            },
            bottomBar = {
                if (bottomNavItems.any { it.screen.route == currentRoute }) {
                    MennuBottomBar(navController = navController, currentRoute = currentRoute)
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = if (sessao.validada && sessao.token != null) Screen.Home.route else Screen.Login.route,
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

}
