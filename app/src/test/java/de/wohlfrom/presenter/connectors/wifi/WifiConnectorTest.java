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

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.widget.ListView;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowActivity;
import org.robolectric.shadows.ShadowConnectivityManager;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowNetworkInfo;

import java.io.IOException;

import de.wohlfrom.presenter.R;
import de.wohlfrom.presenter.connectors.Command;
import de.wohlfrom.presenter.connectors.ProtocolVersion;
import de.wohlfrom.presenter.connectors.RemoteControl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.robolectric.Shadows.shadowOf;

/**
 * These tests ensure that the bluetooth connector fragment works properly.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml",
        shadows = {
                ShadowConnectivityManager.class
        },
        sdk = {
                Build.VERSION_CODES.M
        })
public class WifiConnectorTest {
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
     * enable wifi.
     */
    private static final int REQUEST_ENABLE_WIFI = 1;
    
    private MockupServer mockupServer = null;
    private BroadcastServer broadcastServer = null;
    private ShadowConnectivityManager shadowConnectivityManager = null;
    private NetworkInfo connectedNetworkInfo = null;
    private NetworkInfo disconnectedNetworkInfo = null;

    /**
     * Initialize our mockup server and initialize wifi connection.
     */
    @Before
    public void initWifiAndServer() throws IOException, InterruptedException {
        mockupServer = new MockupServer();
        broadcastServer = new BroadcastServer();
        broadcastServer.start();
        
        ConnectivityManager connectivityManager = 
                (ConnectivityManager) RuntimeEnvironment.application
                        .getSystemService(Context.CONNECTIVITY_SERVICE);

        shadowConnectivityManager = shadowOf(connectivityManager);

        connectedNetworkInfo = ShadowNetworkInfo.newInstance(
                NetworkInfo.DetailedState.CONNECTED,
                ConnectivityManager.TYPE_WIFI,
                0,
                true,
                NetworkInfo.State.CONNECTED);

        disconnectedNetworkInfo = ShadowNetworkInfo.newInstance(
                NetworkInfo.DetailedState.DISCONNECTED,
                ConnectivityManager.TYPE_WIFI,
                0,
                false,
                NetworkInfo.State.DISCONNECTED);

        shadowConnectivityManager.setActiveNetworkInfo(connectedNetworkInfo);
    }

    @After
    public void cleanupMockupServer() throws IOException, InterruptedException {
        mockupServer.close();
        broadcastServer.stop();
    }

    /**
     * Verifies that the basic lifecycle works without any exceptions.
     * Basic lifecycle means, that wifi is enabled and the activity is started and stopped.
     */
    @Test
    public void verifyLifecycleWifiEnabled() {
        ActivityController activityController = Robolectric.buildActivity(WifiConnector.class);
        activityController.create().resume().pause().destroy();
    }

    /**
     * Verifies that the activity can be started, stopped and started again.
     */
    @Test
    public void verifyStartingTwice() {
        ActivityController activityController = Robolectric.buildActivity(WifiConnector.class);
        activityController.create().resume().pause().resume().pause().destroy();
    }

    /**
     * Verify that the correct intent is sent to request wifi enabling if it is disabled
     * on activity start.
     */
    @Test
    public void verifyLifecycleWifiDisabled() {        
        shadowConnectivityManager.setActiveNetworkInfo(disconnectedNetworkInfo);
        
        ActivityController activityController = Robolectric.buildActivity(WifiConnector.class);
        activityController.create().resume();

        WifiConnector connector = (WifiConnector) activityController.get();

        String intentAction =
                shadowOf(connector).getNextStartedActivityForResult().intent.getAction();
        assertThat("Did not get request to enable wifi",
                intentAction, is(android.provider.Settings.ACTION_WIFI_SETTINGS));
    }

    /**
     * Verifies that disabling wifi while wifi connector is running works fine and
     * that the connector is closed properly.
     */
    @Test
    public void checkWifiDisabling() {
        ActivityController activityController = Robolectric.buildActivity(WifiConnector.class);
        activityController.create().resume();

        try {
            Intent intent = new Intent(ConnectivityManager.CONNECTIVITY_ACTION);
            intent.putExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, true);
            RuntimeEnvironment.application.sendBroadcast(intent);
    
            ShadowActivity activityShadow = shadowOf((WifiConnector) activityController.get());
            assertThat("Did not finish wifi connector", activityShadow.isFinishing(), is(true));
        } finally {
            activityController.stop().destroy();
        }
    }

    /**
     * Verifies that selecting a specific device works fine and connecting screen is displayed
     * properly.
     */
    @Test
    public void selectDevice() throws InterruptedException {
        ActivityController activityController = Robolectric.buildActivity(WifiConnector.class);

        WifiConnector connector =
                (WifiConnector) activityController.create().resume().visible().get();

        Thread.sleep(200);
        ShadowLooper.runUiThreadTasks();
        
        try {
            ((ListView) connector.findViewById(R.id.broadcast_devices)).performItemClick(
                    ((ListView) connector.findViewById(R.id.broadcast_devices)).getChildAt(0), 0, 0);

            Thread.sleep(100);
            ShadowLooper.runUiThreadTasks();

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
    public void selectDeviceRestartActivity() throws InterruptedException {
        ActivityController activityController = Robolectric.buildActivity(WifiConnector.class);

        WifiConnector connector =
                (WifiConnector) activityController.create().resume().visible().get();

        Thread.sleep(100);
        ShadowLooper.runUiThreadTasks();
        
        try {
            ((ListView) connector.findViewById(R.id.broadcast_devices)).performItemClick(
                    ((ListView) connector.findViewById(R.id.broadcast_devices)).getChildAt(0), 0, 0);
            
            Thread.sleep(100);
            ShadowLooper.runUiThreadTasks();

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
        mockupServer.setTransmittedString(SERVER_VERSION_SUCCESS);
        
        ActivityController activityController = Robolectric.buildActivity(WifiConnector.class);
        WifiConnector connector =
                (WifiConnector) activityController.create().resume().visible().get();

        Thread.sleep(100);
        ShadowLooper.runUiThreadTasks();
        
        try {
            ((ListView) connector.findViewById(R.id.broadcast_devices)).performItemClick(
                    ((ListView) connector.findViewById(R.id.broadcast_devices)).getChildAt(0), 0, 0);

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
        mockupServer.setTransmittedString(SERVER_VERSION_SUCCESS);

        ActivityController activityController = Robolectric.buildActivity(WifiConnector.class);
        WifiConnector connector =
                (WifiConnector) activityController.create().resume().visible().get();

        Thread.sleep(100);
        ShadowLooper.runUiThreadTasks();
        
        try {
            ((ListView) connector.findViewById(R.id.broadcast_devices)).performItemClick(
                    ((ListView) connector.findViewById(R.id.broadcast_devices)).getChildAt(0), 0, 0);

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
     * Ensures that the wifi selector is shown if connection fails if the server has an
     * incompatible version.
     *
     * @throws InterruptedException If waiting for the events to run through the pipes fails
     */
    @Test
    public void connectionFailedInvalidVersion() throws InterruptedException {
        mockupServer.setTransmittedString(SERVER_INVALID_STRING);

        ActivityController activityController = Robolectric.buildActivity(WifiConnector.class);
        WifiConnector connector =
                (WifiConnector) activityController.create().resume().visible().get();

        Thread.sleep(100);
        ShadowLooper.runUiThreadTasks();
        
        try {
            ((ListView) connector.findViewById(R.id.broadcast_devices)).performItemClick(
                    ((ListView) connector.findViewById(R.id.broadcast_devices)).getChildAt(0), 0, 0);

            Thread.sleep(100);
            ShadowLooper.runUiThreadTasks();

            assertThat("Did not switch back to wifi broadcast overview",
                    connector.findViewById(R.id.button_manual_connection), is(notNullValue()));
        } finally {
            activityController.stop().destroy();
        }
    }

    /**
     * Ensures that the wifi selector is shown if connection fails if the server sends an
     * invalid command.
     *
     * @throws InterruptedException If waiting for the events to run through the pipes fails
     */
    @Test
    public void connectionFailedParsingError() throws InterruptedException {
        mockupServer.setTransmittedString(SERVER_VERSION_FAILURE);

        ActivityController activityController = Robolectric.buildActivity(WifiConnector.class);
        WifiConnector connector =
                (WifiConnector) activityController.create().resume().visible().get();

        Thread.sleep(100);
        ShadowLooper.runUiThreadTasks();
        
        try {
            ((ListView) connector.findViewById(R.id.broadcast_devices)).performItemClick(
                    ((ListView) connector.findViewById(R.id.broadcast_devices)).getChildAt(0), 0, 0);

            Thread.sleep(100);
            ShadowLooper.runUiThreadTasks();

            assertThat("Did not switch back to wifi broadcast overview",
                    connector.findViewById(R.id.button_manual_connection), is(notNullValue()));
        } finally {
            activityController.stop().destroy();
        }
    }

    /**
     * Ensures that the wifi selector is shown if server disconnects from our wifi connector.
     *
     * @throws InterruptedException If waiting for the events to run through the pipes fails
     */
    @Test
    public void connectionLost() throws InterruptedException, IOException {
        mockupServer.setTransmittedString(SERVER_VERSION_SUCCESS);

        ActivityController activityController = Robolectric.buildActivity(WifiConnector.class);
        WifiConnector connector =
                (WifiConnector) activityController.create().resume().visible().get();

        Thread.sleep(100);
        ShadowLooper.runUiThreadTasks();
        
        try {
            ((ListView) connector.findViewById(R.id.broadcast_devices)).performItemClick(
                    ((ListView) connector.findViewById(R.id.broadcast_devices)).getChildAt(0), 0, 0);

            Thread.sleep(100);
            ShadowLooper.runUiThreadTasks();

            mockupServer.close();

            Thread.sleep(100);
            ShadowLooper.runUiThreadTasks();

            assertThat("Did not switch back to wifi device selection",
                    connector.findViewById(R.id.button_manual_connection), is(notNullValue()));
        } finally {
            activityController.stop().destroy();
        }
    }

    /**
     * Ensures that the wifi selector is shown if server disconnects from our wifi
     * connector - even while we are currently connecting to the server.
     *
     * @throws InterruptedException If waiting for the events to run through the pipes fails
     */
    @Test
    public void connectionLostConnecting() throws InterruptedException, IOException {
        mockupServer.close();
        
        ActivityController activityController = Robolectric.buildActivity(WifiConnector.class);

        WifiConnector connector = 
                (WifiConnector) activityController.create().resume().visible().get();

        Thread.sleep(100);
        ShadowLooper.runUiThreadTasks();
        
        try {
            ((ListView) connector.findViewById(R.id.broadcast_devices)).performItemClick(
                    ((ListView) connector.findViewById(R.id.broadcast_devices)).getChildAt(0), 0, 0);
            
            broadcastServer.stop();
            
            Thread.sleep(3000);
            ShadowLooper.runUiThreadTasks();

            assertThat("Did not switch back to wifi device selection",
                    connector.findViewById(R.id.button_manual_connection), is(notNullValue()));
        } finally {
            activityController.stop().destroy();
        }
    }

    /**
     * Verifies that enabling wifi after requested by the presenter works fine and won't crash
     * the system.
     */
    @Test
    public void wifiEnableRequestSuccess() {
        shadowConnectivityManager.setActiveNetworkInfo(disconnectedNetworkInfo);
        ActivityController activityController = Robolectric.buildActivity(WifiConnector.class);
        activityController.create().resume();

        WifiConnector connector = (WifiConnector) activityController.get();

        Intent intent = shadowOf(connector).getNextStartedActivityForResult().intent;
        assertThat("Did not get request to enable bluetooth",
                intent.getAction(), is(android.provider.Settings.ACTION_WIFI_SETTINGS));

        // Enable bluetooth
        shadowConnectivityManager.setActiveNetworkInfo(connectedNetworkInfo);
        ((WifiConnector) activityController.get())
                .onActivityResult(REQUEST_ENABLE_WIFI, Activity.RESULT_OK, intent);

        // Send second result with unknown request code, should also work fine
        ((WifiConnector) activityController.get())
                .onActivityResult(0, Activity.RESULT_OK, intent);
    }

    /**
     * Verifies that denying wifi after requested by the presenter works fine and won't crash
     * the system.
     */
    @Test
    public void wifiEnableRequestDenied() {
        shadowConnectivityManager.setActiveNetworkInfo(disconnectedNetworkInfo);
        ActivityController activityController = Robolectric.buildActivity(WifiConnector.class);
        activityController.create().resume();

        WifiConnector connector = (WifiConnector) activityController.get();
        
        try {
            Intent intent = shadowOf(connector).getNextStartedActivityForResult().intent;
            assertThat("Did not get request to enable wifi",
                    intent.getAction(), is(android.provider.Settings.ACTION_WIFI_SETTINGS));

            // Deny request
            ((WifiConnector) activityController.get()).onActivityResult(
                    REQUEST_ENABLE_WIFI, Activity.RESULT_CANCELED, intent);

            assertThat("Did not stop the wifi connector",
                    ((WifiConnector) activityController.get()).isFinishing(), is(true));
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
        mockupServer.setTransmittedString(SERVER_VERSION_SUCCESS);

        ActivityController activityController = Robolectric.buildActivity(WifiConnector.class);
        
        WifiConnector connector =
                (WifiConnector) activityController.create().resume().visible().get();

        Thread.sleep(100);
        ShadowLooper.runUiThreadTasks();
        
        try {
            ((ListView) connector.findViewById(R.id.broadcast_devices)).performItemClick(
                ((ListView) connector.findViewById(R.id.broadcast_devices)).getChildAt(0), 0, 0);
            
            Thread.sleep(100);
            ShadowLooper.runUiThreadTasks();

            assertThat("Did not find 'next slide' button",
                    connector.findViewById(R.id.next_slide), is(notNullValue()));

            connector.onNextSlide();
            Thread.sleep(100);
            assertThat("Did not emit event for 'onNextSlide'",
                    mockupServer.getLastTransmittedString(),
                    is("{ \"type\": \"command\", \"data\": \""
                            + Command.NEXT_SLIDE.getCommand() + "\"}\n\n"));

            connector.onPrevSlide();
            Thread.sleep(100);
            assertThat("Did not emit event for 'onPrevSlide'",
                    mockupServer.getLastTransmittedString(),
                    is("{ \"type\": \"command\", \"data\": \""
                            + Command.PREV_SLIDE.getCommand() + "\"}\n\n"));

            connector.onStartPresentation();
            Thread.sleep(100);
            assertThat("Did not emit event for 'onStartPresentation'",
                    mockupServer.getLastTransmittedString(),
                    is("{ \"type\": \"command\", \"data\": \""
                            + Command.START_PRESENTATION.getCommand() + "\"}\n\n"));

            connector.onStopPresentation();
            Thread.sleep(100);
            assertThat("Did not emit event for 'onStopPresentation'",
                    mockupServer.getLastTransmittedString(),
                    is("{ \"type\": \"command\", \"data\": \""
                            + Command.STOP_PRESENTATION.getCommand() + "\"}\n\n"));

        } finally {
            activityController.stop().destroy();
        }
    }
}
