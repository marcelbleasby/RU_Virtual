package com.example.ruvirtual

import com.example.ruvirtual.ui.card.CardScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ruvirtual.ui.login.LoginScreen
import com.example.ruvirtual.ui.theme.RUVirtualTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RUVirtualTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(navController)
        }
        // A rota "card" agora é simples, sem argumentos de navegação
        composable("card") { // <-- Rota simplificada
            CardScreen(
                navController = navController
            )
        }
    }
}