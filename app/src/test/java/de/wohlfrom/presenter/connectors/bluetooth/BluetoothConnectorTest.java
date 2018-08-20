package de.wohlfrom.presenter.connectors.bluetooth;

import android.bluetooth.BluetoothAdapter;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowBluetoothAdapter;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.robolectric.Shadows.shadowOf;

/**
 * These tests ensure that the bluetooth connector fragment works properly.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml",
        shadows = {ShadowBluetoothAdapter.class})
public class BluetoothConnectorTest {

    /**
     * Verifies that the basic lifecycle works without any exceptions.
     * Basic lifecycle means, that bluetooth is enabled and the activity is started and stopped.
     */
    @Test
    public void verifyLifecycleBluetoothEnabled() {
        shadowOf(BluetoothAdapter.getDefaultAdapter()).setEnabled(true);
        ActivityController activityController = Robolectric.buildActivity(BluetoothConnector.class);
        activityController.create().resume().destroy();
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
}
