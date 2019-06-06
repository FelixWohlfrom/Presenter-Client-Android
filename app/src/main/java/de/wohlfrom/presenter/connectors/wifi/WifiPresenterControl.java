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

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import de.wohlfrom.presenter.BuildConfig;
import de.wohlfrom.presenter.connectors.RemoteControl;

/**
 * This class handles the wifi connection. It initiates the connection to a device and can be
 * used to transmit data to the other device.
 */
class WifiPresenterControl extends RemoteControl {
    // Debugging
    private static final String TAG = "WifiControl";

    /**
     * The port on which the remote control server is listening.
     */
    private static final int REMOTE_CONTROL_SERVER_PORT = 43155;

    // Member fields
    private ConnectThread mConnectThread;
    private ReaderThread mReaderThread;
    private WriterThread mWriterThread;

    /**
     * Constructor. Prepares a new wifi presenter control session.
     *
     * @param handler A handler to receive connection results
     */
    WifiPresenterControl(Handler handler) {
        super(handler);
    }

    /**
     * Start the presenter service. Called by the Activity onResume()
     */
    synchronized void start() {
        // Cancel any thread attempting to make a connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mReaderThread != null) {
            mReaderThread.cancel();
            mReaderThread = null;
        }
        if (mWriterThread != null) {
            mWriterThread.cancel();
            mWriterThread = null;
        }
    }

    /**
     * Start the ConnectThread to initiate a connection to a remote device.
     *
     * @param hostname The hostname to connect.
     * @param address The ip address to connect
     */
    synchronized void connect(String hostname, String address) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "connect to: " + address);
        }

        // Cancel any thread attempting to make a connection
        if (mState == ServiceState.CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mReaderThread != null) {
            mReaderThread.cancel();
            mReaderThread = null;
        }
        if (mWriterThread != null) {
            mWriterThread.cancel();
            mWriterThread = null;
        }

        // Start the thread to connect with the given ip
        mConnectThread = new ConnectThread(hostname, address);
        mConnectThread.start();
    }

    /**
     * Start the Reader- and WriterThreads to begin managing a network connection
     *
     * @param socket The socket on which the connection was made
     * @param hostname The hostname to which the connection was made
     */
    private synchronized void connected(Socket socket, String hostname) {
        // Cancel the thread that completed the connection
        // No need to do this, since the thread is always guaranteed to be canceled once we reach
        // this line
        /*if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }*/

        // Cancel any thread currently running a connection
        if (mReaderThread != null) {
            mReaderThread.cancel();
            mReaderThread = null;
        }
        if (mWriterThread != null) {
            mWriterThread.cancel();
            mWriterThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mReaderThread = new ReaderThread(socket, this, hostname);
        mReaderThread.start();
        mWriterThread = new WriterThread(socket);
        mWriterThread.start();
    }

    /**
     * Stop all threads
     */
    synchronized void stop() {
        mState = ServiceState.NONE;

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mReaderThread != null) {
            mReaderThread.cancel();
            mReaderThread = null;
        }
        
        if (mWriterThread != null) {
            mWriterThread.cancel();
            mWriterThread = null;
        }
    }

    @Override
    public void sendMessage(PresenterMessage message) {
        write((message.toString() + "\n\n").getBytes());
    }

    /**
     * Write output to the connected device. If no device is connected, no data is written.
     *
     * @param out The bytes to write
     */
    private void write(byte[] out) {
        synchronized (this) {
            if (mState == ServiceState.NONE) {
                return;
            }
            mWriterThread.write(out);
        }
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Message msg = mHandler.obtainMessage(ServiceState.ERROR.ordinal());
        Bundle bundle = new Bundle();
        bundle.putString(RESULT_VALUES[1], ERROR_TYPES.NO_CONNECTION.toString());
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        
        mState = ServiceState.NONE;
    }

    /**
     * Indicate that the connection was lost and notify callback handler.
     */
    private void connectionLost() {
        // Send a failure message back to the handler
        Message msg = mHandler.obtainMessage(ServiceState.NONE.ordinal());
        Bundle bundle = new Bundle();
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        
        mState = ServiceState.NONE;
    }

    @Override
    protected void disconnect() {
        // This will signal the reader thread to stop reading
        mState = ServiceState.NONE;
        if (mReaderThread != null) {
            mReaderThread.cancel();
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final String hostname;
        private final String address;
        private Socket mmSocket;
        
        // Stores if the connection is cancelled. Needed to avoid race conditions if the 
        // cancel call is done before the socket has been created.
        boolean cancelled;

        /**
         * Creates the connection thread that will connect to a given ip.
         *
         * @param hostname The hostname to connect to
         * @param address The ip to connect to
         */
        ConnectThread(String hostname, String address) {
            cancelled = false;
            this.hostname = hostname;
            this.address = address;

            mState = ServiceState.CONNECTING;

            // Notify the user that we are now connecting
            Message userNotification
                    = mHandler.obtainMessage(ServiceState.CONNECTING.ordinal());
            mHandler.sendMessage(userNotification);
        }

        /**
         * Initiate the connection.
         */
        public void run() {
            setName("ConnectThread");

            synchronized (WifiPresenterControl.this) {
                if (cancelled) {
                    return;
                }
                
                Socket tmp;
    
                // Create the socket connection
                try {
                    tmp = new Socket(InetAddress.getByName(address), REMOTE_CONTROL_SERVER_PORT);
                } catch (Exception e) {
                    Log.e(TAG, "create socket failed", e);
                    connectionFailed();
                    return;
                }
                mmSocket = tmp;
            
                // Reset the ConnectThread because we're done
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, hostname);
        }

        /**
         * Cancel connection to the device.
         */
        void cancel() {
            synchronized (WifiPresenterControl.this) {
                cancelled = true;
                
                if (mmSocket != null) {
                    try {
                        mmSocket.close();
                    } catch (IOException e) {
                        Log.e(TAG, "close() of connect socket failed", e);
                    }
                }
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming transmissions.
     */
    private class ReaderThread extends Thread {
        private final Socket mSocket;
        private final String mHostname;
        private final InputStream mInStream;
        private final RemoteControl mRemoteControl;
        private final StringBuffer mMessageBuffer;

        /**
         * Initiate the transmission using a given socket.
         *
         * @param socket  The socket to read
         * @param control The parent remote control object
         * @param hostname The hostname to read               
         */
        ReaderThread(Socket socket, RemoteControl control, String hostname) {
            mSocket = socket;
            mHostname = hostname;
            mRemoteControl = control;
            mMessageBuffer = new StringBuffer();
            InputStream tmpIn = null;

            // Get the socket input stream
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                Log.e(TAG, "tmp input socket not created", e);
                mState = ServiceState.NONE;
            }

            mInStream = tmpIn;
            // State will be set to connected once the version information is exchanged and we
            // found a common protocol version set to use.
        }

        /**
         * Try reading from input stream to wait until connection is lost.
         */
        public void run() {
            byte[] buffer = new byte[100];

            // Keep listening to the InputStream while connected
            while (mState != ServiceState.NONE) {
                try {
                    int readBytes = mInStream.read(buffer);
                    if (readBytes == -1) {
                        connectionLost();
                        return;
                    }
                    
                    for (int i = 0; i < readBytes; i++) {
                        mMessageBuffer.append((char) buffer[i]);
                    }

                    if (mMessageBuffer.toString().contains("\n\n")) {
                        mRemoteControl.handleMessage(mHostname, 
                                mMessageBuffer.substring(0, mMessageBuffer.indexOf("\n\n")));

                        mMessageBuffer.delete(0, mMessageBuffer.indexOf("\n\n") + "\n\n".length());
                    }
                } catch (IOException e) {
                    // Ignore exception if we already recognized that we are disconnected
                    if (mState != ServiceState.NONE) {
                        Log.e(TAG, "disconnected", e);
                        connectionLost();
                    }
                }
            }
        }

        /**
         * Cancel connection to other device.
         */
        void cancel() {
            try {
                mSocket.close();
                // Wait some time until the thread is really closed
                Thread.sleep(10);
            } catch (IOException|InterruptedException e) {
                Log.e(TAG, "closing of read socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all outgoing transmissions.
     */
    private class WriterThread extends Thread {
        private byte[] buffer;
        private final OutputStream mOutStream;
        private final Object notifier = new Object();

        /**
         * Initiate the transmission using a given socket.
         *
         * @param socket The socket to connect to
         */
        WriterThread(Socket socket) {
            buffer = null;
            
            OutputStream tmpOut = null;

            // Get the socket output stream
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "tmp output socket not created", e);
                mState = ServiceState.NONE;
            }

            mOutStream = tmpOut;
            // State will be set to connected once the version information is exchanged and we
            // found a common protocol version set to use.
        }

        /**
         * Try reading from input stream to wait until connection is lost.
         */
        public void run() {
            while (mState != ServiceState.NONE) {
                synchronized (notifier) {
                    try {
                        notifier.wait();
                    } catch (InterruptedException e) {
                        Log.e(TAG, "Wait for notification interrupted, closing.", e);
                        return;
                    }
                }

                if (buffer != null) {
                    try {
                        mOutStream.write(buffer);
                    } catch (IOException e) {
                        Log.e(TAG, "Exception during write", e);
                    }
                    buffer = null;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        void write(byte[] buffer) {
            this.buffer = buffer;
            synchronized (notifier) {
                notifier.notify();
            }
        }

        /**
         * Stop the thread. Socket closing will be done in reader thread.
         */
        void cancel() {
            synchronized (notifier) {
                notifier.notify();
            }
            
            try {
                // Wait some time until the thread is really closed
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Log.e(TAG, "closing of write socket failed", e);
            }
        }
    }
}