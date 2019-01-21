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

package de.wohlfrom.presenter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.ArrayList;
import java.util.List;

import de.wohlfrom.presenter.connectors.Command;
import de.wohlfrom.presenter.connectors.ProtocolVersion;

import static android.content.Context.AUDIO_SERVICE;

/**
 * This fragment displays the main presenter. Depending on the supported protocol version, it
 * displays for example buttons to start and stop a presentation or to switch to the next or
 * previous slide.
 */
public class Presenter extends Fragment {
    /**
     * Audio management
     */
    private Settings mSettings;
    private AudioManager mAudioManager;
    private int mPreviousAudioRingerMode;

    /**
     * The protocol version supported by this fragment.
     */
    private ProtocolVersion mActiveProtocolVersion;

    /**
     * Some older devices needs a small delay between UI widget updates
     * and a change of the status and navigation bar.
     */
    private static final int UI_ANIMATION_DELAY = 300;
    private final Handler mHideHandler = new Handler();
    private View mContentView;
    private final Runnable mHideSystemUi = new Runnable() {
        @Override
        public void run() {
            // Delayed removal of status and navigation bar

            // Note that some of these constants are new as of API 16 (Jelly Bean)
            // and API 19 (KitKat). It is safe to use them, as they are inlined
            // at compile-time and do nothing on earlier devices.
            mContentView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LOW_PROFILE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
    };

    /**
     * Listener interface for presenter events.
     */
    public interface PresenterListener {
        /**
         * Called if the server should switch to the previous slide.
         */
        void onPrevSlide();

        /**
         * Called if the server should switch to the next slide.
         */
        void onNextSlide();

        /**
         * Called if the server should start the presentation.
         */
        void onStartPresentation();

        /**
         * Called if the server should stop the presentation.
         */
        void onStopPresentation();
    }

    /**
     * Creates a new instance of presenter class. The displayed control elements depend on the given
     * protocol version.
     *
     * @param supportedProtocolVersion The protocol version that needs to be supported by this
     *                                 presenter fragment
     * @return The presenter fragment instance
     */
    public static Presenter newInstance(@NonNull ProtocolVersion supportedProtocolVersion) {
        Presenter presenter = new Presenter();
        Bundle bundle = new Bundle();
        bundle.putSerializable("minVersion", supportedProtocolVersion.getMinVersion());
        bundle.putSerializable("maxVersion", supportedProtocolVersion.getMaxVersion());
        presenter.setArguments(bundle);

        return presenter;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        if (getArguments() != null) {
            int minVersion = getArguments().getInt("minVersion");
            int maxVersion = getArguments().getInt("maxVersion");

            mActiveProtocolVersion = new ProtocolVersion(minVersion, maxVersion);
        }

        return inflater.inflate(R.layout.fragment_presenter, container, false);
    }

    @Override
    public void onResume() {
        super.onResume();

        // The pager widget, which handles animation and allows swiping horizontally to access
        // previous and next wizard steps.
        ViewPager mPager = this.getActivity().findViewById(R.id.presenter);
        // The pager adapter, which provides the pages to the view pager widget.
        PagerAdapter mPagerAdapter = new ScreenSlidePagerAdapter(
                this.getChildFragmentManager(), mActiveProtocolVersion);
        mPager.setAdapter(mPagerAdapter);

        // Hide the system ui with a short delay
        mContentView = getActivity().findViewById(R.id.presenter);
        mHideHandler.postDelayed(mHideSystemUi, UI_ANIMATION_DELAY);

        mSettings = new Settings(getActivity());

        // Silence audio during presentation. Store old audio state to reset it afterwards.
        if (mSettings.silenceDuringPresentation()) {
            mAudioManager = (AudioManager) getActivity().getSystemService(AUDIO_SERVICE);
            if (mAudioManager != null) {
                mPreviousAudioRingerMode = mAudioManager.getRingerMode();

                mAudioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            }
        }
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mSettings.silenceDuringPresentation()) {
            mAudioManager.setRingerMode(mPreviousAudioRingerMode);
        }
    }

    /**
     * This slider pager adapter contains the different pages containing the buttons of the
     * presenter fragment.
     */
    private class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        final List<Fragment> activeFragmentList;

        ScreenSlidePagerAdapter(FragmentManager fragmentManager,
                                ProtocolVersion activeProtocolVersion) {
            super(fragmentManager);

            activeFragmentList = new ArrayList<>();

            if ((activeProtocolVersion.getMaxVersion() >= Command.NEXT_SLIDE.getMinVersion() &&
                    activeProtocolVersion.getMinVersion() <= Command.NEXT_SLIDE.getMaxVersion()) &&
                    activeProtocolVersion.getMaxVersion() >= Command.PREV_SLIDE.getMinVersion() &&
                    activeProtocolVersion.getMinVersion() <= Command.PREV_SLIDE.getMaxVersion()) {
                activeFragmentList.add(
                        PresenterPage.newInstance(R.layout.fragment_presenter_nextprev));
            }

            if (activeProtocolVersion.getMaxVersion()
                    >= Command.START_PRESENTATION.getMinVersion() &&
                    activeProtocolVersion.getMinVersion()
                            <= Command.START_PRESENTATION.getMaxVersion() &&
                    activeProtocolVersion.getMaxVersion()
                            >= Command.STOP_PRESENTATION.getMinVersion() &&
                    activeProtocolVersion.getMinVersion()
                            <= Command.STOP_PRESENTATION.getMaxVersion()) {
                activeFragmentList.add(
                        PresenterPage.newInstance(R.layout.fragment_presenter_startstop));
            }
        }

        @Override
        public Fragment getItem(int position) {
            return activeFragmentList.get(position);
        }

        @Override
        public int getCount() {
            return activeFragmentList.size();
        }
    }

    /**
     * A single page containing the presenter buttons.
     */
    public static class PresenterPage extends Fragment {
        /**
         * The listener for presenter events.
         */
        private PresenterListener mListener;

        /**
         * Key to insert the layout id into the mapping of a Bundle.
         */
        private static final String LAYOUT_ID = "layoutId";

        /**
         * The id of the layout contained by this page.
         */
        private int mLayoutId;

        /**
         * Creates a new page for a given layout.
         *
         * @param layoutId The resource id of the layout to add
         * @return A new page instance
         */
        static PresenterPage newInstance(int layoutId) {
            PresenterPage fragment = new PresenterPage();

            Bundle bundle = new Bundle();
            bundle.putInt(LAYOUT_ID, layoutId);
            fragment.setArguments(bundle);
            fragment.setRetainInstance(true);

            return fragment;
        }

        @Override
        public void onAttach(Context context) {
            super.onAttach(context);

            try {
                mListener = (PresenterListener) context;
            } catch (ClassCastException e) {
                throw new ClassCastException(context.toString()
                        + " must implement PresenterListener");
            }
        }

        @Override
        public void onDetach() {
            super.onDetach();
            mListener = null;
        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            if (this.getArguments() != null) {
                this.mLayoutId = this.getArguments().getInt(LAYOUT_ID);
            }
        }

        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            return inflater.inflate(mLayoutId, container, false);
        }

        @Override
        public void onResume() {
            super.onResume();

            Button nextSlideButton = getActivity().findViewById(R.id.next_slide);
            if (nextSlideButton != null) {
                nextSlideButton.setOnClickListener(view -> mListener.onNextSlide());
            }

            Button prevSlideButton = getActivity().findViewById(R.id.prev_slide);
            if (prevSlideButton != null) {
                prevSlideButton.setOnClickListener(view -> mListener.onPrevSlide());
            }

            Button startButton = getActivity().findViewById(R.id.start_presentation);
            if (startButton != null) {
                startButton.setOnClickListener(view -> mListener.onStartPresentation());
            }

            Button stopButton = getActivity().findViewById(R.id.stop_presentation);
            if (stopButton != null) {
                stopButton.setOnClickListener(view -> mListener.onStopPresentation());
            }
        }
    }
}
