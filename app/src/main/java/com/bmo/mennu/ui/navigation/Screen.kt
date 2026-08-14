package com.bmo.mennu.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Home : Screen("home")
    object Cardapio : Screen("cardapio")
    object Cartao : Screen("card")
    object QrCode : Screen("qrcode")
}

data class BottomNavItem(val screen: Screen, val label: String, val icon: ImageVector)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home, "Início", Icons.Filled.Home),
    BottomNavItem(Screen.Cardapio, "Cardápio", Icons.Filled.Restaurant),
    BottomNavItem(Screen.Cartao, "Cartão", Icons.Filled.CreditCard),
    BottomNavItem(Screen.QrCode, "QR Code", Icons.Filled.QrCode2)
)

// Único jeito de navegar entre as abas da bottom bar (usado pela própria bottom bar
// e por qualquer atalho, ex: QuickActionCard da Home). Precisa ser o MESMO em todo
// call site — misturar isso com navigate() puro pra uma rota da bottom bar duplica
// entradas na back stack e trava a navegação de volta (ex: "Início" para de responder).
fun NavHostController.navigateToBottomNavDestination(route: String) {
    navigate(route) {
        // Anchored to Home (not graph.findStartDestination(), which is "login" —
        // already popped off the stack by the time the bottom bar is visible).
        popUpTo(Screen.Home.route) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
