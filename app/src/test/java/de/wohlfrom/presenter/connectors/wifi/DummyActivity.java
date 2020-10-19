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

import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentTransaction;
import de.wohlfrom.presenter.R;

/**
 * This activity can be used to test the fragments used for wifi connection.
 * It implements the required listeners and provides getters for the relevant values set by these 
 * listeners.
 * Use the following snippet to run an activity with a given fragment:
 * <code>
 *     ActivityController activityController = Robolectric.buildActivity(DummyActivity.class);
 *     ((DummyActivity) activityController.get()).setFragment([yourFragment]);
 *     activityController.create().resume();
 * </code>
 */
public class DummyActivity extends FragmentActivity
        implements DeviceSelector.DeviceListResultListener {
    
    private String hostname = null;
    private String address = null;
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
        setContentView(R.layout.activity_wifi_connector);
    }

    @Override
    public void onResume() {
        super.onResume();
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.connector_content, fragment);
        transaction.commit();
    }

    @Override
    public void onDeviceSelected(String hostname, String address) {
        this.hostname = hostname;
        this.address = address;
    }

    /**
     * Returns the hostname set by {@link DeviceSelector.DeviceListResultListener}
     *
     * @return The selected hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * Returns the address set by {@link DeviceSelector.DeviceListResultListener}
     * 
     * @return The selected address
     */
    public String getAddress() {
        return address;
    }
}
