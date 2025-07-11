package com.example.p2pchessapp.activities

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.p2pchessapp.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GameActivityUITest {

    // Custom rule to launch GameActivity with necessary intent extras
    @get:Rule
    var activityRule: ActivityScenarioRule<GameActivity> =
        ActivityScenarioRule(
            Intent(
                ApplicationProvider.getApplicationContext(),
                GameActivity::class.java
            ).apply {
                putExtra(GameActivity.EXTRA_IS_HOST, true) // Example: launching as host (White)
                putExtra(GameActivity.EXTRA_OPPONENT_NAME, "TestOpponent")
            }
        )

    @Test
    fun gameActivity_DisplaysCorrectly() {
        // Check if the turn info TextView is displayed
        onView(withId(R.id.textViewTurnInfo))
            .check(matches(isDisplayed()))
        // Text will depend on who's turn it is initially, which is White (Host)
        // .check(matches(withText("Your Turn (WHITE)"))) // or similar based on playerColor

        // Check if the game state TextView is displayed
        onView(withId(R.id.textViewGameState))
            .check(matches(isDisplayed()))
            .check(matches(withText("State: ONGOING"))) // Initial game state

        // Check if the chessboard GridLayout is displayed
        onView(withId(R.id.gridLayoutChessboard))
            .check(matches(isDisplayed()))

        // Check if the Resign button is displayed
        onView(withId(R.id.buttonResign))
            .check(matches(isDisplayed()))
            .check(matches(withText("Resign")))
    }

    // More complex UI tests for GameActivity would involve:
    // - Verifying pieces are drawn (custom ViewMatchers for ImageView sources).
    // - Simulating clicks on squares and asserting piece movement or selection highlights.
    // - Checking for dialogs (like pawn promotion or game over).
    // These require more intricate Espresso usage and potentially custom matchers/actions.
}
