package de.wohlfrom.presenter.connectors.bluetooth;

import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.view.View;
import android.widget.ListAdapter;
import android.widget.ListView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBluetoothAdapter;

import java.util.HashSet;

import de.wohlfrom.presenter.R;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.robolectric.Shadows.shadowOf;
import static org.robolectric.util.FragmentTestUtil.startFragment;

/**
 * These tests ensure that the device selector fragment works properly.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml",
        shadows = {ShadowBluetoothAdapter.class},
        sdk = {
            Build.VERSION_CODES.LOLLIPOP_MR1,
            Build.VERSION_CODES.M
        })
public class DeviceSelectorTest {

    private String BLUETOOTH_DEVICE_ID = "AA:BB:CC:DD:EE:FF";

    private DeviceSelector deviceSelector;
    private ActivityController activityController;

    /**
     * Initializes the {@link DeviceSelector} fragment.
     */
    @Before
    public void initDeviceSelector() {
        deviceSelector = new DeviceSelector();
        activityController = Robolectric.buildActivity(DummyActivity.class);
        ((DummyActivity) activityController.get()).setFragment(deviceSelector);
    }

    /**
     * Helper method to create a new device and add it to bonded device list.
     */
    private void initBondedDevice() {
        HashSet<BluetoothDevice> devices = new HashSet<>();
        devices.add(ShadowBluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(BLUETOOTH_DEVICE_ID));
        shadowOf(BluetoothAdapter.getDefaultAdapter()).setBondedDevices(devices);
    }

    /**
     * Verifies that correct exception is thrown if the inflating context does not implement
     * {@link DeviceSelector.DeviceListResultListener}
     */
    @Test(expected = ClassCastException.class)
    public void instantiateNoDeviceListResultListener() {
        Fragment fragment = new DeviceSelector();
        startFragment(fragment);
    }

    /**
     * Verifies that the fragment can be displayed if no paired devices are available.
     */
    @Test
    public void instantiateFragmentNoDevices() {
        activityController.create().resume();

        View view = deviceSelector.getView();
        assertThat("Could not find paired devices",
                view.findViewById(R.id.paired_devices), is(notNullValue()));
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
        String displayedDeviceId = ((ListView) view.findViewById(R.id.paired_devices)).getAdapter()
                .getItem(0).toString();
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
        ShadowBluetoothAdapter.getDefaultAdapter().startDiscovery();
        activityController.create().resume().destroy();

        assertThat("Discovery not stopped",
                ShadowBluetoothAdapter.getDefaultAdapter().isDiscovering(), is(false));
    }

    /**
     * Verifies that searching for new devices can be triggered.
     * Discovery already started.
     */
    @Test
    public void clickOnSearchDiscoveryRunning() {
        ShadowBluetoothAdapter.getDefaultAdapter().startDiscovery();
        activityController.create().resume();

        View view = deviceSelector.getView();
        view.findViewById(R.id.button_scan).performClick();

        assertThat("Did not start device discovery",
                ShadowBluetoothAdapter.getDefaultAdapter().isDiscovering(), is(true));
    }

    /**
     * Verifies that searching for new devices can be triggered.
     * Discovery not started.
     */
    @Test
    public void clickOnSearchDiscoveryStopped() {
        activityController.create().resume();

        View view = deviceSelector.getView();
        view.findViewById(R.id.button_scan).performClick();

        assertThat("Did not start device discovery",
                ShadowBluetoothAdapter.getDefaultAdapter().isDiscovering(), is(true));
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

        ListView listView = view.findViewById(R.id.paired_devices);
        ListAdapter adapter = listView.getAdapter();
        View itemView = adapter.getView(0, null, listView);
        listView.performItemClick(itemView, 0, adapter.getItemId(0));

        assertThat("Received wrong value on device listener",
                ((DummyActivity)activityController.get()).getSelectedDevice(),
                is(BLUETOOTH_DEVICE_ID));
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
                ShadowBluetoothAdapter.getDefaultAdapter().getRemoteDevice(BLUETOOTH_DEVICE_ID));
        RuntimeEnvironment.application.sendBroadcast(intent);

        View view = deviceSelector.getView();
        assertThat("Did not discover new device",
                ((ListView) view.findViewById(R.id.new_devices)).getAdapter().getCount(), is(1));
    }

    /**
     * Check that finishing discovery without finding any devices works fine.
     */
    @Test
    public void finishDiscoveryNoDevices() {
        activityController.create().resume().visible();

        // Directly send out intent for discovery finished
        Intent intent = new Intent(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        RuntimeEnvironment.application.sendBroadcast(intent);

        View view = deviceSelector.getView();

        assertThat("Found device although none was expected",
                ((ListView) view.findViewById(R.id.new_devices)).getAdapter()
                        .getItem(0).toString(),
                is(view.getResources().getText(R.string.none_found)));
    }
}
