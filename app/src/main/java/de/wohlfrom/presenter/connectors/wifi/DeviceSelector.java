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
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import de.wohlfrom.presenter.R;
import de.wohlfrom.presenter.connectors.RemoteControl;

/**
 * This Activity appears as a dialog. It lists any paired devices and
 * devices detected in the area after discovery. When a device is chosen
 * by the user, the ip address of the device is sent back to the parent
 * Activity in the result Intent.
 */
public class DeviceSelector extends Fragment {
    /**
     * Tag for Log
     */
    private static final String TAG = "DeviceSelector";

    /**
     * The port on which broadcasted messages are sent from the server
     */
    public static final int DEVICE_DISCOVERY_PORT = 43154;

    /**
     * Timeout in msec after which broadcasts are removed from view if no update was received.
     */
    private static final int BROADCAST_SHOW_TIMEOUT = 10000;

    /**
     * Broadcasted devices. A map of ip addresses and timestamps of last update.
     */
    private Map<String, Long> mBroadcastDevices;

    /**
     * Broadcasted devices on the ui.
     */
    private ArrayAdapter<String> mBroadcastDeviceAdapter;

    /**
     * Background threads to manage broadcast list
     */
    private BroadcastReceiverThread mBroadcastReceiverThread;
    private Timer mBroadcastCleanupThread;

    /**
     * Some constants to be used for thread notification.
     */
    public static final int ADDRESS_FOUND = 1;
    public static final int ADDRESS_REMOVED = 2;

    /**
     * Return values of this fragment
     */
    interface DeviceListResultListener {
        /**
         * This method is called once a device was selected from device list
         *
         * @param hostname The hostname of the selected device
         * @param address The address of the selected device
         */
        void onDeviceSelected(String hostname, String address);
    }

    /**
     * The listener for return values.
     */
    private DeviceListResultListener mListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wifi_device_selector, container, false);
    }

    // Required for backwards compatibility
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        initListener(activity);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        initListener(context);
    }

    /**
     * Initializes the device list result listener.
     *
     * @param context The context that should be used as listener. Needs to implement
     *                {@link DeviceListResultListener}
     */
    private void initListener(Context context) {
        try {
            mListener = (DeviceListResultListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement DeviceListResultListener");
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // Initialize the button to connect manually to a device
        Button manualConnection = getActivity().findViewById(R.id.button_manual_connection);
        manualConnection.setOnClickListener(view -> {
            getActivity().setTitle(R.string.manual_connection);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            Fragment fragment = WifiManualInput.newInstance();
            transaction.replace(R.id.connector_content, fragment);
            transaction.addToBackStack(null);
            transaction.commit();
        });

        // Initialize array adapter for broadcasted devices
        mBroadcastDeviceAdapter = new ArrayAdapter<>(getActivity(), R.layout.device_name);
        ListView broadcastDevicesListView = getActivity().findViewById(R.id.broadcast_devices);
        broadcastDevicesListView.setAdapter(mBroadcastDeviceAdapter);
        broadcastDevicesListView.setOnItemClickListener(mBroadcastDeviceClickListener);

        // Initialize map of last update timestamps
        mBroadcastDevices = new HashMap<>();
        
        // Start broadcast reader thread, if not already running
        if (mBroadcastReceiverThread == null) {
            mBroadcastReceiverThread = new BroadcastReceiverThread(mHandler);
            mBroadcastReceiverThread.start();
        }
        
        // Start broadcast cleanup thread, if not already running
        if (mBroadcastCleanupThread == null) {
            mBroadcastCleanupThread = new Timer();
            mBroadcastCleanupThread.schedule(new BroadcastCleanupThread(mHandler), 0, 2000);
        }
    }

    @Override
    public void onDestroy() {
        mBroadcastReceiverThread.cancel();
        mBroadcastCleanupThread.cancel();
        super.onDestroy();
    }

    /**
     * The on-click listener for all devices in the ListViews.
     */
    private final AdapterView.OnItemClickListener mBroadcastDeviceClickListener
            = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
            String info = ((TextView) view).getText().toString();
            String hostname = info.substring(0, info.lastIndexOf('\n'));
            String address = info.substring(info.lastIndexOf('\n') + 1);

            mListener.onDeviceSelected(hostname, address);
        }
    };

    /**
     * This thread reads all broadcasts. Will emit a message if new broadcasts are received.
     */
    private class BroadcastReceiverThread extends Thread {
        final Handler mHandler;
        DatagramSocket mBroadcastSocket;
        
        /**
         * Creates the broadcast receiver thread.
         * 
         * @param handler  The handler that will handle new found devices
         */
        BroadcastReceiverThread(Handler handler) {
            mHandler = handler;
            try {
                mBroadcastSocket = 
                        new DatagramSocket(DEVICE_DISCOVERY_PORT, InetAddress.getByName("0.0.0.0"));
                mBroadcastSocket.setBroadcast(true);
            } catch (UnknownHostException | SocketException e) {
                Log.e(TAG, "Error while creating broadcast socket", e);
            }
        }

        /**
         * Receive broadcasts.
         */
        public void run() {
            setName("BroadcastReceiverThread");

            if (mBroadcastSocket == null) {
                return;
            }

            byte[] recvBuf = new byte[1024];
            DatagramPacket packet = new DatagramPacket(recvBuf, recvBuf.length);
            try {
                mBroadcastSocket.setSoTimeout(1000);
            } catch (SocketException e) {
                Log.e(TAG, "Could net set timeout for broadcast receiver. Aborting.", e);
                return;
            }
            
            while (!mBroadcastSocket.isClosed()) {
                try {
                    mBroadcastSocket.receive(packet);
                } catch (SocketTimeoutException e) {
                    // Continue after a timeout, this allows us to regularly check if the thread
                    // should be stopped.
                    continue;
                } catch (IOException e) {
                    if (!mBroadcastSocket.isClosed()) {
                        Log.e(TAG, "Could not receive data from broadcast socket. Aborting.", e);
                    }
                    return;
                }
                
                String message = new String(packet.getData()).trim();
                if (message.startsWith(RemoteControl.SERVICE_ID + "\n")) {
                    Message notification = mHandler.obtainMessage(ADDRESS_FOUND);
                    Bundle data = new Bundle();
                    data.putString("ip", packet.getAddress().getHostAddress());
                    data.putString("host", message.substring(message.indexOf('\n') + 1));
                    notification.setData(data);
                    notification.sendToTarget();
                }
            }
        }

        /**
         * Cancel broadcast reception.
         */
        void cancel() {
            if (mBroadcastSocket != null) {
                mBroadcastSocket.close();
            }
        }
    }

    /**
     * Will remove broadcasted devices from list after some time. Timeout is defined in 
     * {@link #BROADCAST_SHOW_TIMEOUT}.
     */
    private class BroadcastCleanupThread extends TimerTask {
        final Handler mHandler;

        /**
         * Creates the broadcast cleanup thread.
         *
         * @param handler The handler that will handle new found devices
         */
        BroadcastCleanupThread(Handler handler) {
            mHandler = handler;
        }

        /**
         * Cleanup broadcasts.
         */
        public void run() {
            Long currentTime = System.currentTimeMillis();
            List<String> deleteItems = new LinkedList<>();
            for (Map.Entry<String, Long> broadcast: mBroadcastDevices.entrySet()) {
                if (currentTime - broadcast.getValue() > BROADCAST_SHOW_TIMEOUT) {
                    deleteItems.add(broadcast.getKey());
                }
            }
            
            for (String deleteItem: deleteItems) {
                mBroadcastDevices.remove(deleteItem);
            }

            if (deleteItems.size() > 0) {
                Message notification = mHandler.obtainMessage(ADDRESS_REMOVED);
                notification.sendToTarget();
            }
        }
    }

    /**
     * The handler reacts on notifications from our threads.
     */
    @SuppressLint("HandlerLeak") // We don't leak any handlers here
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ADDRESS_FOUND:
                    String ip_to_add = msg.getData().getString("ip");
                    String host = msg.getData().getString("host");
                    if (!mBroadcastDevices.containsKey(ip_to_add)) {
                        mBroadcastDeviceAdapter.add(host + "\n" + ip_to_add);
                    }
                    mBroadcastDevices.put(ip_to_add, System.currentTimeMillis());
                    return;
                case ADDRESS_REMOVED:
                    int i = 0;
                    while (i < mBroadcastDeviceAdapter.getCount()) {
                        String info = mBroadcastDeviceAdapter.getItem(i);
                        if (info != null) {
                            String ip_to_check = info.substring(info.lastIndexOf('\n'));
                            if (!mBroadcastDevices.containsKey(ip_to_check)) {
                                mBroadcastDeviceAdapter.remove(info);
                                // Don't update the index if we removed an item, since the next element
                                // in the list will be accessible on the current index.
                            } else {
                                i++;
                            }
                        }
                    }
            }
        }
    };
}