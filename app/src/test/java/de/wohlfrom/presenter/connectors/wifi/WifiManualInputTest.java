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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import java.util.Objects;

import androidx.fragment.app.testing.FragmentScenario;
import de.wohlfrom.presenter.R;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * These tests ensure that the device selector fragment works properly.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {
            Build.VERSION_CODES.LOLLIPOP_MR1,
            Build.VERSION_CODES.M
        })
public class WifiManualInputTest {
    private WifiManualInput wifiManualInput;
    private ActivityController<DummyActivity> activityController;

    /**
     * Initializes the {@link WifiManualInput} fragment.
     */
    @Before
    public void initTestcase() {
        wifiManualInput = new WifiManualInput();
        activityController = Robolectric.buildActivity(DummyActivity.class);
        activityController.get().setFragment(wifiManualInput);
    }

    /**
     * Verifies that the fragment can be displayed properly.
     */
    @Test
    public void instantiateFragment() {
        activityController.create().resume();

        View view = wifiManualInput.getView();
        assertThat("Could not find manual input view",
                Objects.requireNonNull(view).findViewById(R.id.ip_address_label),
                is(notNullValue()));
    }

    /**
     * Verifies that correct exception is thrown if the inflating context does not implement
     * {@link DeviceSelector.DeviceListResultListener}
     */
    @Test(expected = ClassCastException.class)
    public void instantiateNoDeviceListResultListener() {
        FragmentScenario.launch(WifiManualInput.class);
    }

    /**
     * Ensures that clicking on connection sends the correct data to 
     * {@link DeviceSelector.DeviceListResultListener}.
     */
    @Test
    public void clickOnConnection() {
        activityController.create().resume().visible();
        
        TextView hostname =
                Objects.requireNonNull(wifiManualInput.getView()).findViewById(R.id.wifi_name);
        hostname.setText(BroadcastServer.WIFI_DEVICE_NAME);

        TextView ipaddress =
                Objects.requireNonNull(wifiManualInput.getView()).findViewById(R.id.ip_address);
        ipaddress.setText(BroadcastServer.WIFI_IP_ADDRESS);
        
        Button connect = 
                Objects.requireNonNull(wifiManualInput.getView()).findViewById(R.id.connect);
        
        assertThat("Did not click connect button", 
                connect.performClick(), is(true));

        assertThat("Received wrong address on device listener",
                ((DummyActivity)activityController.get()).getAddress(),
                is(BroadcastServer.WIFI_IP_ADDRESS));
        assertThat("Received wrong hostname on device listener",
                ((DummyActivity)activityController.get()).getHostname(),
                is(BroadcastServer.WIFI_DEVICE_NAME));
    }

    /**
     * Ensures that clicking on connection sends the correct data to 
     * {@link DeviceSelector.DeviceListResultListener}.
     * If no hostname is available, the ip address should still be sent.
     */
    @Test
    public void clickOnConnectionNoHostname() {
        activityController.create().resume().visible();

        TextView ipaddress =
                Objects.requireNonNull(wifiManualInput.getView()).findViewById(R.id.ip_address);
        ipaddress.setText(BroadcastServer.WIFI_IP_ADDRESS);

        Button connect =
                Objects.requireNonNull(wifiManualInput.getView()).findViewById(R.id.connect);

        assertThat("Did not click connect button",
                connect.performClick(), is(true));

        assertThat("Received wrong address on device listener",
                ((DummyActivity)activityController.get()).getAddress(),
                is(BroadcastServer.WIFI_IP_ADDRESS));
        assertThat("Received wrong hostname on device listener",
                ((DummyActivity)activityController.get()).getHostname(),
                is(BroadcastServer.WIFI_IP_ADDRESS));
    }

    /**
     * Ensures that clicking on connection sends the correct data to 
     * {@link DeviceSelector.DeviceListResultListener}.
     * If no ip is defined, no data should be sent.
     */
    @Test
    public void clickOnConnectionNoIP() {
        activityController.create().resume().visible();
        Button connect =
                Objects.requireNonNull(wifiManualInput.getView()).findViewById(R.id.connect);

        assertThat("Did not click connect button",
                connect.performClick(), is(true));

        assertThat("Received wrong address on device listener",
                ((DummyActivity)activityController.get()).getAddress(),
                is(nullValue()));
        assertThat("Received wrong hostname on device listener",
                ((DummyActivity)activityController.get()).getHostname(),
                is(nullValue()));
    }
}
