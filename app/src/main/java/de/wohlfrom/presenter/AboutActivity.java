package de.wohlfrom.presenter;

import android.app.Activity;
import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.widget.TextView;

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

        ((TextView)findViewById(R.id.authorView)).setText(getString(R.string.copyright));

        // Make links in to license clickable
        ((TextView)findViewById(R.id.license)).setMovementMethod(LinkMovementMethod.getInstance());
    }
}
