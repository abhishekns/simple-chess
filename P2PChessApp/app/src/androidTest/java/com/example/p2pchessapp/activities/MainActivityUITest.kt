package com.example.p2pchessapp.activities

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
class MainActivityUITest {

    // Rule to launch MainActivity before each test
    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun mainActivity_DisplaysCorrectly() {
        // Check if the title TextView is displayed
        onView(withId(R.id.textViewTitle))
            .check(matches(isDisplayed()))
            .check(matches(withText("P2P Chess"))) // Assuming this is the title text

        // Check if "Host Game" button is displayed
        onView(withId(R.id.buttonHostGame))
            .check(matches(isDisplayed()))
            .check(matches(withText("Host Game")))

        // Check if "Join Game" button is displayed
        onView(withId(R.id.buttonJoinGame))
            .check(matches(isDisplayed()))
            .check(matches(withText("Join Game")))

        // Check if "Share Invite" button is displayed
        onView(withId(R.id.buttonShareInvite))
            .check(matches(isDisplayed()))
            .check(matches(withText("Share Invite")))

        // Check if the status TextView is displayed
        onView(withId(R.id.textViewStatus))
            .check(matches(isDisplayed()))
            // Initial status could vary, so just check if displayed or check for a default known text
            // For example, if it starts with "Status: Idle" or "Welcome!"
            // .check(matches(withText("Status: Idle"))) // This might change based on P2P state
    }

    // Add more tests here for button clicks if they don't immediately require P2P interaction
    // or complex setup. For example, testing that clicking Share Invite attempts to launch an Intent
    // would require Espresso-Intents.

    // For P2P dependent actions (Host/Join), UI tests become more complex and might require
    // mocking the P2P layer or using multiple emulators, which is beyond basic UI tests.
}
