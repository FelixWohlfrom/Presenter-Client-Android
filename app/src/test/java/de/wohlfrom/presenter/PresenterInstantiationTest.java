package de.wohlfrom.presenter;

import android.app.Fragment;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.annotation.Config;
import org.robolectric.util.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import de.wohlfrom.presenter.connectors.ProtocolVersion;
import de.wohlfrom.presenter.connectors.RemoteControl;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

/**
 * This test verifies that instantiation of presenter fragment works fine.
 * It will instantiate all possible combinations of supported protocol versions.
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
@Config(manifest = "src/main/AndroidManifest.xml")
public class PresenterInstantiationTest {

    /**
     * Generator for the different parameter combinations
     * @return A list of parameter combinations
     */
    @ParameterizedRobolectricTestRunner.Parameters(name = "ProtocolVersion[{0}, {1}]")
    public static Collection<Object[]> data() {
        List<Pair<Integer, Integer>> result = new LinkedList<>();
        for (int i = RemoteControl.CLIENT_PROTOCOL_VERSION.getMinVersion() - 1;
             i <= RemoteControl.CLIENT_PROTOCOL_VERSION.getMaxVersion() + 1; i++) {
            for (int j = RemoteControl.CLIENT_PROTOCOL_VERSION.getMinVersion() - 1;
                 j <= RemoteControl.CLIENT_PROTOCOL_VERSION.getMaxVersion() + 1; j++) {
                result.add(new Pair<>(i, j));
            }
        }
        
        Object[][] coll = new Object[result.size()][2];
        for (int i = 0; i < result.size(); i++) {
            coll[i][0] = result.get(i).first;
            coll[i][1] = result.get(i).second;
        }
        
        return Arrays.asList(coll);
    }
    
    private int minValue;
    private int maxValue;

    /**
     * Constructor, creates parametrized test.
     * 
     * @param minValue The minimum protocol version that is supported by the presenter
     * @param maxValue The maximum protocol version that is supported by the presenter
     */
    public PresenterInstantiationTest(int minValue, int maxValue) {
        this.minValue = minValue;
        this.maxValue = maxValue;
    }

    /**
     * This test will verify that the fragment can be instantiated properly with all
     * combinations of supported protocol versions.
     */
    @Test
    public void instantiateFragment() {
        Fragment presenter = Presenter.newInstance(new ProtocolVersion(minValue, maxValue));  
        assertThat(presenter, is(notNullValue()));
    }

    /**
     * This test verifies that the fragment can be added to activity with all combinations of
     * supported protocol versions.
     */
    @Test
    public void fragmentLifecycle() {
        Fragment presenter = Presenter.newInstance(new ProtocolVersion(minValue, maxValue));
        ActivityController activityController = Robolectric.buildActivity(
                PresenterTestDummyActivity.class);
        ((PresenterTestDummyActivity) activityController.get()).setFragment(presenter);
        activityController.create().resume().start().stop().destroy();
    }
}
