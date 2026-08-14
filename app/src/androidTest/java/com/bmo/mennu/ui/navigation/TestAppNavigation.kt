package com.bmo.mennu.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

/**
 * Exercises the real navigation contract — [Screen], [bottomNavItems], [MennuBottomBar] — behind
 * lightweight fake destinations standing in for LoginScreen/HomeScreen/CardapioScreen/CardScreen/
 * QrCodeScreen. The real screens need Hilt view models and live network calls (MainActivity.kt's
 * AppNavigation) that this project has no instrumented test harness for (no hilt-android-testing
 * dependency, no custom Hilt test runner) — wiring those up is out of scope here.
 *
 * The Scaffold/NavHost structure and bottom-bar visibility rule below mirror MainActivity.kt:
 * 45-63 line for line. Keep the two in sync if that logic changes.
 */
object BottomNavTestState {
    var lastNavController: NavHostController? = null
}

@Composable
fun TestAppNavigation() {
    val navController = rememberNavController()
    SideEffect { BottomNavTestState.lastNavController = navController }
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
            composable(Screen.Login.route) {
                Button(onClick = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }) { Text("Entrar") }
            }
            composable(Screen.Home.route) {
                var counter by rememberSaveable { mutableIntStateOf(0) }
                Column {
                    Text("Home Screen")
                    Text("Counter: $counter")
                    Button(onClick = { counter++ }) { Text("Incrementar") }
                    Button(onClick = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }) { Text("Sair") }
                }
            }
            composable(Screen.Cardapio.route) { Text("Cardapio Screen") }
            composable(Screen.Cartao.route) {
                Column {
                    Text("Card Screen")
                    Button(onClick = {
                        navController.navigate(Screen.Login.route) {
                            popUpTo(navController.graph.id) { inclusive = true }
                        }
                    }) { Text("Sair Cartao") }
                }
            }
            composable(Screen.QrCode.route) { Text("QrCode Screen") }
        }
    }
}
