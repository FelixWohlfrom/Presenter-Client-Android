package de.wohlfrom.presenter;

import android.support.test.InstrumentationRegistry;
import android.support.test.espresso.matcher.BoundedMatcher;
import android.support.test.filters.MediumTest;
import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.widget.Toolbar;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Basic verification for main activity interactions.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    /**
     * The rule to interact with the main activity.
     */
    @Rule
    public ActivityTestRule<MainActivity> mainActivityRule
            = new ActivityTestRule<>(MainActivity.class);

    /**
     * Test that the main activity can be started at all.
     */
    @Test
    @SmallTest
    public void instantiateActivity() {
        assertThat(mainActivityRule.getActivity(), is(notNullValue()));
    }

    /**
     * Verify that the settings activity can be started from main menu.
     */
    @Test
    @MediumTest
    public void startSettingsActivity() {
        CharSequence settingsTitle =
                InstrumentationRegistry.getTargetContext().getString(R.string.settings);

        onView(withId(R.id.settings)).perform(click());
        onView(isAssignableFrom(Toolbar.class))
                .check(matches(withToolbarTitle(is(settingsTitle))));
    }

    /**
     * Verify that the about activity can be started from main menu.
     */
    @Test
    @MediumTest
    public void startAboutActivity() {
        CharSequence aboutTitle =
                InstrumentationRegistry.getTargetContext().getString(R.string.about,
                        InstrumentationRegistry.getTargetContext().getString(R.string.app_name));

        onView(withId(R.id.about)).perform(click());
        onView(isAssignableFrom(Toolbar.class))
                .check(matches(withToolbarTitle(is(aboutTitle))));
    }

    /**
     * Helper class that matches a given toolbar title.
     *
     * @param textMatcher The matcher to verify the toolbar title against
     * @return The matcher that will verify the toolbar title
     */
    private static Matcher<Object> withToolbarTitle(final Matcher<CharSequence> textMatcher) {
        return new BoundedMatcher<Object, Toolbar>(Toolbar.class) {
            @Override
            public boolean matchesSafely(Toolbar toolbar) {
                return textMatcher.matches(toolbar.getTitle());
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("with toolbar title: ");
                textMatcher.describeTo(description);
            }
        };
    }
}
