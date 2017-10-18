package de.wohlfrom.presenter;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * A simple test that verifies that our settings class can properly store and restore the settings.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class SettingsTest {
    private Settings mSettings;

    @Before
    public void setUp() {
        mSettings = new Settings(Robolectric.setupActivity(DummyActivity.class));
    }

    /**
     * Verify that "silence device during presentation" setting is stored and restored properly.
     */
    @Test
    public void verifySilenceDuringPresentationStoring() {
        mSettings.silenceDuringPresentation(true);
        assertThat(mSettings.silenceDuringPresentation(), is(true));
        mSettings.silenceDuringPresentation(false);
        assertThat(mSettings.silenceDuringPresentation(), is(false));
    }

    /**
     * Verify that "use volume keys for navigation" setting is stored and restored properly.
     */
    @Test
    public void verifyUseVolumeKeysForNavigationStoring() {
        mSettings.useVolumeKeysForNavigation(true);
        assertThat(mSettings.useVolumeKeysForNavigation(), is(true));
        mSettings.useVolumeKeysForNavigation(false);
        assertThat(mSettings.useVolumeKeysForNavigation(), is(false));
    }
}
