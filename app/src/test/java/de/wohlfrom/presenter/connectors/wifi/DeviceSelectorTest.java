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
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.Objects;

import de.wohlfrom.presenter.R;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * These tests ensure that the device selector fragment works properly.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {
            Build.VERSION_CODES.LOLLIPOP_MR1,
            Build.VERSION_CODES.M
        })
public class DeviceSelectorTest {
    private DeviceSelector deviceSelector;
    private ActivityController<DummyActivity> activityController;
    private BroadcastServer broadcastServer;

    /**
     * Initializes the {@link DeviceSelector} fragment and the broadcast server.
     * Starting and stopping of server still needs to be done in testcase.
     */
    @Before
    public void initTestcase() {
        deviceSelector = new DeviceSelector();
        activityController = Robolectric.buildActivity(DummyActivity.class);
        activityController.get().setFragment(deviceSelector);
        
        broadcastServer = new BroadcastServer();
    }

    /**
     * Cleans up the testcase. Will clean up the activity and stop any eventually running broadcast 
     * server.
     */
    @After
    public void cleanupTestcase() {
        if (!activityController.get().isDestroyed()) {
            activityController.destroy();
        }
        broadcastServer.stop();
    }

    /**
     * Verifies that the fragment can be displayed if no broadcasted devices are available.
     */
    @Test
    public void instantiateFragmentNoDevices() {
        activityController.create().resume();

        View view = deviceSelector.getView();
        assertThat("Could not find broadcasted devices view",
                Objects.requireNonNull(view).findViewById(R.id.broadcast_devices),
                is(notNullValue()));
        assertThat("Found unexpected entry",
                ((ListView) view.findViewById(R.id.broadcast_devices)).getAdapter().getCount(),
                is(0));
    }

    /**
     * Verifies that the fragment can be displayed if a server is broadcasting.
     * 
     * @throws InterruptedException On test failures
     */
    @Test
    public void instantiateFragmentWithDevice() throws InterruptedException {        
        activityController.create().resume().visible();

        // Start broadcasting of new devices
        broadcastServer.start();
        shadowOf(Looper.getMainLooper()).idle();

        View view = deviceSelector.getView();
        assertThat("Could not find broadcasted devices",
                Objects.requireNonNull(view).findViewById(R.id.broadcast_devices),
                is(notNullValue()));
        assertThat("Found not exactly one device",
                ((ListView) view.findViewById(R.id.broadcast_devices))
                        .getAdapter().getCount(), is(1));
        String displayedDeviceId =
                ((ListView) view.findViewById(R.id.broadcast_devices))
                        .getAdapter().getItem(0).toString();
        assertThat("Unexpected entry found", displayedDeviceId, 
                is(BroadcastServer.WIFI_DEVICE_NAME + "\n" + BroadcastServer.WIFI_IP_ADDRESS));
    }

    /**
     * Verifies that a broadcasting device is successfully removed after broadcasting has stopped.
     * 
     * @throws InterruptedException If sleeping was interrupted. Should never appear.
     */
    @Test
    public void deviceRemovalAfterTimeout() throws InterruptedException {
        activityController.create().resume().visible();

        // Start broadcasting of new devices
        broadcastServer.start();
        shadowOf(Looper.getMainLooper()).idle();

        // Fist check that our device is found
        // Other checks like only one device found and others are already handled in
        // instantiateFragmentWithDevice test
        View view = deviceSelector.getView();
        String displayedDeviceId =
                ((ListView) Objects.requireNonNull(view).findViewById(R.id.broadcast_devices))
                        .getAdapter().getItem(0).toString();
        assertThat("Unexpected entry found", displayedDeviceId,
                is(BroadcastServer.WIFI_DEVICE_NAME + "\n" + BroadcastServer.WIFI_IP_ADDRESS));
        
        // Now stop broadcasting and give the device some time to disappear
        broadcastServer.stop();
        shadowOf(Looper.getMainLooper()).idle();
        
        // Sleep some longer time than the removal time, just to be sure it is properly removed.
        Thread.sleep(15000);
        shadowOf(Looper.getMainLooper()).idle();
        assertThat("Found a device, although it should have been removed.",
                ((ListView) view.findViewById(R.id.broadcast_devices))
                        .getAdapter().getCount(), is(0));
    }

    /**
     * Verifies that the fragment can be destroyed. No devices broadcasting.
     */
    @Test
    public void verifyDestroyingNoDevice() {
        activityController.create().resume().destroy();
    }

    /**
     * Verifies that the fragment can be destroyed. One device broadcasting.
     * 
     * @throws InterruptedException On test failures
     */
    @Test
    public void verifyDestroyingOneDevice() throws InterruptedException {
        // Start broadcasting of new devices
        new BroadcastServer().start();
        shadowOf(Looper.getMainLooper()).idle();
        
        activityController.create().resume().destroy();
    }

    /**
     * Ensures that clicking on a device sends the correct value to
     * {@link DeviceSelector.DeviceListResultListener}
     *
     * @throws InterruptedException On test failures
     */
    @Test
    public void clickOnPairedDevice() throws InterruptedException {        
        activityController.create().resume().visible();

        // Start broadcasting of new devices
        new BroadcastServer().start();
        shadowOf(Looper.getMainLooper()).idle();

        View view = deviceSelector.getView();

        ListView listView = Objects.requireNonNull(view).findViewById(R.id.broadcast_devices);
        ListAdapter adapter = listView.getAdapter();
        View itemView = adapter.getView(0, null, listView);
        listView.performItemClick(itemView, 0, adapter.getItemId(0));

        assertThat("Received wrong address on device listener",
                activityController.get().getAddress(), is(BroadcastServer.WIFI_IP_ADDRESS));
        assertThat("Received wrong hostname on device listener",
                activityController.get().getHostname(), is(BroadcastServer.WIFI_DEVICE_NAME));
    }

    /**
     * Ensures that clicking on manual connection displays the correct view.
     */
    @Test
    public void clickOnManualConnection() {
        activityController.create().resume().visible();

        Button manualConnection =
                Objects.requireNonNull(deviceSelector.getView())
                        .findViewById(R.id.button_manual_connection);
        
        assertThat("Did not click manual connection button", 
                manualConnection.performClick(), is(true));
        shadowOf(Looper.getMainLooper()).idle();
        
        assertThat("Did not show manual connection fragment",
                activityController.get().findViewById(R.id.ip_address_label), is(notNullValue()));
    }
}
