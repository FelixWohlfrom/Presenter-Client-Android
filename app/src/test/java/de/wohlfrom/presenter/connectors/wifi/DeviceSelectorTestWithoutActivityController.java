/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *  Presenter. Android Client to remote control a presentation.          *
 *  Copyright (C) 2019 Felix Wohlfrom                                    *
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

package de.wohlfrom.presenter.connectors.wifi;

import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import androidx.fragment.app.testing.FragmentScenario;

/**
 * These tests ensure that the device selector fragment works properly.
 * In contrary to the tests in {@link DeviceSelectorTest} these tests don't require
 * an activity controller
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {
            Build.VERSION_CODES.LOLLIPOP_MR1,
            Build.VERSION_CODES.M
        })
public class DeviceSelectorTestWithoutActivityController {
    /**
     * Verifies that correct exception is thrown if the inflating context does not implement
     * {@link DeviceSelector.DeviceListResultListener}
     */
    @Test(expected = ClassCastException.class)
    public void instantiateNoDeviceListResultListener() {
        FragmentScenario.launchInContainer(DeviceSelector.class);
    }
}
