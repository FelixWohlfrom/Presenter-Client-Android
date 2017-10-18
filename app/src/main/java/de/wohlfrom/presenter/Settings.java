package de.wohlfrom.presenter;

import android.app.Activity;
import android.content.SharedPreferences;

import static android.content.Context.MODE_PRIVATE;

/**
 * This class is used to store and read settings for our application in a central way.
 */
public class Settings {

    // Our preference object
    private SharedPreferences mPreferences;

    // The settings keys
    private static final String SILENCE_DURING_PRESENTATION_SETTING = "silenceDuringPresentation";
    private static final String USE_VOLUME_KEY_SETTING = "useVolumeKeys";

    /**
     * Initialize settings for a given activity.
     *
     * @param activity The activity to initialize the settings with
     */
    public Settings(Activity activity) {
        mPreferences = activity.getSharedPreferences("Settings", MODE_PRIVATE);
    }

    /**
     * If this value is set, the device should be silenced while presenter is connected to the
     * server.
     *
     * @return If the device should be silenced. Defaults to false.
     */
    public boolean silenceDuringPresentation() {
        return mPreferences.getBoolean(SILENCE_DURING_PRESENTATION_SETTING, false);
    }

    /**
     * If this value is set, the volume keys can be used for navigation.
     *
     * @return If volume keys can be used for navigation. Defaults to true.
     */
    public boolean useVolumeKeysForNavigation() {
        return mPreferences.getBoolean(USE_VOLUME_KEY_SETTING, true);
    }

    /**
     * Set if the device should be silenced while presenter is connected to the server.
     *
     * @param value If the device should be silenced during presentation.
     */
    void silenceDuringPresentation(boolean value) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(SILENCE_DURING_PRESENTATION_SETTING, value);
        editor.apply();
    }

    /**
     * Set if the volume keys can be used for navigation.
     *
     * @param value If the volume keys can be used for navigation.
     */
    void useVolumeKeysForNavigation(boolean value) {
        SharedPreferences.Editor editor = mPreferences.edit();
        editor.putBoolean(USE_VOLUME_KEY_SETTING, value);
        editor.apply();
    }
}
