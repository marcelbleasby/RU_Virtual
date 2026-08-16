package com.bmo.mennu.ui.navigation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

/**
 * Test-only host Activity for [BottomNavigationTest]. A plain ComponentActivity that calls
 * setContent from its own onCreate (rather than the test calling composeTestRule.setContent
 * directly) so that Activity recreation — rotation, config change — redraws the nav graph the
 * same way MainActivity does in production.
 */
class BottomNavTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { TestAppNavigation() }
    }
}
