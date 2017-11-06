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

package de.wohlfrom.presenter.connectors.bluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.widget.Toast;

import de.wohlfrom.presenter.Presenter;
import de.wohlfrom.presenter.R;
import de.wohlfrom.presenter.Settings;

/**
 * The bluetooth connector activity is used to handle the complete presenter control using
 * a bluetooth connection.
 * It shows the bluetooth device selector and afterwards the presenter fragment.
 */
public class BluetoothConnector extends Activity
        implements DeviceSelector.DeviceListResultListener,
        Presenter.PresenterListener {

    /**
     * This request code is used to verify that the activity result is really our request to
     * enable bt.
     */
    private static final int REQUEST_ENABLE_BT = 1;

    /**
     * Local Bluetooth adapter.
     */
    private BluetoothAdapter mBluetoothAdapter = null;

    /**
     * Member object for the presenter control service.
     */
    private BluetoothPresenterControl mPresenterControl = null;

    /**
     * Stores if the presenter fragment is visible or not.
     */
    private boolean mPresenterVisible = false;

    /**
     * The settings instance
     */
    private Settings mSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mSettings = new Settings(this);

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.bluetooth_not_available, Toast.LENGTH_LONG).show();
            this.finish();
            return;
        }

        //The BroadcastReceiver that listens for bluetooth broadcasts
        BroadcastReceiver mReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (BluetoothDevice.ACTION_ACL_DISCONNECTED.equals(action)) {
                    //Device has disconnected
                    Toast.makeText(BluetoothConnector.this, R.string.bluetooth_required_leaving,
                            Toast.LENGTH_LONG).show();
                    BluetoothConnector.this.finish();
                }
            }
        };

        IntentFilter disconnectFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        this.registerReceiver(mReceiver, disconnectFilter);

        setContentView(R.layout.activity_bluetooth_connector);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mPresenterControl != null) {
            mPresenterControl.stop();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        // FIXME Check why we switch back to main menu after a few seconds after connection
        // to server is lost

        mPresenterVisible = false;

        // If BT is not on, request that it be enabled.
        if (!mBluetoothAdapter.isEnabled()) {
            setTitle("");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);

        } else if (mPresenterControl == null) {
            // Initialize the BluetoothPresenterControl to perform bluetooth connections
            mPresenterControl = new BluetoothPresenterControl(mHandler);
        }

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mPresenterControl != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mPresenterControl.getState() == BluetoothPresenterControl.ServiceState.NONE) {
                // initialize presenter control service
                mPresenterControl.start();
            }

            if (mPresenterControl.getState() != BluetoothPresenterControl.ServiceState.CONNECTED) {
                // show device selector
                setTitle(R.string.title_device_selector);
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                Fragment fragment = new DeviceSelector();
                transaction.replace(R.id.connector_content, fragment);
                transaction.commit();

            } else {
                // show presenter fragment
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                Fragment fragment = new Presenter();
                transaction.replace(R.id.connector_content, fragment);
                transaction.commit();

                mPresenterVisible = true;
            }
        }
    }

    /**
     * The handler reacts on bluetooth status changes (connect, disconnect).
     */
    @SuppressLint("HandlerLeak") // We don't leak any handlers here
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        if (msg.what == BluetoothPresenterControl.ServiceState.CONNECTED.ordinal()) {
            if (msg.getData().getBoolean(BluetoothPresenterControl.RESULT_VALUES[0])) {
                // If connection succeeded
                Toast.makeText(BluetoothConnector.this,
                        BluetoothConnector.this.getString(R.string.bluetooth_connected,
                                msg.getData().getString(
                                        BluetoothPresenterControl.RESULT_VALUES[1])),
                        Toast.LENGTH_SHORT).show();

                // show presenter fragment
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                Fragment fragment = new Presenter();
                transaction.replace(R.id.connector_content, fragment);
                transaction.addToBackStack(null);
                transaction.commit();

                mPresenterVisible = true;

            } else {
                Toast.makeText(BluetoothConnector.this,
                        getString(R.string.bluetooth_not_connected),
                        Toast.LENGTH_SHORT).show();
            }

        } else if (msg.what == BluetoothPresenterControl.ServiceState.NONE.ordinal()) {
            Toast.makeText(BluetoothConnector.this,
                    BluetoothConnector.this.getString(R.string.connection_lost),
                    Toast.LENGTH_LONG).show();

            if (getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStack();

                mPresenterVisible = false;
            }
        }
        }
    };

    /**
     * Handles answers of our request to enable bluetooth.
     *
     * @param requestCode The request code to identify our request.
     *                    Should always be REQUEST_ENABLE_BT
     * @param resultCode  The result code of the request
     * @param data        Additional data, unused.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_ENABLE_BT) {
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Show device selection fragment
                FragmentTransaction transaction = getFragmentManager().beginTransaction();
                Fragment fragment = new DeviceSelector();
                transaction.replace(R.id.connector_content, fragment);
                transaction.commit();

            } else {
                // User did not enable Bluetooth or an error occurred
                Toast.makeText(this, R.string.bluetooth_required_leaving,
                        Toast.LENGTH_LONG).show();
                this.finish();
            }
        }
    }

    @Override
    public void onDeviceSelected(String address) {
        // Get the BluetoothDevice object
        BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        mPresenterControl.connect(device);
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
        super.onBackPressed();
    }

    @Override
    public void onPrevSlide() {
        mPresenterControl.write(Presenter.PREV_SLIDE.getBytes());
    }

    @Override
    public void onNextSlide() {
        mPresenterControl.write(Presenter.NEXT_SLIDE.getBytes());
    }
}
