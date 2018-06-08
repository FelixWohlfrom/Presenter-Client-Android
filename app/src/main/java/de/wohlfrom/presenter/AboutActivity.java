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
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

import de.wohlfrom.presenter.connectors.RemoteControl;

/**
 * Shows information about our app.
 */
public class AboutActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);

        setTitle(getString(R.string.about, getString(R.string.app_name)));

        ((TextView)findViewById(R.id.appVersionView)).setText(
                getString(R.string.version, BuildConfig.VERSION_NAME));

        ((TextView)findViewById(R.id.protocol_version)).setText(
                Html.fromHtml(getString(R.string.protocol_version,
                        RemoteControl.CLIENT_PROTOCOL_VERSION.getMinVersion(),
                        RemoteControl.CLIENT_PROTOCOL_VERSION.getMaxVersion())));

        // Make links clickable
        ((TextView)findViewById(R.id.license)).setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView)findViewById(R.id.howto)).setMovementMethod(LinkMovementMethod.getInstance());
        ((TextView)findViewById(R.id.protocol_version))
                .setMovementMethod(LinkMovementMethod.getInstance());
    }
}
