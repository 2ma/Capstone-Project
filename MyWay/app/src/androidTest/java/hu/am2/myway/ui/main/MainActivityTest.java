package hu.am2.myway.ui.main;

import android.Manifest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import hu.am2.myway.R;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    @Rule
    public final ActivityTestRule activityTestRule = new ActivityTestRule<>(MainActivity.class);

    @Rule
    public GrantPermissionRule mRuntimePermissionRule
        = GrantPermissionRule.grant(Manifest.permission.ACCESS_FINE_LOCATION);

    @Test
    public void newTrackButtonTest() {
        onView(withId(R.id.newWayBtn)).perform(click());
        onView(withId(R.id.map)).check(matches(isDisplayed()));
    }

    @Test
    public void historyButtonTest() {
        onView(withId(R.id.historyBtn)).perform(click());
        onView(withId(R.id.emptyView)).check(matches(isDisplayed()));
    }

    @Test
    public void settingsButtonTest() {
        onView(withId(R.id.settingsBtn)).perform(click());
        onView(withId(R.id.settings_preference_fragment)).check(matches(isDisplayed()));
    }
}
