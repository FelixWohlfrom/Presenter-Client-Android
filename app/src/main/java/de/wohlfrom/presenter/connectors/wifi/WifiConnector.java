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

package de.wohlfrom.presenter.connectors.wifi;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.Toast;

import de.wohlfrom.presenter.Connecting;
import de.wohlfrom.presenter.Presenter;
import de.wohlfrom.presenter.R;
import de.wohlfrom.presenter.Settings;
import de.wohlfrom.presenter.connectors.Command;
import de.wohlfrom.presenter.connectors.RemoteControl;

/**
 * The wifi connector activity is used to handle the complete presenter control using
 * a wifi connection.
 * It shows the network service selector and afterwards the presenter fragment.
 */
public class WifiConnector extends Activity
        implements DeviceSelector.DeviceListResultListener,
        Presenter.PresenterListener {

    /**
     * This request code is used to verify that the activity result is really our request to
     * enable wifi.
     */
    private static final int REQUEST_ENABLE_WIFI = 1;

    /**
     * Local connectivity manager.
     */
    private ConnectivityManager mConnectivityManager = null;

    /**
     * Member object for the presenter control service.
     */
    private WifiPresenterControl mPresenterControl = null;

    /**
     * Stores if the presenter fragment is visible or not.
     */
    private boolean mPresenterVisible = false;

    /**
     * Stores if the current visibility state of the wifi connector.
     */
    private boolean mWifiConnectorVisible = false;

    /**
     * The settings instance.
     */
    private Settings mSettings;

    /**
     * The BroadcastReceiver that listens for wifi broadcasts
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(action)
                    && intent.hasExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY)
                    && intent.getBooleanExtra(ConnectivityManager.EXTRA_NO_CONNECTIVITY, true)) {
                //Device has disconnected
                Toast.makeText(WifiConnector.this, R.string.wifi_required_leaving,
                        Toast.LENGTH_LONG).show();
                WifiConnector.this.finish();
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = new Settings(this);

        // Get connectivity manager
        mConnectivityManager = 
                (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);

        // If the adapter is null, then wifi is not supported
        if (mConnectivityManager == null) {
            Toast.makeText(this, R.string.wifi_not_available, Toast.LENGTH_LONG).show();
            this.finish();
            return;
        }
        
        IntentFilter disconnectFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        this.registerReceiver(mReceiver, disconnectFilter);

        setContentView(R.layout.activity_wifi_connector);
    }

    @Override
    public void onDestroy() {
        this.unregisterReceiver(mReceiver);

        super.onDestroy();
        if (mPresenterControl != null) {
            mPresenterControl.stop();
        }
    }

    @Override
    public void onPause() {
        mWifiConnectorVisible = false;

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();
        mPresenterVisible = false;
        mWifiConnectorVisible = true;

        // If wifi is not on, request that it be enabled.
        NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
        if (activeNetwork == null || !activeNetwork.isConnectedOrConnecting() ||
                activeNetwork.getType() != ConnectivityManager.TYPE_WIFI) {
            setTitle("");
            Intent enableIntent = new Intent(android.provider.Settings.ACTION_WIFI_SETTINGS);
            startActivityForResult(enableIntent, REQUEST_ENABLE_WIFI);

        } else if (mPresenterControl == null) {
            // Initialize the WifiPresenterControl to perform wifi connections
            mPresenterControl = new WifiPresenterControl(mHandler);
        }

        // Performing this check in onResume() covers the case in which wifi was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mPresenterControl != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mPresenterControl.getState() == RemoteControl.ServiceState.NONE) {
                // initialize presenter control service
                mPresenterControl.start();
            }

            switch (mPresenterControl.getState()) {
                case CONNECTED: {
                    // show presenter fragment
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    Fragment fragment = Presenter.newInstance(
                            mPresenterControl.getActiveProtocolVersion());
                    transaction.replace(R.id.connector_content, fragment);
                    transaction.addToBackStack(null);
                    transaction.commit();

                    mPresenterVisible = true;
                    break;
                }
                case CONNECTING: {
                    // show "connecting" fragment
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    Fragment fragment = new Connecting();
                    transaction.replace(R.id.connector_content, fragment);
                    transaction.addToBackStack(null);
                    transaction.commit();

                    break;
                }
                default: {
                    // show device selector
                    setTitle(R.string.title_device_selector);
                    FragmentTransaction transaction = getFragmentManager().beginTransaction();
                    Fragment fragment = new DeviceSelector();
                    transaction.replace(R.id.connector_content, fragment);
                    transaction.commit();
                    break;
                }
            }
        }
    }

    /**
     * The handler reacts on status changes of our service.
     */
    @SuppressLint("HandlerLeak") // We don't leak any handlers here
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == RemoteControl.ServiceState.CONNECTED.ordinal()) {
                // If connection succeeded
                Toast.makeText(WifiConnector.this,
                        WifiConnector.this.getString(R.string.wifi_connected,
                                msg.getData().getString(WifiPresenterControl.RESULT_VALUES[0])),
                        Toast.LENGTH_SHORT).show();

                // Remove "connecting" fragment
                if (getFragmentManager().getBackStackEntryCount() > 0) {
                    getFragmentManager().popBackStack();
                }

                // show presenter fragment
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                Fragment fragment = Presenter.newInstance(
                        mPresenterControl.getActiveProtocolVersion());
                transaction.replace(R.id.connector_content, fragment);
                transaction.addToBackStack(null);
                transaction.commit();

                mPresenterVisible = true;
                return;

            } else if (msg.what == RemoteControl.ServiceState.CONNECTING.ordinal()) {
                // show "connecting" fragment
                setTitle(R.string.connecting_to_service);
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                Fragment fragment = new Connecting();
                transaction.replace(R.id.connector_content, fragment);
                transaction.addToBackStack(null);
                transaction.commit();

                return;

            } else if (msg.what == RemoteControl.ServiceState.ERROR.ordinal()) {
                RemoteControl.ERROR_TYPES error_type = RemoteControl.ERROR_TYPES.valueOf(
                        msg.getData().getString(RemoteControl.RESULT_VALUES[1]));

                String errorMessage = "";

                switch (error_type) {
                    case NO_CONNECTION:
                        errorMessage = getString(R.string.wifi_not_connected);
                        break;
                    case VERSION:
                        errorMessage = getString(R.string.incompatible_server_version);
                        break;
                    case PARSING:
                        errorMessage = getString(R.string.parsing_error);
                        break;
                }

                Toast.makeText(WifiConnector.this, errorMessage, Toast.LENGTH_LONG).show();

            } else if (msg.what == RemoteControl.ServiceState.NONE.ordinal()) {
                Toast.makeText(WifiConnector.this,
                        WifiConnector.this.getString(R.string.connection_lost),
                        Toast.LENGTH_LONG).show();
            }

            if (getFragmentManager().getBackStackEntryCount() > 0) {
                if (mWifiConnectorVisible) {
                    getFragmentManager().popBackStack();
                }

                mPresenterVisible = false;
            }

            // Set title depending on visible fragments (for device selector and manual connection)
            if (WifiConnector.this.findViewById(R.id.button_manual_connection) != null) {
                setTitle(R.string.title_device_selector);
            } else {
                setTitle(R.string.manual_connection);
            }
        }
    };

    /**
     * Handles answers of our request to enable wifi.
     *
     * @param requestCode The request code to identify our request.
     *                    Should always be REQUEST_ENABLE_WIFI
     * @param resultCode  The result code of the request
     * @param data        Additional data, unused.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_WIFI) {
            NetworkInfo activeNetwork = mConnectivityManager.getActiveNetworkInfo();
            if (activeNetwork == null || !activeNetwork.isConnectedOrConnecting() ||
                    activeNetwork.getType() != ConnectivityManager.TYPE_WIFI) {
                // User did not enable wifi
                Toast.makeText(this, R.string.wifi_required_leaving,
                        Toast.LENGTH_LONG).show();
                this.finish();
            }
        }
    }

    @Override
    public void onDeviceSelected(String hostname, String address) {
        // Attempt to connect to the device
        mPresenterControl.connect(hostname, address);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Ignore volume key events if volume keys are used for navigation and
        // presenter fragment is active
        return (mSettings.useVolumeKeysForNavigation() && mPresenterVisible
                    && ((keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                        || (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)))
                || super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        // Handle volume key usage for navigation
        if (mSettings.useVolumeKeysForNavigation() && mPresenterVisible) {
            if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                onNextSlide();
                return true;
            } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                onPrevSlide();
                return true;
            }
        }
        return super.onKeyUp(keyCode, event);
    }

    @Override
    public void onBackPressed() {
        mPresenterVisible = false;
        mPresenterControl.disconnect();
        setTitle(R.string.title_device_selector);
        super.onBackPressed();
    }

    @Override
    public void onPrevSlide() {
        mPresenterControl.sendCommand(Command.PREV_SLIDE);
    }

    @Override
    public void onNextSlide() {
        mPresenterControl.sendCommand(Command.NEXT_SLIDE);
    }

    @Override
    public void onStartPresentation() {
        mPresenterControl.sendCommand(Command.START_PRESENTATION);
    }

    @Override
    public void onStopPresentation() {
        mPresenterControl.sendCommand(Command.STOP_PRESENTATION);
    }
}
