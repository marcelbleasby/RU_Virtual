package com.bmo.mennu.ui.navigation

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.espresso.Espresso
import androidx.test.espresso.NoActivityResumedException
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private val TAB_LABELS = listOf("Início", "Cardápio", "Cartão", "QR Code")

@RunWith(AndroidJUnit4::class)
class BottomNavigationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<BottomNavTestActivity>()

    @Before
    fun resetCapturedNavController() {
        BottomNavTestState.lastNavController = null
    }

    private fun login() {
        composeTestRule.onNodeWithText("Entrar").performClick()
    }

    // Scenario 1 — cold start on Login, bar hidden
    @Test
    fun coldStart_showsLogin_bottomBarHidden() {
        composeTestRule.onNodeWithText("Entrar").assertExists()
        TAB_LABELS.forEach { composeTestRule.onNodeWithText(it).assertDoesNotExist() }
    }

    // Scenario 2 — login pops Login off the stack, lands on Home with all 4 tabs visible
    @Test
    fun login_navigatesToHome_showsBottomBarWithFourTabs() {
        login()
        composeTestRule.onNodeWithText("Home Screen").assertExists()
        TAB_LABELS.forEach { composeTestRule.onNodeWithText(it).assertExists() }
        composeTestRule.onNodeWithText("Entrar").assertDoesNotExist()
    }

    // Scenario 3 — switching tabs swaps content, bar stays visible throughout
    @Test
    fun switchingTabs_updatesContent_bottomBarStaysVisible() {
        login()

        composeTestRule.onNodeWithText("Cardápio").performClick()
        composeTestRule.onNodeWithText("Cardapio Screen").assertExists()

        composeTestRule.onNodeWithText("Cartão").performClick()
        composeTestRule.onNodeWithText("Card Screen").assertExists()

        composeTestRule.onNodeWithText("QR Code").performClick()
        composeTestRule.onNodeWithText("QrCode Screen").assertExists()

        TAB_LABELS.forEach { composeTestRule.onNodeWithText(it).assertExists() }
    }

    // Scenario 3 (state half) — leaving and returning to a tab restores its state via
    // MennuBottomBar's saveState/restoreState (MennuBottomBar.kt:20-22), not a fresh instance
    @Test
    fun switchingAwayAndBack_restoresHomeState() {
        login()

        composeTestRule.onNodeWithText("Incrementar").performClick()
        composeTestRule.onNodeWithText("Incrementar").performClick()
        composeTestRule.onNodeWithText("Counter: 2").assertExists()

        composeTestRule.onNodeWithText("Cardápio").performClick()
        composeTestRule.onNodeWithText("Início").performClick()

        composeTestRule.onNodeWithText("Counter: 2").assertExists()
    }

    // Scenario 4 — re-tapping the already-selected tab is launchSingleTop: no new back-stack entry
    @Test
    fun reclickingActiveTab_doesNotDuplicateBackStackEntry() {
        login()
        composeTestRule.waitForIdle()
        val navController = requireNotNull(BottomNavTestState.lastNavController)

        val entryCountBefore = navController.currentBackStack.value.size
        composeTestRule.onNodeWithText("Início").performClick()
        composeTestRule.waitForIdle()
        val entryCountAfter = navController.currentBackStack.value.size

        assertEquals(entryCountBefore, entryCountAfter)
        composeTestRule.onNodeWithText("Home Screen").assertExists()
    }

    // Scenario 5 — logout from Home clears the whole graph (popUpTo(graph.id), HomeScreen.kt:69)
    @Test
    fun logoutFromHome_returnsToLogin_bottomBarHidden() {
        login()
        composeTestRule.onNodeWithText("Sair").performClick()

        composeTestRule.onNodeWithText("Entrar").assertExists()
        TAB_LABELS.forEach { composeTestRule.onNodeWithText(it).assertDoesNotExist() }
    }

    // Scenario 6 — same logout contract, second call site (CardScreen.kt:230/301)
    @Test
    fun logoutFromCard_returnsToLogin_bottomBarHidden() {
        login()
        composeTestRule.onNodeWithText("Cartão").performClick()
        composeTestRule.onNodeWithText("Sair Cartao").performClick()

        composeTestRule.onNodeWithText("Entrar").assertExists()
        TAB_LABELS.forEach { composeTestRule.onNodeWithText(it).assertDoesNotExist() }
    }

    // Scenario 7 — visibility rule is an allowlist (MainActivity.kt:47): a route absent from
    // bottomNavItems must hide the bar even mid-graph. All 5 real Screen routes except Login are
    // in bottomNavItems today, so there's no such route to click through — this pins the
    // invariant directly against the production predicate instead.
    @Test
    fun visibilityRule_hidesBarForAnyRouteNotInBottomNavItems() {
        val allScreens = listOf(Screen.Login, Screen.Home, Screen.Cardapio, Screen.Cartao, Screen.QrCode)
        assertTrue(bottomNavItems.none { it.screen.route == Screen.Login.route })
        allScreens.forEach { screen ->
            val shouldShow = bottomNavItems.any { it.screen.route == screen.route }
            assertEquals(screen.route != Screen.Login.route, shouldShow)
        }
    }

    // Scenario 9 — system back press from Home (Login was popped inclusive on login, so there's
    // nothing left below Home on the stack). Confirmed on-device: Espresso.pressBack() throws
    // NoActivityResumedException here — the Activity finishes rather than resurrecting Login.
    @Test
    fun systemBackFromHome_exitsBottomNavGraph() {
        login()

        var caught: NoActivityResumedException? = null
        try {
            Espresso.pressBack()
        } catch (e: NoActivityResumedException) {
            caught = e
        }

        assertNotNull(
            "Expected back press from Home to finish the Activity (nothing left below Home " +
                "on the stack once Login is popped inclusive on login, LoginScreen.kt:49)",
            caught
        )
    }

    // Scenario 10 — Activity recreation (rotation/config change) keeps the selected tab and its
    // NavHost-managed state, since NavController survives via its own SavedStateHandle
    @Test
    fun configurationChange_restoresSelectedTab() {
        login()
        composeTestRule.onNodeWithText("Cardápio").performClick()

        composeTestRule.activityRule.scenario.recreate()

        composeTestRule.onNodeWithText("Cardapio Screen").assertExists()
        TAB_LABELS.forEach { composeTestRule.onNodeWithText(it).assertExists() }
    }
}
