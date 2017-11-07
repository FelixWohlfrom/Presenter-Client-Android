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

package de.wohlfrom.presenter.connectors.bluetooth;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowBluetoothAdapter;
import org.robolectric.shadows.ShadowBluetoothDevice;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import de.wohlfrom.presenter.BuildConfig;
import de.wohlfrom.presenter.connectors.Command;
import de.wohlfrom.presenter.connectors.ProtocolVersion;
import de.wohlfrom.presenter.connectors.RemoteControl;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * These testcases verify that the BluetoothPresenterControl class works as expected.
 * It checks connecting, disconnecting and failure handling.
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class,
        shadows = {ShadowBluetoothAdapter.class, ShadowBluetoothDevice.class,
                ShadowBluetoothSocket.class})
public class BluetoothPresenterControlTest {

    /** The address and name of the bluetooth device we are connecting to */
    private static final String DEVICE_ADDRESS = "12:34:56:78:AB:CD";
    private static final String DEVICE_NAME = "DeviceName";

    /** The version information to transmit from our fake server. */
    private static final String SERVER_VERSION_SUCCESS
            = "{ \"type\" = \"version\"," +
            " \"data\" = '" + RemoteControl.CLIENT_PROTOCOL_VERSION + "' }\n\n";
    private static final String SERVER_VERSION_FAILURE
            = "{ \"type\" = \"version\"," +
            " \"data\" = '" + new ProtocolVersion(-1, -1).toString() + "' }\n\n";

    /** The time in ms that the service might take to change the state. */
    private static final int SERVICE_STATE_CHANGE_TIME = 500;
    /** The time in ms that we want to wait maximum for a message to be received. */
    private static final int MESSAGE_RECEIVING_TIMEOUT = 300;

    /**
     * Initialize our fake bluetooth socket.
     * By default, we don't fail the reading and succeed the connection.
     * These values are overwritten in the testcases that need other values.
     */
    @Before
    public void initBluetoothSocket() {
        ShadowBluetoothSocket.setFailReading(false);
        ShadowBluetoothSocket.setConnectionSucceed(true);
    }

    /**
     * Test that the class can be instantiated successfully.
     */
    @Test
    public void instantiationTest() {
        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {});
        assertThat(control, is(notNullValue()));
        assertThat(control.getState(), is(RemoteControl.ServiceState.NONE));
    }

    /**
     * Test that we can successfully initiate a connection to another device.
     */
    @Test
    public void testConnectingState() {
        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {});
        BluetoothDevice bluetoothDevice = ShadowBluetoothAdapter.getDefaultAdapter()
                                                    .getRemoteDevice(DEVICE_ADDRESS);
        control.connect(bluetoothDevice);
        assertThat(control.getState(), is(RemoteControl.ServiceState.CONNECTING));

        // Make sure we clean everything up
        control.stop();
    }

    /**
     * Test that we can successfully connect to another device.
     */
    @Test
    public void testConnectedStateSuccess() throws InterruptedException {
        ShadowBluetoothSocket.setTransmittedString(SERVER_VERSION_SUCCESS);

        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {});
        BluetoothDevice bluetoothDevice = ShadowBluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(DEVICE_ADDRESS);
        control.connect(bluetoothDevice);
        Thread.sleep(SERVICE_STATE_CHANGE_TIME);
        assertThat(control.getState(), is(RemoteControl.ServiceState.CONNECTED));

        // Make sure we clean everything up
        control.stop();
    }

    /**
     * Test that if connection fails, we get the correct state of the service.
     */
    @Test
    public void testConnectedStateFailure() throws InterruptedException {
        ShadowBluetoothSocket.setConnectionSucceed(false);
        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {});
        BluetoothDevice bluetoothDevice = ShadowBluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(DEVICE_ADDRESS);
        control.connect(bluetoothDevice);
        Thread.sleep(SERVICE_STATE_CHANGE_TIME);
        assertThat(control.getState(), is(RemoteControl.ServiceState.NONE));

        // Make sure we clean everything up
        control.stop();
    }

    /**
     * Test that if connection fails due to invalid version information,
     * we get the correct state of the service.
     */
    @Test
    public void testConnectedStateFailureInvalidVersion() throws InterruptedException {
        ShadowBluetoothSocket.setTransmittedString(SERVER_VERSION_FAILURE);
        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {});
        BluetoothDevice bluetoothDevice = ShadowBluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(DEVICE_ADDRESS);
        control.connect(bluetoothDevice);
        Thread.sleep(SERVICE_STATE_CHANGE_TIME);
        assertThat(control.getState(), is(RemoteControl.ServiceState.NONE));

        // Make sure we clean everything up
        control.stop();
    }

    /**
     * Test that "connecting" and "connected" event are sent successfully if the
     * connection succeeded.
     */
    @Test
    public void testConnectedEventSuccess() throws InterruptedException {
        final CountDownLatch connectingMessageReceived = new CountDownLatch(1);
        final CountDownLatch connectedMessageReceived = new CountDownLatch(1);

        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == RemoteControl.ServiceState.CONNECTING.ordinal()) {
                    connectingMessageReceived.countDown();
                } else if (msg.what == RemoteControl.ServiceState.CONNECTED.ordinal()) {
                    assertThat("Got wrong connection result",
                            msg.getData().getBoolean(RemoteControl.RESULT_VALUES[0]),
                            is(true));
                    assertThat("Got wrong device to which we are connected",
                            msg.getData().getString(RemoteControl.RESULT_VALUES[1]),
                            is(DEVICE_NAME));

                    connectedMessageReceived.countDown();
                }
            }
        });

        ShadowBluetoothSocket.setTransmittedString(SERVER_VERSION_SUCCESS);

        BluetoothDevice bluetoothDevice = ShadowBluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(DEVICE_ADDRESS);
        shadowOf(bluetoothDevice).setName(DEVICE_NAME);
        control.connect(bluetoothDevice);
        Thread.sleep(SERVICE_STATE_CHANGE_TIME);

        ShadowLooper.runUiThreadTasks();
        assertThat("Did not receive 'connecting' message",
                connectingMessageReceived.await(MESSAGE_RECEIVING_TIMEOUT, TimeUnit.MILLISECONDS),
                is(true));
        assertThat("Did not receive 'connected' message",
                connectedMessageReceived.await(MESSAGE_RECEIVING_TIMEOUT, TimeUnit.MILLISECONDS),
                is(true));

        // Make sure we clean everything up
        control.stop();
    }

    /**
     * Test that the  "connecting" and "connected" event are sent successfully if the
     * connection fails and that the result state is correct.
     */
    @Test
    public void testConnectedEventFailure() throws InterruptedException {
        ShadowBluetoothSocket.setConnectionSucceed(false);
        final CountDownLatch connectingMessageReceived = new CountDownLatch(1);
        final CountDownLatch connectedMessageReceived = new CountDownLatch(1);

        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == RemoteControl.ServiceState.CONNECTING.ordinal()) {
                    connectingMessageReceived.countDown();

                } else if (msg.what == RemoteControl.ServiceState.CONNECTED.ordinal()) {
                    assertThat("Got wrong connection result",
                            msg.getData().getBoolean(RemoteControl.RESULT_VALUES[0]),
                            is(false));

                    connectedMessageReceived.countDown();
                }
            }
        });
        BluetoothDevice bluetoothDevice = ShadowBluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(DEVICE_ADDRESS);
        control.connect(bluetoothDevice);
        Thread.sleep(SERVICE_STATE_CHANGE_TIME);

        ShadowLooper.runUiThreadTasks();
        assertThat("Did not receive 'connecting' event",
                connectingMessageReceived.await(MESSAGE_RECEIVING_TIMEOUT, TimeUnit.MILLISECONDS),
                is(true));
        assertThat("Did not receive 'connected' event",
                connectedMessageReceived.await(MESSAGE_RECEIVING_TIMEOUT, TimeUnit.MILLISECONDS),
                is(true));

        // Make sure we clean everything up
        control.stop();
    }

    /**
     * Test that the "error" event is sent successfully if the server sends incompatible version
     * information.
     */
    @Test
    public void testConnectedEventIncompatibleVersion() throws InterruptedException {
        ShadowBluetoothSocket.setTransmittedString(SERVER_VERSION_FAILURE);
        final CountDownLatch messageReceived = new CountDownLatch(1);

        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == RemoteControl.ServiceState.ERROR.ordinal()) {
                    assertThat("Got wrong error type",
                            msg.getData().getString(RemoteControl.RESULT_VALUES[2]),
                            is(RemoteControl.ERROR_TYPES.VERSION.toString()));

                    messageReceived.countDown();
                }
            }
        });
        BluetoothDevice bluetoothDevice = ShadowBluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(DEVICE_ADDRESS);
        control.connect(bluetoothDevice);
        Thread.sleep(SERVICE_STATE_CHANGE_TIME);

        ShadowLooper.runUiThreadTasks();
        assertThat("Did not receive 'error' event",
                messageReceived.await(MESSAGE_RECEIVING_TIMEOUT, TimeUnit.MILLISECONDS),
                is(true));

        // Make sure we clean everything up
        control.stop();
    }

    /**
     * Test that the "error" event is sent successfully if the server sends invalid data.
     */
    @Test
    public void testConnectedEventInvalidData() throws InterruptedException {
        ShadowBluetoothSocket.setTransmittedString("This is invalid json data\n\n");
        final CountDownLatch messageReceived = new CountDownLatch(1);

        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == RemoteControl.ServiceState.ERROR.ordinal()) {
                    assertThat("Got wrong error type",
                            msg.getData().getString(RemoteControl.RESULT_VALUES[2]),
                            is(RemoteControl.ERROR_TYPES.PARSING.toString()));

                    messageReceived.countDown();
                }
            }
        });
        BluetoothDevice bluetoothDevice = ShadowBluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(DEVICE_ADDRESS);
        control.connect(bluetoothDevice);
        Thread.sleep(SERVICE_STATE_CHANGE_TIME);

        ShadowLooper.runUiThreadTasks();
        assertThat("Did not receive 'error' event",
                messageReceived.await(MESSAGE_RECEIVING_TIMEOUT, TimeUnit.MILLISECONDS),
                is(true));

        // Make sure we clean everything up
        control.stop();
    }

    /**
     * Test that disconnection from client works fine.
     */
    @Test
    public void testClientDisconnected() throws InterruptedException {
        ShadowBluetoothSocket.setTransmittedString(SERVER_VERSION_SUCCESS);

        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {});
        BluetoothDevice bluetoothDevice = ShadowBluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(DEVICE_ADDRESS);
        control.connect(bluetoothDevice);

        Thread.sleep(SERVICE_STATE_CHANGE_TIME);
        assertThat(control.getState(), is(RemoteControl.ServiceState.CONNECTED));

        control.stop();
        assertThat(control.getState(), is(RemoteControl.ServiceState.NONE));
    }

    /**
     * Test that disconnection from server is properly recognized while connection is still
     * establishing and initial information is not exchanged (e.g. version information).
     */
    @Test
    public void testServerDisconnectedDuringInformationExchange() throws InterruptedException {
        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {});
        BluetoothDevice bluetoothDevice = ShadowBluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(DEVICE_ADDRESS);
        control.connect(bluetoothDevice);
        Thread.sleep(SERVICE_STATE_CHANGE_TIME);
        assertThat(control.getState(), is(RemoteControl.ServiceState.CONNECTING));

        ShadowBluetoothSocket.setFailReading(true);
        Thread.sleep(SERVICE_STATE_CHANGE_TIME); // Wait some time until the thread really stopped
        assertThat(control.getState(), is(RemoteControl.ServiceState.NONE));

        // Make sure we clean everything up
        control.stop();
    }

    /**
     * Test that disconnection from server is properly recognized.
     */
    @Test
    public void testServerDisconnectedAfterConnectionEstablished() throws InterruptedException {
        ShadowBluetoothSocket.setTransmittedString(SERVER_VERSION_SUCCESS);

        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {});
        BluetoothDevice bluetoothDevice = ShadowBluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(DEVICE_ADDRESS);
        control.connect(bluetoothDevice);
        Thread.sleep(SERVICE_STATE_CHANGE_TIME);
        assertThat(control.getState(), is(RemoteControl.ServiceState.CONNECTED));

        ShadowBluetoothSocket.setFailReading(true);
        Thread.sleep(SERVICE_STATE_CHANGE_TIME); // Wait some time until the thread really stopped
        assertThat(control.getState(), is(RemoteControl.ServiceState.NONE));

        // Make sure we clean everything up
        control.stop();
    }

    /**
     * Test that disconnection from server is properly recognized.
     */
    @Test
    public void testServerEventDisconnected() throws InterruptedException {
        ShadowBluetoothSocket.setTransmittedString(SERVER_VERSION_SUCCESS);

        final CountDownLatch messageReceived = new CountDownLatch(1);

        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == RemoteControl.ServiceState.NONE.ordinal()) {
                    messageReceived.countDown();
                }
            }
        });
        BluetoothDevice bluetoothDevice = ShadowBluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(DEVICE_ADDRESS);
        control.connect(bluetoothDevice);
        Thread.sleep(SERVICE_STATE_CHANGE_TIME);
        assertThat(control.getState(), is(RemoteControl.ServiceState.CONNECTED));

        ShadowBluetoothSocket.setFailReading(true);
        Thread.sleep(SERVICE_STATE_CHANGE_TIME);
        ShadowLooper.runUiThreadTasks();
        assertThat("Handler was not called",
                messageReceived.await(MESSAGE_RECEIVING_TIMEOUT, TimeUnit.MILLISECONDS), is(true));

        // Make sure we clean everything up
        control.stop();
    }

    /**
     * Test that writing data using the presenter control works without errors.
     */
    @Test
    public void testWriteCommand() throws InterruptedException {
        ShadowBluetoothSocket.setTransmittedString(SERVER_VERSION_SUCCESS);

        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {});
        BluetoothDevice bluetoothDevice = ShadowBluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(DEVICE_ADDRESS);
        control.connect(bluetoothDevice);
        Thread.sleep(SERVICE_STATE_CHANGE_TIME);
        assertThat(control.getState(), is(RemoteControl.ServiceState.CONNECTED));

        control.sendCommand(Command.NEXT_SLIDE);
        assertThat(ShadowBluetoothSocket.getLastTransmittedString(),
                is("{ \"type\": \"command\", " +
                        "\"data\": \"" + Command.NEXT_SLIDE.getCommand() + "\"}\n\n"));

        // Make sure we clean everything up
        control.stop();
    }
}
