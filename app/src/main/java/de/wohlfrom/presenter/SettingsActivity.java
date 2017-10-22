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

import android.app.Activity;
import android.os.Bundle;
import android.widget.Switch;

/**
 * The settings activity can be used to change the presenter settings.
 */
public class SettingsActivity extends Activity {

    // Our settings object
    private Settings mSettings;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mSettings = new Settings(this);

        ((Switch) findViewById(R.id.silenceDuringPresentation))
                .setChecked(mSettings.silenceDuringPresentation());

        ((Switch) findViewById(R.id.useVolumeKeysForNavigation))
                .setChecked(mSettings.useVolumeKeysForNavigation());

    }

    @Override
    protected void onStop(){
        super.onStop();

        mSettings.silenceDuringPresentation(
                ((Switch) findViewById(R.id.silenceDuringPresentation)).isChecked());

        mSettings.useVolumeKeysForNavigation(
                ((Switch) findViewById(R.id.useVolumeKeysForNavigation)).isChecked());
    }

}
