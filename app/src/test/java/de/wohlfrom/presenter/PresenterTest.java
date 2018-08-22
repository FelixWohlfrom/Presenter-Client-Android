package de.wohlfrom.presenter;

import android.app.Activity;
import android.app.Fragment;
import android.media.AudioManager;
import android.os.Build;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;

import de.wohlfrom.presenter.connectors.ProtocolVersion;
import de.wohlfrom.presenter.connectors.RemoteControl;

import static android.content.Context.AUDIO_SERVICE;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Verifies that the presenter fragment works fine.
 * Needs to be executed with Android M support or newer, or deprecated onAttach method
 * will be used within presenter fragment.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml", sdk = {Build.VERSION_CODES.M})
public class PresenterTest {

    /**
     * Verify that all buttons required for support of protocol version 1 are working.
     */
    @Test
    public void checkProtocolVersion1() {
        Fragment presenter = Presenter.newInstance(new ProtocolVersion(1, 1));
        ActivityController activityController = Robolectric.buildActivity(
                PresenterTestDummyActivity.class);
        ((PresenterTestDummyActivity) activityController.get()).setFragment(presenter);
        activityController.create().resume().visible();
        
        assertThat("Did not find 'next slide' button",
                presenter.getView().findViewById(R.id.next_slide),
                is(notNullValue()));

        presenter.getView().findViewById(R.id.next_slide).performClick();
        assertThat("Did not click on 'next slide' button",
                ((PresenterTestDummyActivity) activityController.get()).isNextSlidePressed());

        assertThat("Did not find 'prev slide' button",
                presenter.getView().findViewById(R.id.prev_slide),
                is(notNullValue()));

        presenter.getView().findViewById(R.id.prev_slide).performClick();
        assertThat("Did not click on 'prev slide' button",
                ((PresenterTestDummyActivity) activityController.get()).isPrevSlidePressed());
    }

    /**
     * Verify that all buttons required for support of protocol version 2 are working.
     */
    @Test
    public void checkProtocolVersion2() {
        Fragment presenter = Presenter.newInstance(new ProtocolVersion(2, 2));
        ActivityController activityController = Robolectric.buildActivity(
                PresenterTestDummyActivity.class);
        ((PresenterTestDummyActivity) activityController.get()).setFragment(presenter);
        activityController.create().resume().visible();

        assertThat("Did not find 'start presentation' button",
                presenter.getView().findViewById(R.id.start_presentation),
                is(notNullValue()));

        presenter.getView().findViewById(R.id.start_presentation).performClick();
        assertThat("Did not click on 'start presentation' button",
                ((PresenterTestDummyActivity) activityController.get())
                        .isStartPresentationPressed());

        assertThat("Did not find 'stop presentation' button",
                presenter.getView().findViewById(R.id.stop_presentation),
                is(notNullValue()));

        presenter.getView().findViewById(R.id.stop_presentation).performClick();
        assertThat("Did not click on 'stop presentation' button",
                ((PresenterTestDummyActivity) activityController.get())
                        .isStopPresentationPressed());
    }

    /**
     * This test verifies that the phone is in silent mode if configured in settings.
     * It also ensures that the previous state is restored after the activity has been stopped.
     */
    @Test
    public void silencingRestoring() {
        Fragment presenter = Presenter.newInstance(RemoteControl.CLIENT_PROTOCOL_VERSION);
        ActivityController activityController = Robolectric.buildActivity(
                PresenterTestDummyActivity.class);
        ((PresenterTestDummyActivity) activityController.get()).setFragment(presenter);
        
        Activity activity = (Activity) activityController.get();
        
        Settings settings = new Settings(activity);
        settings.silenceDuringPresentation(true);

        ((AudioManager) activity.getSystemService(AUDIO_SERVICE))
                .setRingerMode(AudioManager.RINGER_MODE_NORMAL);

        activityController.create().resume().visible();
        
        assertThat("Did not silence the device", 
                ((AudioManager) activity.getSystemService(AUDIO_SERVICE)).getRingerMode(),
                is(AudioManager.RINGER_MODE_SILENT));
        
        activityController.stop();

        assertThat("Did not restore previous ringing mode",
                ((AudioManager) activity.getSystemService(AUDIO_SERVICE)).getRingerMode(),
                is(AudioManager.RINGER_MODE_NORMAL));
        
    }
}
