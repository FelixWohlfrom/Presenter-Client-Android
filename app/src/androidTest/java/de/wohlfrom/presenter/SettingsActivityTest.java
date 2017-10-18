package de.wohlfrom.presenter;

import android.support.test.filters.LargeTest;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isChecked;
import static android.support.test.espresso.matcher.ViewMatchers.isNotChecked;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * This class verifies that in the settings activity storing and restoring of all settings
 * works fine.
 */
@RunWith(AndroidJUnit4.class)
public class SettingsActivityTest {

    /**
     * Store the settings before tests and restore them after test execution.
     */
    private boolean silenceDuringPresentation;
    private boolean useVolumeKeysForNavigation;

    /**
     * The rule to interact with the settings activity
     */
    @Rule
    public ActivityTestRule<SettingsActivity> settingsActivityRule
            = new ActivityTestRule<>(SettingsActivity.class);

    /**
     * Store all settings before the testcase and initialize them.
     */
    @Before
    public void storeSettings() {
        Settings settings = new Settings(settingsActivityRule.getActivity());
        silenceDuringPresentation = settings.silenceDuringPresentation();
        useVolumeKeysForNavigation = settings.useVolumeKeysForNavigation();

        settings.silenceDuringPresentation(false);
        settings.useVolumeKeysForNavigation(false);
    }

    /**
     * Restore all settings after the testcase.
     */
    @After
    public void restoreSettings() {
        Settings settings = new Settings(settingsActivityRule.getActivity());
        settings.silenceDuringPresentation(silenceDuringPresentation);
        settings.useVolumeKeysForNavigation(useVolumeKeysForNavigation);
    }

    /**
     * Test that the activity can be started at all.
     */
    @Test
    @SmallTest
    public void instantiateActivity() {
        assertThat(settingsActivityRule.getActivity(), is(notNullValue()));
    }

    /**
     * Verify that the "Silence during presentation" setting is correctly stored and restored
     * on activity loading.
     *
     * @throws InterruptedException If sleep times fail
     */
    @Test
    @LargeTest
    public void verifySilenceDuringPresentationStoring() throws InterruptedException {
        onView(withId(R.id.silenceDuringPresentation))
                .perform(click())
                .check(matches(isChecked()));

        settingsActivityRule.getActivity().onStop();
        settingsActivityRule.getActivity().finish();
        Thread.sleep(500);
        assertThat(settingsActivityRule.getActivity().isDestroyed(), is(true));

        settingsActivityRule.launchActivity(null);
        onView(withId(R.id.silenceDuringPresentation))
                .check(matches(isChecked()))
                .perform(click())
                .check(matches(isNotChecked()));

        settingsActivityRule.getActivity().onStop();
        settingsActivityRule.getActivity().finish();
        Thread.sleep(500);
        assertThat(settingsActivityRule.getActivity().isDestroyed(), is(true));

        settingsActivityRule.launchActivity(null);
        onView(withId(R.id.silenceDuringPresentation)).check(matches(isNotChecked()));
    }

    /**
     * Verify that the "Use volume keys for navigation" setting is correctly stored and restored
     * on activity loading.
     *
     * @throws InterruptedException If sleep times fail
     */
    @Test
    @LargeTest
    public void verifyUseVolumeKeysForNavigationStoring() throws InterruptedException {
        onView(withId(R.id.useVolumeKeysForNavigation))
                .perform(click())
                .check(matches(isChecked()));

        settingsActivityRule.getActivity().onStop();
        settingsActivityRule.getActivity().finish();
        Thread.sleep(500);
        assertThat(settingsActivityRule.getActivity().isDestroyed(), is(true));

        settingsActivityRule.launchActivity(null);
        onView(withId(R.id.useVolumeKeysForNavigation))
                .check(matches(isChecked()))
                .perform(click())
                .check(matches(isNotChecked()));

        settingsActivityRule.getActivity().onStop();
        settingsActivityRule.getActivity().finish();
        Thread.sleep(500);
        assertThat(settingsActivityRule.getActivity().isDestroyed(), is(true));

        settingsActivityRule.launchActivity(null);
        onView(withId(R.id.useVolumeKeysForNavigation)).check(matches(isNotChecked()));
    }
}
