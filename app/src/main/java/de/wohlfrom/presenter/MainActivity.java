package de.wohlfrom.presenter;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;

import de.wohlfrom.presenter.bluetooth.connectors.BluetoothConnector;

/**
 * The main activity that shows the main menu.
 */
public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.connect_via_bluetooth).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this,
                                BluetoothConnector.class);
                        startActivity(intent);
                    }
                });

        findViewById(R.id.settings).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this,
                                SettingsActivity.class);
                        startActivity(intent);
                    }
                });

        ((Button)findViewById(R.id.about)).setText(
                getString(R.string.about, getString(R.string.app_name)));
        findViewById(R.id.about).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Intent intent = new Intent(MainActivity.this,
                                AboutActivity.class);
                        startActivity(intent);
                    }
                });
    }
}
