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

package de.wohlfrom.presenter;

import android.app.Activity;
import android.media.AudioManager;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.android.controller.ActivityController;

import java.util.Objects;

import androidx.fragment.app.Fragment;
import de.wohlfrom.presenter.connectors.ProtocolVersion;
import de.wohlfrom.presenter.connectors.RemoteControl;

import static android.content.Context.AUDIO_SERVICE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Verifies that the presenter fragment works fine.
 * Needs to be executed with Android M support or newer, or deprecated onAttach method
 * will be used within presenter fragment.
 */
@RunWith(RobolectricTestRunner.class)
public class PresenterTest {

    /**
     * Verify that all buttons required for support of protocol version 1 are working.
     */
    @Test
    public void checkProtocolVersion1() {
        Fragment presenter = Presenter.newInstance(new ProtocolVersion(1, 1));
        ActivityController<PresenterTestDummyActivity> activityController =
                Robolectric.buildActivity(PresenterTestDummyActivity.class);
        activityController.get().setFragment(presenter);
        activityController.create().resume().visible();
        
        assertThat("Did not find 'next slide' button",
                Objects.requireNonNull(presenter.getView()).findViewById(R.id.next_slide),
                is(notNullValue()));

        presenter.getView().findViewById(R.id.next_slide).performClick();
        assertThat("Did not click on 'next slide' button",
                activityController.get().isNextSlidePressed());

        assertThat("Did not find 'prev slide' button",
                presenter.getView().findViewById(R.id.prev_slide),
                is(notNullValue()));

        presenter.getView().findViewById(R.id.prev_slide).performClick();
        assertThat("Did not click on 'prev slide' button",
                activityController.get().isPrevSlidePressed());
    }

    /**
     * Verify that all buttons required for support of protocol version 2 are working.
     */
    @Test
    public void checkProtocolVersion2() {
        Fragment presenter = Presenter.newInstance(new ProtocolVersion(2, 2));
        ActivityController<PresenterTestDummyActivity> activityController =
                Robolectric.buildActivity(PresenterTestDummyActivity.class);
        activityController.get().setFragment(presenter);
        activityController.create().resume().visible();

        assertThat("Did not find 'start presentation' button",
                Objects.requireNonNull(presenter.getView()).findViewById(R.id.start_presentation),
                is(notNullValue()));

        presenter.getView().findViewById(R.id.start_presentation).performClick();
        assertThat("Did not click on 'start presentation' button",
                activityController.get().isStartPresentationPressed());

        assertThat("Did not find 'stop presentation' button",
                presenter.getView().findViewById(R.id.stop_presentation),
                is(notNullValue()));

        presenter.getView().findViewById(R.id.stop_presentation).performClick();
        assertThat("Did not click on 'stop presentation' button",
                activityController.get().isStopPresentationPressed());
    }

    /**
     * This test verifies that the phone is in silent mode if configured in settings.
     * It also ensures that the previous state is restored after the activity has been stopped.
     */
    @Test
    public void silencingRestoring() {
        Fragment presenter = Presenter.newInstance(RemoteControl.CLIENT_PROTOCOL_VERSION);
        ActivityController<PresenterTestDummyActivity> activityController =
                Robolectric.buildActivity(PresenterTestDummyActivity.class);
        activityController.get().setFragment(presenter);
        
        Activity activity = activityController.get();
        
        Settings settings = new Settings(activity);
        settings.silenceDuringPresentation(true);

        ((AudioManager)activity.getSystemService(AUDIO_SERVICE))
                .setRingerMode(AudioManager.RINGER_MODE_NORMAL);

        activityController.create().resume().visible();
        
        assertThat("Did not silence the device", 
                ((AudioManager)activity.getSystemService(AUDIO_SERVICE)).getRingerMode(),
                is(AudioManager.RINGER_MODE_SILENT));
        
        activityController.stop();

        assertThat("Did not restore previous ringing mode",
                ((AudioManager)activity.getSystemService(AUDIO_SERVICE)).getRingerMode(),
                is(AudioManager.RINGER_MODE_NORMAL));
        
    }
}
