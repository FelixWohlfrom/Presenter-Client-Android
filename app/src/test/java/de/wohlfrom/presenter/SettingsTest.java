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

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * A simple test that verifies that our settings class can properly store and restore the settings.
 */
@RunWith(RobolectricTestRunner.class)
public class SettingsTest {
    private Settings mSettings;

    @Before
    public void setUp() {
        mSettings = new Settings(Robolectric.setupActivity(DummyActivity.class));
    }

    /**
     * Verify that "silence device during presentation" setting is stored and restored properly.
     */
    @Test
    public void verifySilenceDuringPresentationStoring() {
        mSettings.silenceDuringPresentation(true);
        assertThat(mSettings.silenceDuringPresentation(), is(true));
        mSettings.silenceDuringPresentation(false);
        assertThat(mSettings.silenceDuringPresentation(), is(false));
    }

    /**
     * Verify that "use volume keys for navigation" setting is stored and restored properly.
     */
    @Test
    public void verifyUseVolumeKeysForNavigationStoring() {
        mSettings.useVolumeKeysForNavigation(true);
        assertThat(mSettings.useVolumeKeysForNavigation(), is(true));
        mSettings.useVolumeKeysForNavigation(false);
        assertThat(mSettings.useVolumeKeysForNavigation(), is(false));
    }
}
