package de.wohlfrom.presenter;

import android.support.test.filters.SmallTest;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Basic verification for about activity interactions.
 */
@RunWith(AndroidJUnit4.class)
public class AboutActivityTest {

    /**
     * The rule to interact with the about activity.
     */
    @Rule
    public ActivityTestRule<AboutActivity> aboutActivityRule
            = new ActivityTestRule<>(AboutActivity.class);

    /**
     * Test that the about activity can be started at all.
     */
    @Test
    @SmallTest
    public void instantiateActivity() {
        assertThat(aboutActivityRule.getActivity(), is(notNullValue()));
    }
}
