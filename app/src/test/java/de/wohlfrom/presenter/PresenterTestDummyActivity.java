package de.wohlfrom.presenter;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.os.Bundle;

/**
 * This activity can be used to test the fragments used for bluetooth connection.
 * It implements the required listeners and provides getters for the relevant values set by these 
 * listeners.
 * Use the following snippet to run an activity with a given fragment:
 * <code>
 *     ActivityController activityController = 
 *          Robolectric.buildActivity(PresenterTestDummyActivity.class);
 *     ((PresenterTestDummyActivity) activityController.get()).setFragment([yourFragment]);
 *     activityController.create().resume();
 * </code>
 */
public class PresenterTestDummyActivity extends Activity
        implements Presenter.PresenterListener {

    private Fragment fragment;

    /**
     * Sets the fragment to display to the given fragment.
     *
     * @param fragment The fragment to display on creating the activity.
     */
    public void setFragment(Fragment fragment) {
        this.fragment = fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bluetooth_connector);
    }

    @Override
    public void onResume() {
        super.onResume();
        FragmentTransaction transaction = getFragmentManager().beginTransaction();
        transaction.replace(R.id.connector_content, fragment);
        transaction.commit();
    }

    // Variables to store if the events have been triggered
    private boolean prevSlidePressed;
    private boolean nextSlidePressed;
    private boolean startPresentationPressed;
    private boolean stopPresentationPressed;

    /**
     * Returns true if the prev slide event has been triggered.
     * Resets it afterwards.
     * 
     * @return If prev slide event has been triggered.
     */
    public boolean isPrevSlidePressed() {
        boolean res = prevSlidePressed;
        prevSlidePressed = false;
        return res;
    }

    /**
     * Returns true if the next slide event has been triggered.
     * Resets it afterwards.
     *
     * @return If next slide event has been triggered.
     */
    public boolean isNextSlidePressed() {
        boolean res = nextSlidePressed;
        nextSlidePressed = false;
        return res;
    }

    /**
     * Returns true if the start presentation event has been triggered.
     * Resets it afterwards.
     *
     * @return If start presentation event has been triggered.
     */
    public boolean isStartPresentationPressed() {
        boolean res = startPresentationPressed;
        startPresentationPressed = false;
        return res;
    }

    /**
     * Returns true if the stop presentation event has been triggered.
     * Resets it afterwards.
     *
     * @return If stop presentation event has been triggered.
     */
    public boolean isStopPresentationPressed() {
        boolean res = stopPresentationPressed;
        stopPresentationPressed = false;
        return res;
    }

    @Override
    public void onPrevSlide() {
        prevSlidePressed = true;
    }

    @Override
    public void onNextSlide() {
        nextSlidePressed = true;
    }

    @Override
    public void onStartPresentation() {
        startPresentationPressed = true;
    }

    @Override
    public void onStopPresentation() {
        stopPresentationPressed = true;
    }
}
