package de.wohlfrom.presenter;

import android.app.Fragment;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertNotNull;
import static org.robolectric.util.FragmentTestUtil.startFragment;

/**
 * Verify that the "connecting" fragment can be properly started.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml")
public class ConnectingTest {

    /**
     * Verify that the fragment can be started.
     */
    @Test
    public void testFragment() {
        Fragment fragment = new Connecting();
        startFragment(fragment);
        assertNotNull(fragment);
    }
}
