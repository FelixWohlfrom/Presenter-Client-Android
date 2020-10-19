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

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.os.Looper;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBluetoothAdapter;
import org.robolectric.shadows.ShadowBluetoothDevice;

import java.util.HashSet;
import java.util.Objects;

import androidx.fragment.app.testing.FragmentScenario;
import androidx.test.core.app.ApplicationProvider;
import de.wohlfrom.presenter.R;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * These tests ensure that the device selector fragment works properly.
 */
@RunWith(RobolectricTestRunner.class)
@Config(shadows = {ShadowBluetoothAdapter.class},
        sdk = {
            Build.VERSION_CODES.LOLLIPOP_MR1,
            Build.VERSION_CODES.M
        })
public class DeviceSelectorTest {

    private final String BLUETOOTH_DEVICE_ID = "AA:BB:CC:DD:EE:FF";

    private DeviceSelector deviceSelector;
    private ActivityController<DummyActivity> activityController;

    /**
     * Initializes the {@link DeviceSelector} fragment.
     */
    @Before
    public void initDeviceSelector() {
        deviceSelector = new DeviceSelector();
        activityController = Robolectric.buildActivity(DummyActivity.class);
        activityController.get().setFragment(deviceSelector);
    }

    /**
     * Helper method to create a new device and add it to bonded device list.
     */
    private void initBondedDevice() {
        HashSet<BluetoothDevice> devices = new HashSet<>();
        devices.add(ShadowBluetoothDevice.newInstance(BLUETOOTH_DEVICE_ID));
        shadowOf(BluetoothAdapter.getDefaultAdapter()).setBondedDevices(devices);
    }

    /**
     * Verifies that correct exception is thrown if the inflating context does not implement
     * {@link DeviceSelector.DeviceListResultListener}
     */
    @Test(expected = ClassCastException.class)
    public void instantiateNoDeviceListResultListener() {
        FragmentScenario.launch(DeviceSelector.class);
    }

    /**
     * Verifies that the fragment can be displayed if no paired devices are available.
     */
    @Test
    public void instantiateFragmentNoDevices() {
        activityController.create().resume();

        View view = deviceSelector.getView();
        assertThat("Could not find paired devices",
                Objects.requireNonNull(view).findViewById(R.id.paired_devices), is(notNullValue()));
        assertThat("Found not exactly one entry",
                ((ListView) view.findViewById(R.id.paired_devices)).getAdapter().getCount(), is(1));
        assertThat("Unexpected entry found",
                ((ListView) view.findViewById(R.id.paired_devices)).getAdapter().getItem(0)
                        .toString(),
                is(view.getResources().getText(R.string.none_paired).toString()));
    }

    /**
     * Verifies that the fragment can be displayed if a device was previously paired.
     */
    @Test
    public void instantiateFragmentWithDevice() {
        initBondedDevice();

        activityController.create().resume();

        View view = deviceSelector.getView();
        String displayedDeviceId =
                ((ListView) Objects.requireNonNull(view).findViewById(R.id.paired_devices))
                        .getAdapter().getItem(0).toString();
        displayedDeviceId = displayedDeviceId.substring(displayedDeviceId.length() - 17);
        assertThat("Could not find paired devices",
                view.findViewById(R.id.paired_devices), is(notNullValue()));
        assertThat("Found not exactly one device",
                ((ListView) view.findViewById(R.id.paired_devices)).getAdapter().getCount(), is(1));
        assertThat("Unexpected entry found", displayedDeviceId, is(BLUETOOTH_DEVICE_ID));
    }

    /**
     * Verifies that the fragment can be destroyed. No devices paired before.
     */
    @Test
    public void verifyDestroyingNoDevice() {
        activityController.create().resume().destroy();
    }

    /**
     * Verifies that the fragment can be destroyed. One device paired before.
     */
    @Test
    public void verifyDestroyingOneDevice() {
        initBondedDevice();
        BluetoothAdapter.getDefaultAdapter().startDiscovery();
        activityController.create().resume().destroy();

        assertThat("Discovery not stopped",
                BluetoothAdapter.getDefaultAdapter().isDiscovering(), is(false));
    }

    /**
     * Verifies that searching for new devices can be triggered.
     * Discovery already started.
     */
    @Test
    public void clickOnSearchDiscoveryRunning() {
        BluetoothAdapter.getDefaultAdapter().startDiscovery();
        activityController.create().resume();

        View view = deviceSelector.getView();
        Objects.requireNonNull(view).findViewById(R.id.button_scan).performClick();

        assertThat("Did not start device discovery",
                BluetoothAdapter.getDefaultAdapter().isDiscovering(), is(true));
    }

    /**
     * Verifies that searching for new devices can be triggered.
     * Discovery not started.
     */
    @Test
    public void clickOnSearchDiscoveryStopped() {
        activityController.create().resume();

        View view = deviceSelector.getView();
        Objects.requireNonNull(view).findViewById(R.id.button_scan).performClick();

        assertThat("Did not start device discovery",
                BluetoothAdapter.getDefaultAdapter().isDiscovering(), is(true));
    }

    /**
     * Ensures that clicking on a device sends the correct value to
     * {@link DeviceSelector.DeviceListResultListener}
     */
    @Test
    public void clickOnPairedDevice() {
        initBondedDevice();

        activityController.create().resume();

        View view = deviceSelector.getView();

        ListView listView = Objects.requireNonNull(view).findViewById(R.id.paired_devices);
        ListAdapter adapter = listView.getAdapter();
        View itemView = adapter.getView(0, null, listView);
        listView.performItemClick(itemView, 0, adapter.getItemId(0));

        assertThat("Received wrong value on device listener",
                activityController.get().getSelectedDevice(), is(BLUETOOTH_DEVICE_ID));
    }

    /**
     * Check that adding a new device during bluetooth discovery works fine.
     * Device list is empty before.
     */
    @Test
    public void findNewDeviceEmptyList() {
        activityController.create().resume().visible();

        // First send out the intent for the new device
        Intent intent = new Intent(BluetoothDevice.ACTION_FOUND);
        intent.putExtra(BluetoothDevice.EXTRA_DEVICE,
                BluetoothAdapter.getDefaultAdapter().getRemoteDevice(BLUETOOTH_DEVICE_ID));
        ApplicationProvider.getApplicationContext().sendBroadcast(intent);
        shadowOf(Looper.getMainLooper()).idle();

        View view = deviceSelector.getView();
        assertThat("Did not discover new device",
                ((ListView) Objects.requireNonNull(view).findViewById(R.id.new_devices))
                        .getAdapter().getCount(), is(1));
    }

    /**
     * Check that finishing discovery without finding any devices works fine.
     */
    @Test
    public void finishDiscoveryNoDevices() {
        activityController.create().resume().visible();

        // Directly send out intent for discovery finished
        Intent intent = new Intent(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        ApplicationProvider.getApplicationContext().sendBroadcast(intent);
        shadowOf(Looper.getMainLooper()).idle();

        View view = deviceSelector.getView();

        assertThat("Found device although none was expected",
                ((ListView) Objects.requireNonNull(view).findViewById(R.id.new_devices))
                        .getAdapter().getItem(0).toString(),
                is(view.getResources().getText(R.string.none_found)));
    }
}
