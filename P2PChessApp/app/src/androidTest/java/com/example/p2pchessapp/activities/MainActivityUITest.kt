package com.example.p2pchessapp.activities

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import com.example.p2pchessapp.R
import com.example.p2pchessapp.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class MainActivityUITest {

    // Rule to launch MainActivity before each test
    @get:Rule
    var activityRule: ActivityScenarioRule<MainActivity> =
        ActivityScenarioRule(MainActivity::class.java)

    // Check if the expected buttons are displayed correctly in MainActivity
    @Test
    fun mainActivity_DisplaysCorrectly() {
        val scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.moveToState(Lifecycle.State.RESUMED)
        onView(withId(R.id.textViewTitle)).check(matches(isDisplayed()))
        onView(withId(R.id.buttonHostGame)).check(matches(isDisplayed()))
        onView(withId(R.id.buttonJoinGame)).check(matches(isDisplayed()))
        onView(withId(R.id.buttonShareInvite)).check(matches(isDisplayed()))
        onView(withId(R.id.textViewStatus)).check(matches(isDisplayed()))
    }

    // Add more tests here for button clicks if they don't immediately require P2P interaction
    // or complex setup. For example, testing that clicking Share Invite attempts to launch an Intent
    // would require Espresso-Intents.

    // For P2P dependent actions (Host/Join), UI tests become more complex and might require
    // mocking the P2P layer or using multiple emulators, which is beyond basic UI tests.
}
