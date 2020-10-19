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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.ParameterizedRobolectricTestRunner;
import org.robolectric.Robolectric;
import org.robolectric.android.controller.ActivityController;
import org.robolectric.util.Pair;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import androidx.fragment.app.Fragment;
import de.wohlfrom.presenter.connectors.ProtocolVersion;
import de.wohlfrom.presenter.connectors.RemoteControl;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * This test verifies that instantiation of presenter fragment works fine.
 * It will instantiate all possible combinations of supported protocol versions.
 */
@RunWith(ParameterizedRobolectricTestRunner.class)
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
    
    private final int minValue;
    private final int maxValue;

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
        ActivityController<PresenterTestDummyActivity> activityController =
                Robolectric.buildActivity(PresenterTestDummyActivity.class);
        activityController.get().setFragment(presenter);
        activityController.create().start().resume().visible().stop().destroy();
    }
}
