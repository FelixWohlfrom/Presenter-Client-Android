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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;

/**
 * Dummy mockup server that provides setter and getter to read out data transmitted from/to our
 * wifi connector.
 */
class MockupServer {
    private String stringToTransmit = null;
    
    private ServerSocket mServer;
    private ReaderThread mReaderThread = null;
    private WriterThread mWriterThread = null;

    private final StringBuffer mLastReadMessage;
    
    /**
     * Will initialize the server to accept new client connections.
     */
    MockupServer() throws IOException {
        mLastReadMessage = new StringBuffer();
        
        mServer = new ServerSocket(43155);
        AcceptThread mAcceptThread = new AcceptThread();
        mAcceptThread.start();
    }

    /**
     * Close the server.
     * 
     * @throws IOException If closing fails.
     */
    void close() throws IOException {
        mServer.close();
        if (mReaderThread != null) {
            mReaderThread.close();
        }
        if (mWriterThread != null) {
            mWriterThread.close();
        }
    }

    /**
     * Sets the string to transmit on next transmission to given string. Will transmit the given
     * string once. If empty string or null is given, nothing is transmitted.
     *
     * @param stringToTransmit The string to transmit on next transmission
     */
    void setTransmittedString(String stringToTransmit) {
        if (stringToTransmit != null && stringToTransmit.length() > 0) {
            this.stringToTransmit = stringToTransmit;
        } else {
            this.stringToTransmit = null;
        }
    }

    /**
     * Resets the last transmitted string. Can be used to ensure you get just the data you
     * have written since the last call of this method.
     */
    void resetLastTransmittedString() {
        this.mLastReadMessage.setLength(0);
    }

    /**
     * Returns the last string transmitted via input stream.
     *
     * @return The string received since last call of this method.
     */
    String getLastTransmittedString() {
        String string = this.mLastReadMessage.toString();
        this.mLastReadMessage.setLength(0);
        return string;
    }

    /**
     * The thread to wait for new connections.
     * Will spawn new threads on new client connections.
     */
    private class AcceptThread extends Thread {
        @Override
        public void run() {
            while (!mServer.isClosed()) {
                try {
                    Socket clientSocket = mServer.accept();
                    mReaderThread = new ReaderThread(clientSocket);
                    mReaderThread.start();
                    mWriterThread = new WriterThread(clientSocket);
                    mWriterThread.start();
                } catch (SocketException e) {
                    if (!mServer.isClosed()) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Reader thread to read from a given socket.
     * Read data can be retreived using {@link #getLastTransmittedString()}
     */
    private class ReaderThread extends Thread {
        private final Socket mSocket;
        private final InputStream mInStream;

        /**
         * Creates a new reader thread.
         * 
         * @param socket The socket to read from.
         */
        ReaderThread(Socket socket) {
            mSocket = socket;
            InputStream tmpIn = null;

            // Get the socket input stream
            try {
                tmpIn = socket.getInputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mInStream = tmpIn;
        }
        
        @Override
        public void run() {
            while (!mSocket.isClosed()) {
                byte[] buffer = new byte[100];

                try {
                    int readBytes = mInStream.read(buffer);
                    if (readBytes == -1) {
                        return;
                    }

                    for (int i = 0; i < readBytes; i++) {
                        mLastReadMessage.append((char) buffer[i]);
                    }
                    
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Closes the socket
         * 
         * @throws IOException If the socket can not be closed.
         */
        void close() throws IOException {
            mSocket.close();
        }
    }

    /**
     * Writer thread to write to a socket.
     * Data to be written can be set using {@link #setTransmittedString(String)}
     */
    private class WriterThread extends Thread {
        private final Socket mSocket;
        private final OutputStream mOutStream;

        /**
         * Creates a new writer thread.
         *
         * @param socket The socket to write to
         */
        WriterThread(Socket socket) {
            mSocket = socket;

            OutputStream tmpOut = null;

            // Get the socket output stream
            try {
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }

            mOutStream = tmpOut;
        }

        @Override
        public void run() {
            while (!mSocket.isClosed()) {
                if (stringToTransmit != null) {
                    try {
                        mOutStream.write(stringToTransmit.getBytes());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    stringToTransmit = null;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        /**
         * Closes the socket
         *
         * @throws IOException If the socket can not be closed.
         */
        void close() throws IOException {
            mSocket.close();
        }
    }
}