/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *  Presenter. Android Client to remote control a presentation.          *
 *  Copyright (C) 2017 Felix Wohlfrom                                    *
 *                                                                       *
 *  This program is free software: you can redistribute it and/or modify *
 *  it under the terms of the GNU General Public License as published by *
 *  the Free Software Foundation, either version 3 of the License, or    *
 *  (at your option) any later version.                                  *
 *                                                                       *
 *  This program is distributed in the hope that it will be useful,      *
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of       *
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the        *
 *  GNU General Public License for more details.                         *
 *                                                                       *
 *  You should have received a copy of the GNU General Public License    *
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.*
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

package de.wohlfrom.presenter;

import android.os.Build;
import android.widget.Toolbar;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SmallTest;
import androidx.test.platform.app.InstrumentationRegistry;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withResourceName;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.is;

/**
 * Basic verification for main activity interactions.
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityTest {

    /**
     * Test that the main activity can be started at all.
     */
    @Test
    @SmallTest
    public void instantiateActivity() {
        ActivityScenario.launch(MainActivity.class);
    }

    /**
     * Verify that the settings activity can be started from main menu.
     */
    @Test
    @MediumTest
    public void startSettingsActivity() {
        ActivityScenario.launch(MainActivity.class);

        CharSequence settingsTitle =
                InstrumentationRegistry.getInstrumentation()
                        .getTargetContext().getString(R.string.settings);

        onView(withId(R.id.settings)).perform(click());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            onView(isAssignableFrom(Toolbar.class))
                    .check(matches(withToolbarTitle(is(settingsTitle))));
        } else {
            onView(withResourceName("action_bar_title"))
                    .check(matches(withText(settingsTitle.toString())));
        }
    }

    /**
     * Verify that the about activity can be started from main menu.
     */
    @Test
    @MediumTest
    public void startAboutActivity() {
        ActivityScenario.launch(MainActivity.class);

        CharSequence aboutTitle =
                InstrumentationRegistry.getInstrumentation()
                        .getTargetContext().getString(R.string.about,
                            InstrumentationRegistry.getInstrumentation()
                                    .getTargetContext().getString(R.string.app_name));

        onView(withId(R.id.about)).perform(click());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            onView(isAssignableFrom(Toolbar.class))
                    .check(matches(withToolbarTitle(is(aboutTitle))));
        } else {
            onView(withResourceName("action_bar_title"))
                    .check(matches(withText(aboutTitle.toString())));
        }
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
