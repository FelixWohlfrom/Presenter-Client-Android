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
