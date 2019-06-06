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

package de.wohlfrom.presenter.connectors.bluetooth;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;

import de.wohlfrom.presenter.R;

/**
 * This activity can be used to test the fragments used for bluetooth connection.
 * It implements the required listeners and provides getters for the relevant values set by these 
 * listeners.
 * Use the following snippet to run an activity with a given fragment:
 * <code>
 *     ActivityController activityController = Robolectric.buildActivity(DummyActivity.class);
 *     ((DummyActivity) activityController.get()).setFragment([yourFragment]);
 *     activityController.create().resume();
 * </code>
 */
public class DummyActivity extends Activity 
        implements DeviceSelector.DeviceListResultListener {
    
    private String selectedDevice = null;
    private Fragment fragment;

    /**
     * Sets the fragment to display to the given fragment.
     * 
     * @param fragment The fragment to display on creating the activity.
     */
    public void setFragment(Fragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connector);
    }

    @Override
    public void onResume() {
        super.onResume();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.connector_content, fragment);
        transaction.commit();
    }

    @Override
    public void onDeviceSelected(String address) {
        selectedDevice = address;
    }

    /**
     * Returns the device set by {@link DeviceSelector.DeviceListResultListener}
     * 
     * @return The selected device
     */
    public String getSelectedDevice() {
        return selectedDevice;
    }
}
