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
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Build;
import android.view.KeyEvent;
import android.widget.ListView;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowBluetoothAdapter;
import org.robolectric.shadows.ShadowBluetoothDevice;
import org.robolectric.shadows.ShadowLooper;

import java.util.HashSet;

import de.wohlfrom.presenter.R;
import de.wohlfrom.presenter.Settings;
import de.wohlfrom.presenter.connectors.Command;
import de.wohlfrom.presenter.connectors.ProtocolVersion;
import de.wohlfrom.presenter.connectors.RemoteControl;
import de.wohlfrom.presenter.connectors.wifi.WifiConnector;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.robolectric.Shadows.shadowOf;

/**
 * These tests ensure that the bluetooth connector fragment works properly.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml",
        shadows = {ShadowBluetoothAdapter.class, ShadowBluetoothDevice.class,
                ShadowBluetoothSocket.class},
        sdk = {
                Build.VERSION_CODES.M
        })
public class BluetoothConnectorTest {

    /**
     * The address of the bluetooth device we are connecting to
     */
    private static final String DEVICE_ADDRESS = "12:34:56:78:AB:CD";

    /**
     * The version information to transmit from our fake server.
     */
    private static final String SERVER_VERSION_SUCCESS
            = "{ \"type\" = \"version\"," +
            " \"data\" = '" + RemoteControl.CLIENT_PROTOCOL_VERSION + "' }\n\n";
    private static final String SERVER_VERSION_FAILURE
            = "{ \"type\" = \"version\"," +
            " \"data\" = '" + new ProtocolVersion(-1, -1).toString() + "' }\n\n";
    private static final String SERVER_INVALID_STRING = "Foo, Bar\n\n";

    /**
     * This request code is used to verify that the activity result is really our request to
     * enable bt.
     */
    private static final int REQUEST_ENABLE_BT = 1;

    /**
     * Initialize the bluetooth adapter before each testcase
     */
    @Before
    public void initBluetoothAdapter() {
        shadowOf(BluetoothAdapter.getDefaultAdapter()).setEnabled(true);

        ShadowBluetoothSocket.setFailClosing(false);
        ShadowBluetoothSocket.setFailReading(false);
        ShadowBluetoothSocket.setFailStreamGetter(false);
        ShadowBluetoothSocket.setConnectionSucceed(true);
    }

    /**
     * Helper method to create a new device and add it to bonded device list.
     */
    private void initBondedDevice() {
        HashSet<BluetoothDevice> devices = new HashSet<>();
        devices.add(ShadowBluetoothDevice.newInstance(DEVICE_ADDRESS));
        shadowOf(BluetoothAdapter.getDefaultAdapter()).setBondedDevices(devices);
    }

    /**
     * Verifies that the basic lifecycle works without any exceptions.
     * Basic lifecycle means, that bluetooth is enabled and the activity is started and stopped.
     */
    @Test
    public void verifyLifecycleBluetoothEnabled() {
        ActivityController activityController = Robolectric.buildActivity(BluetoothConnector.class);
        activityController.create().resume().pause().destroy();
    }

    /**
     * Verifies that the activity can be started, stopped and started again.
     */
    @Test
    public void verifyStartingTwice() {
        ActivityController activityController = Robolectric.buildActivity(BluetoothConnector.class);
        activityController.create().resume().pause().resume().pause().destroy();
    }

    /**
     * Verify that the correct intent is sent to request bluetooth enabling if it is disabled
     * on activity start.
     */
    @Test
    public void verifyLifecycleBluetoothDisabled() {
        shadowOf(BluetoothAdapter.getDefaultAdapter()).setEnabled(false);
        ActivityController activityController = Robolectric.buildActivity(BluetoothConnector.class);
        activityController.create().resume();

        BluetoothConnector connector = (BluetoothConnector) activityController.get();

        String intentAction =
                shadowOf(connector).getNextStartedActivityForResult().intent.getAction();
        assertThat("Did not get request to enable bluetooth",
                intentAction, is(BluetoothAdapter.ACTION_REQUEST_ENABLE));
    }

    /**
     * Verifies that disabling bluetooth while bluetooth connector is running works fine and
     * that the connector is closed properly.
     */
    @Test
    public void checkBluetoothDisabling() {
        ActivityController activityController = Robolectric.buildActivity(BluetoothConnector.class);
        activityController.create().resume();

        Intent intent = new Intent(BluetoothAdapter.ACTION_STATE_CHANGED);
        intent.putExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);
        RuntimeEnvironment.application.sendBroadcast(intent);

        ShadowActivity activityShadow = shadowOf((BluetoothConnector) activityController.get());
        assertThat("Did not finish bluetooth connector", activityShadow.isFinishing(), is(true));
    }

    /**
     * Verifies that selecting a specific device works fine and connecting screen is displayed
     * properly.
     */
    @Test
    public void selectDevice() {
        initBondedDevice();

        ActivityController activityController = Robolectric.buildActivity(BluetoothConnector.class);
        BluetoothConnector connector =
                (BluetoothConnector) activityController.create().resume().visible().get();

        try {
            ((ListView) connector.findViewById(R.id.paired_devices)).performItemClick(
                    ((ListView) connector.findViewById(R.id.paired_devices)).getChildAt(0), 0, 0);

            assertThat("Did not find 'connecting' screen", connector.getTitle(),
                    is(connector.getString(R.string.connecting_to_service)));

        } finally {
            activityController.stop().destroy();
        }
    }

    /**
     * Verifies that connecting to a device continues also if activity is restarted.
     */
    @Test
    public void selectDeviceRestartActivity() {
        initBondedDevice();

        ActivityController activityController = Robolectric.buildActivity(BluetoothConnector.class);
        BluetoothConnector connector =
                (BluetoothConnector) activityController.create().resume().visible().get();

        ((ListView) connector.findViewById(R.id.paired_devices)).performItemClick(
                ((ListView) connector.findViewById(R.id.paired_devices)).getChildAt(0), 0, 0);


        try {
            // Make sure that the activity has initially been started
            assertThat("Did not initiate connection to device", connector.getTitle(),
                    is(connector.getString(R.string.connecting_to_service)));

            // Restart activity
            activityController.stop().resume().visible();
            assertThat("Did not resume connection initiation.", connector.getTitle(),
                    is(connector.getString(R.string.connecting_to_service)));
        } finally {
            activityController.stop().destroy();
        }
    }

    /**
     * Verifies that connection to a device works fine.
     *
     * @throws InterruptedException If waiting for the events to run through the pipes fails
     */
    @Test
    public void connectedToDevice() throws InterruptedException {
        initBondedDevice();

        ShadowBluetoothSocket.setTransmittedString(SERVER_VERSION_SUCCESS);

        ActivityController activityController = Robolectric.buildActivity(BluetoothConnector.class);
        BluetoothConnector connector =
                (BluetoothConnector) activityController.create().resume().visible().get();

        try {
            ((ListView) connector.findViewById(R.id.paired_devices)).performItemClick(
                    ((ListView) connector.findViewById(R.id.paired_devices)).getChildAt(0), 0, 0);

            Thread.sleep(100);
            ShadowLooper.runUiThreadTasks();

            assertThat("Did not find 'prev slide' button",
                    connector.findViewById(R.id.prev_slide), is(notNullValue()));
        } finally {
            activityController.stop().destroy();
        }
    }

    /**
     * Ensures that the connection to the remote device is still established after closing and
     * restoring the activity.
     *
     * @throws InterruptedException If waiting for the events to run through the pipes fails
     */
    @Test
    public void connectedToDeviceRestartActivity() throws InterruptedException {
        initBondedDevice();

        ShadowBluetoothSocket.setTransmittedString(SERVER_VERSION_SUCCESS);

        ActivityController activityController = Robolectric.buildActivity(BluetoothConnector.class);
        BluetoothConnector connector =
                (BluetoothConnector) activityController.create().resume().visible().get();

        try {
            ((ListView) connector.findViewById(R.id.paired_devices)).performItemClick(
                    ((ListView) connector.findViewById(R.id.paired_devices)).getChildAt(0), 0, 0);

            Thread.sleep(100);
            ShadowLooper.runUiThreadTasks();

            assertThat("Did not find 'next slide' button",
                    connector.findViewById(R.id.next_slide), is(notNullValue()));

            // Restart activity
            activityController.stop().resume().visible();
            assertThat("Did not find 'next slide' button after restart",
                    connector.findViewById(R.id.next_slide), is(notNullValue()));
        } finally {
            activityController.stop().destroy();
        }
    }

    /**
     * Ensures that the bluetooth selector is shown if connection fails if the server has an
     * incompatible version.
     *
     * @throws InterruptedException If waiting for the events to run through the pipes fails
     */
    @Test
    public void connectionFailedInvalidVersion() throws InterruptedException {
        initBondedDevice();

        ShadowBluetoothSocket.setTransmittedString(SERVER_INVALID_STRING);

        ActivityController activityController = Robolectric.buildActivity(BluetoothConnector.class);
        BluetoothConnector connector =
                (BluetoothConnector) activityController.create().resume().visible().get();

        try {
            ((ListView) connector.findViewById(R.id.paired_devices)).performItemClick(
                    ((ListView) connector.findViewById(R.id.paired_devices)).getChildAt(0), 0, 0);

            Thread.sleep(100);
            ShadowLooper.runUiThreadTasks();

            assertThat("Did not switch back to bluetooth device selection",
                    connector.findViewById(R.id.button_scan), is(notNullValue()));
        } finally {
            activityController.stop().destroy();
        }
    }

    /**
     * Ensures that the bluetooth selector is shown if connection fails if the server sends an
     * invalid command.
     *
     * @throws InterruptedException If waiting for the events to run through the pipes fails
     */
    @Test
    public void connectionFailedParsingError() throws InterruptedException {
        initBondedDevice();

        ShadowBluetoothSocket.setTransmittedString(SERVER_VERSION_FAILURE);

        ActivityController activityController = Robolectric.buildActivity(BluetoothConnector.class);
        BluetoothConnector connector =
                (BluetoothConnector) activityController.create().resume().visible().get();

        try {
            ((ListView) connector.findViewById(R.id.paired_devices)).performItemClick(
                    ((ListView) connector.findViewById(R.id.paired_devices)).getChildAt(0), 0, 0);

            Thread.sleep(100);
            ShadowLooper.runUiThreadTasks();

            assertThat("Did not switch back to bluetooth device selection",
                    connector.findViewById(R.id.button_scan), is(notNullValue()));
        } finally {
            activityController.stop().destroy();
        }
    }

    /**
     * Ensures that the bluetooth selector is shown if server disconnects from our bluetooth
     * connector.
     *
     * @throws InterruptedException If waiting for the events to run through the pipes fails
     */
    @Test
    public void connectionLost() throws InterruptedException {
        initBondedDevice();

        ShadowBluetoothSocket.setTransmittedString(SERVER_VERSION_SUCCESS);

        ActivityController activityController = Robolectric.buildActivity(BluetoothConnector.class);
        BluetoothConnector connector =
                (BluetoothConnector) activityController.create().resume().visible().get();

        try {
            ((ListView) connector.findViewById(R.id.paired_devices)).performItemClick(
                    ((ListView) connector.findViewById(R.id.paired_devices)).getChildAt(0), 0, 0);

            Thread.sleep(100);
            ShadowLooper.runUiThreadTasks();

            ShadowBluetoothSocket.setFailReading(true);

            Thread.sleep(100);
            ShadowLooper.runUiThreadTasks();

            assertThat("Did not switch back to bluetooth device selection",
                    connector.findViewById(R.id.button_scan), is(notNullValue()));
        } finally {
            activityController.stop().destroy();
        }
    }

    /**
     * Ensures that the bluetooth selector is shown if server disconnects from our bluetooth
     * connector - even while we are currently connecting to the server.
     *
     * @throws InterruptedException If waiting for the events to run through the pipes fails
     */
    @Test
    public void connectionLostConnecting() throws InterruptedException {
        initBondedDevice();

        ShadowBluetoothSocket.setTransmittedString(SERVER_VERSION_SUCCESS);
        ShadowBluetoothSocket.setConnectionSucceed(false);

        ActivityController activityController = Robolectric.buildActivity(BluetoothConnector.class);

        BluetoothConnector connector =
                (BluetoothConnector) activityController.create().resume().visible().get();

        try {
            ((ListView) connector.findViewById(R.id.paired_devices)).performItemClick(
                    ((ListView) connector.findViewById(R.id.paired_devices)).getChildAt(0), 0, 0);

            Thread.sleep(100);
            ShadowLooper.runUiThreadTasks();

            assertThat("Did not switch back to bluetooth device selection",
                    connector.findViewById(R.id.button_scan), is(notNullValue()));
        } finally {
            activityController.stop().destroy();
        }
    }

    /**
     * Verifies that enabling bluetooth after requested by the presenter works fine and won't crash
     * the system.
     */
    @Test
    public void bluetoothEnableRequestSuccess() {
        shadowOf(BluetoothAdapter.getDefaultAdapter()).setEnabled(false);
        ActivityController activityController = Robolectric.buildActivity(BluetoothConnector.class);
        activityController.create().resume();

        BluetoothConnector connector = (BluetoothConnector) activityController.get();

        Intent intent = shadowOf(connector).getNextStartedActivityForResult().intent;
        assertThat("Did not get request to enable bluetooth",
                intent.getAction(), is(BluetoothAdapter.ACTION_REQUEST_ENABLE));

        // Enable bluetooth
        shadowOf(BluetoothAdapter.getDefaultAdapter()).setEnabled(true);
        ((BluetoothConnector) activityController.get())
                .onActivityResult(REQUEST_ENABLE_BT, Activity.RESULT_OK, intent);

        // Send second result with unknown request code, should also work fine
        ((BluetoothConnector) activityController.get())
                .onActivityResult(0, Activity.RESULT_OK, intent);
    }

    /**
     * Verifies that denying bluetooth after requested by the presenter works fine and won't crash
     * the system.
     */
    @Test
    public void bluetoothEnableRequestDenied() {
        shadowOf(BluetoothAdapter.getDefaultAdapter()).setEnabled(false);
        ActivityController activityController = Robolectric.buildActivity(BluetoothConnector.class);
        activityController.create().resume();

        BluetoothConnector connector = (BluetoothConnector) activityController.get();

        Intent intent = shadowOf(connector).getNextStartedActivityForResult().intent;
        assertThat("Did not get request to enable bluetooth",
                intent.getAction(), is(BluetoothAdapter.ACTION_REQUEST_ENABLE));

        // Deny request
        ((BluetoothConnector) activityController.get()).onActivityResult(
                REQUEST_ENABLE_BT, Activity.RESULT_CANCELED, intent);

        assertThat("Did not stop the bluetooth connector",
                ((BluetoothConnector) activityController.get()).isFinishing(), is(true));
    }

    /**
     * Verifies that the button keys are sent properly if navigation using volume keys is enabled.
     * Test all methods at once since they are quite similar from implementation and if one test
     * fails usually all tests fail.
     */
    @Test
    public void verifyVolumeNavigationEnabled() throws InterruptedException {
        initBondedDevice();

        ShadowBluetoothSocket.setTransmittedString(SERVER_VERSION_SUCCESS);

        ActivityController activityController = Robolectric.buildActivity(BluetoothConnector.class);
        BluetoothConnector connector =
                (BluetoothConnector) activityController.create().resume().visible().get();

        try {
            ((ListView) connector.findViewById(R.id.paired_devices)).performItemClick(
                    ((ListView) connector.findViewById(R.id.paired_devices)).getChildAt(0), 0, 0);

            Thread.sleep(100);
            ShadowLooper.runUiThreadTasks();

            connector.onKeyUp(KeyEvent.KEYCODE_VOLUME_UP, null);
            assertThat("Did not emit event for 'volume up key'",
                    ShadowBluetoothSocket.getLastTransmittedString(),
                    is("{ \"type\": \"command\", \"data\": \""
                            + Command.NEXT_SLIDE.getCommand() + "\"}\n\n"));

            connector.onKeyUp(KeyEvent.KEYCODE_VOLUME_DOWN, null);
            assertThat("Did not emit event for 'volume down key'",
                    ShadowBluetoothSocket.getLastTransmittedString(),
                    is("{ \"type\": \"command\", \"data\": \""
                            + Command.PREV_SLIDE.getCommand() + "\"}\n\n"));

        } finally {
            activityController.stop().destroy();
        }
    }

    /**
     * Verifies that the button keys are not sent if navigation using volume keys is disabled.
     * Test all methods at once since they are quite similar from implementation and if one test
     * fails usually all tests fail.
     */
    @Test
    public void verifyVolumeNavigationDisabled() throws InterruptedException {
        initBondedDevice();

        ShadowBluetoothSocket.setTransmittedString(SERVER_VERSION_SUCCESS);

        ActivityController activityController = Robolectric.buildActivity(BluetoothConnector.class);
        BluetoothConnector connector =
                (BluetoothConnector) activityController.create().resume().visible().get();

        Settings settings = new Settings(connector);
        settings.useVolumeKeysForNavigation(false);

        try {
            ((ListView) connector.findViewById(R.id.paired_devices)).performItemClick(
                    ((ListView) connector.findViewById(R.id.paired_devices)).getChildAt(0), 0, 0);

            Thread.sleep(100);
            ShadowLooper.runUiThreadTasks();

            ShadowBluetoothSocket.resetLastTransmittedString();

            connector.onKeyUp(KeyEvent.KEYCODE_VOLUME_UP, null);
            Thread.sleep(100);
            assertThat("Received unexpected event for 'volume up key'",
                    ShadowBluetoothSocket.getLastTransmittedString(),
                    is(""));

            connector.onKeyUp(KeyEvent.KEYCODE_VOLUME_DOWN, null);
            Thread.sleep(100);
            assertThat("Received unexpected event for 'volume down key'",
                    ShadowBluetoothSocket.getLastTransmittedString(),
                    is(""));

        } finally {
            activityController.stop().destroy();
        }
    }

    /**
     * Verifies that each button press listener emits the correct command to the server.
     * Test all methods at once since they are quite similar from implementation and if one test
     * fails usually all tests fail.
     */
    @Test
    public void verifyButtonPresses() throws InterruptedException {
        initBondedDevice();

        ShadowBluetoothSocket.setTransmittedString(SERVER_VERSION_SUCCESS);

        ActivityController activityController = Robolectric.buildActivity(BluetoothConnector.class);
        BluetoothConnector connector =
                (BluetoothConnector) activityController.create().resume().visible().get();

        try {
            ((ListView) connector.findViewById(R.id.paired_devices)).performItemClick(
                    ((ListView) connector.findViewById(R.id.paired_devices)).getChildAt(0), 0, 0);

            Thread.sleep(100);
            ShadowLooper.runUiThreadTasks();

            assertThat("Did not find 'next slide' button",
                    connector.findViewById(R.id.next_slide), is(notNullValue()));

            connector.onNextSlide();
            assertThat("Did not emit event for 'onNextSlide'",
                    ShadowBluetoothSocket.getLastTransmittedString(),
                    is("{ \"type\": \"command\", \"data\": \""
                            + Command.NEXT_SLIDE.getCommand() + "\"}\n\n"));

            connector.onPrevSlide();
            assertThat("Did not emit event for 'onPrevSlide'",
                    ShadowBluetoothSocket.getLastTransmittedString(),
                    is("{ \"type\": \"command\", \"data\": \""
                            + Command.PREV_SLIDE.getCommand() + "\"}\n\n"));

            connector.onStartPresentation();
            assertThat("Did not emit event for 'onStartPresentation'",
                    ShadowBluetoothSocket.getLastTransmittedString(),
                    is("{ \"type\": \"command\", \"data\": \""
                            + Command.START_PRESENTATION.getCommand() + "\"}\n\n"));

            connector.onStopPresentation();
            assertThat("Did not emit event for 'onStopPresentation'",
                    ShadowBluetoothSocket.getLastTransmittedString(),
                    is("{ \"type\": \"command\", \"data\": \""
                            + Command.STOP_PRESENTATION.getCommand() + "\"}\n\n"));

        } finally {
            activityController.stop().destroy();
        }
    }
}
