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

package de.wohlfrom.presenter.bluetooth.connectors;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.os.Message;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBluetoothAdapter;
import org.robolectric.shadows.ShadowBluetoothDevice;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import de.wohlfrom.presenter.BuildConfig;

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
        assertThat(control.getState(), is(BluetoothPresenterControl.ServiceState.NONE));
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
        assertThat(control.getState(), is(BluetoothPresenterControl.ServiceState.CONNECTING));

        // Make sure we clean everything up
        control.stop();
    }

    /**
     * Test that we can successfully connect to another device.
     */
    @Test
    public void testConnectedStateSuccess() throws InterruptedException {
        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {});
        BluetoothDevice bluetoothDevice = ShadowBluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(DEVICE_ADDRESS);
        control.connect(bluetoothDevice);
        Thread.sleep(200);
        assertThat(control.getState(), is(BluetoothPresenterControl.ServiceState.CONNECTED));

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
        Thread.sleep(150);
        assertThat(control.getState(), is(BluetoothPresenterControl.ServiceState.NONE));

        // Make sure we clean everything up
        control.stop();
    }

    /**
     * Test that the "connected" event is sent successfully if the connection succeeded.
     */
    @Test
    public void testConnectedEventSuccess() throws InterruptedException {
        final CountDownLatch messageReceived = new CountDownLatch(1);

        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                assertThat("Received wrong message from handler", msg.what,
                        is(BluetoothPresenterControl.ServiceState.CONNECTED.ordinal()));
                assertThat("Got wrong connection result",
                        msg.getData().getBoolean(BluetoothPresenterControl.RESULT_VALUES[0]),
                        is(true));
                assertThat("Got wrong device to which we are connected",
                        msg.getData().getString(BluetoothPresenterControl.RESULT_VALUES[1]),
                        is(DEVICE_NAME));

                messageReceived.countDown();
            }
        });
        BluetoothDevice bluetoothDevice = ShadowBluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(DEVICE_ADDRESS);
        shadowOf(bluetoothDevice).setName(DEVICE_NAME);
        control.connect(bluetoothDevice);
        Thread.sleep(150);
        ShadowLooper.runUiThreadTasks();
        assertThat("Handler was not called",
                messageReceived.await(100, TimeUnit.MILLISECONDS), is(true));

        // Make sure we clean everything up
        control.stop();
    }

    /**
     * Test that the "connected" event is sent successfully if the connection fails.
     */
    @Test
    public void testConnectedEventFailure() throws InterruptedException {
        ShadowBluetoothSocket.setConnectionSucceed(false);
        final CountDownLatch messageReceived = new CountDownLatch(1);

        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                assertThat("Received wrong message from handler", msg.what,
                        is(BluetoothPresenterControl.ServiceState.CONNECTED.ordinal()));
                assertThat("Got wrong connection result",
                        msg.getData().getBoolean(BluetoothPresenterControl.RESULT_VALUES[0]),
                        is(false));

                messageReceived.countDown();
            }
        });
        BluetoothDevice bluetoothDevice = ShadowBluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(DEVICE_ADDRESS);
        control.connect(bluetoothDevice);
        Thread.sleep(150);
        ShadowLooper.runUiThreadTasks();
        assertThat("Handler was not called",
                messageReceived.await(100, TimeUnit.MILLISECONDS), is(true));

        // Make sure we clean everything up
        control.stop();
    }

    /**
     * Test that disconnection from client works fine.
     */
    @Test
    public void testClientDisconnected() throws InterruptedException {
        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {});
        BluetoothDevice bluetoothDevice = ShadowBluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(DEVICE_ADDRESS);
        control.connect(bluetoothDevice);
        Thread.sleep(150);
        assertThat(control.getState(), is(BluetoothPresenterControl.ServiceState.CONNECTED));

        control.stop();
        assertThat(control.getState(), is(BluetoothPresenterControl.ServiceState.NONE));
    }

    /**
     * Test that disconnection from server is properly recognized.
     */
    @Test
    public void testServerDisconnected() throws InterruptedException {
        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {});
        BluetoothDevice bluetoothDevice = ShadowBluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(DEVICE_ADDRESS);
        control.connect(bluetoothDevice);
        Thread.sleep(200);
        assertThat(control.getState(), is(BluetoothPresenterControl.ServiceState.CONNECTED));

        ShadowBluetoothSocket.setFailReading(true);
        Thread.sleep(100); // Wait some time until the thread really stopped
        assertThat(control.getState(), is(BluetoothPresenterControl.ServiceState.NONE));

        // Make sure we clean everything up
        control.stop();
    }

    /**
     * Test that disconnection from server is properly recognized.
     */
    @Test
    public void testServerEventDisconnected() throws InterruptedException {
        final CountDownLatch messageReceived = new CountDownLatch(1);

        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {
            @Override
            public void handleMessage(Message msg) {
                if (msg.what == BluetoothPresenterControl.ServiceState.NONE.ordinal()) {
                    messageReceived.countDown();
                }
            }
        });
        BluetoothDevice bluetoothDevice = ShadowBluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(DEVICE_ADDRESS);
        control.connect(bluetoothDevice);
        Thread.sleep(200);
        assertThat(control.getState(), is(BluetoothPresenterControl.ServiceState.CONNECTED));

        ShadowBluetoothSocket.setFailReading(true);
        Thread.sleep(150);
        ShadowLooper.runUiThreadTasks();
        assertThat("Handler was not called",
                messageReceived.await(100, TimeUnit.MILLISECONDS), is(true));

        // Make sure we clean everything up
        control.stop();
    }

    /**
     * Test that writing data using the presenter control works without errors.
     */
    @Test
    public void testWriteData() throws InterruptedException {
        BluetoothPresenterControl control = new BluetoothPresenterControl(new Handler() {});
        BluetoothDevice bluetoothDevice = ShadowBluetoothAdapter.getDefaultAdapter()
                .getRemoteDevice(DEVICE_ADDRESS);
        control.connect(bluetoothDevice);
        Thread.sleep(150);
        assertThat(control.getState(), is(BluetoothPresenterControl.ServiceState.CONNECTED));

        control.write("Test".getBytes());
        assertThat(ShadowBluetoothSocket.getLastTransmittedString(), is("Test"));

        // Make sure we clean everything up
        control.stop();
    }
}
