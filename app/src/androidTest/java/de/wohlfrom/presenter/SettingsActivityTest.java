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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.RequiresDevice;
import androidx.test.filters.SmallTest;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isChecked;
import static androidx.test.espresso.matcher.ViewMatchers.isNotChecked;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

/**
 * This class verifies that in the settings activity storing and restoring of all settings
 * works fine.
 */
@RunWith(AndroidJUnit4.class)
public class SettingsActivityTest {

    /**
     * Timeout for elements to react in android emulator.
     */
    private static final int RESPONSE_TIMEOUT = 2000;

    /**
     * Store the settings before tests and restore them after test execution.
     */
    private boolean silenceDuringPresentation;
    private boolean useVolumeKeysForNavigation;

    /**
     * Store all settings before the testcase and initialize them.
     */
    @Before
    public void storeSettings() {
        ActivityScenario<SettingsActivity> scenario =
                ActivityScenario.launch(SettingsActivity.class);
        scenario.onActivity(activity -> {
            Settings settings = new Settings(activity);
            silenceDuringPresentation = settings.silenceDuringPresentation();
            useVolumeKeysForNavigation = settings.useVolumeKeysForNavigation();

            settings.silenceDuringPresentation(false);
            settings.useVolumeKeysForNavigation(false);
        });
    }

    /**
     * Restore all settings after the testcase.
     */
    @After
    public void restoreSettings() {
        ActivityScenario<SettingsActivity> scenario =
                ActivityScenario.launch(SettingsActivity.class);
        scenario.onActivity(activity -> {
            Settings settings = new Settings(activity);
            settings.silenceDuringPresentation(silenceDuringPresentation);
            settings.useVolumeKeysForNavigation(useVolumeKeysForNavigation);
        });
    }

    /**
     * Test that the activity can be started at all.
     */
    @Test
    @SmallTest
    public void instantiateActivity() {
        ActivityScenario.launch(SettingsActivity.class);
    }

    /**
     * Verify that the "Silence during presentation" setting is correctly stored and restored
     * on activity loading.
     *
     * @throws InterruptedException If sleep times fail
     */
    @Test
    @LargeTest
    @RequiresDevice
    public void verifySilenceDuringPresentationStoring() throws InterruptedException {
        ActivityScenario<SettingsActivity> scenario =
                ActivityScenario.launch(SettingsActivity.class);

        onView(withId(R.id.silenceDuringPresentation))
                .perform(click());
        Thread.sleep(RESPONSE_TIMEOUT);
        onView(withId(R.id.silenceDuringPresentation))
                .check(matches(isChecked()));

        scenario.recreate();
        onView(withId(R.id.silenceDuringPresentation))
                .check(matches(isChecked()))
                .perform(click());
        Thread.sleep(RESPONSE_TIMEOUT);
        onView(withId(R.id.silenceDuringPresentation))
                .check(matches(isNotChecked()));

        scenario.recreate();
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
    @RequiresDevice
    public void verifyUseVolumeKeysForNavigationStoring() throws InterruptedException {
        ActivityScenario<SettingsActivity> scenario =
                ActivityScenario.launch(SettingsActivity.class);

        onView(withId(R.id.useVolumeKeysForNavigation))
                .perform(click());
        Thread.sleep(RESPONSE_TIMEOUT);
        onView(withId(R.id.useVolumeKeysForNavigation))
                .check(matches(isChecked()));

        scenario.recreate();
        onView(withId(R.id.useVolumeKeysForNavigation))
                .check(matches(isChecked()))
                .perform(click());
        Thread.sleep(RESPONSE_TIMEOUT);
        onView(withId(R.id.useVolumeKeysForNavigation))
                .check(matches(isNotChecked()));

        scenario.recreate();
        onView(withId(R.id.useVolumeKeysForNavigation)).check(matches(isNotChecked()));
    }
}
