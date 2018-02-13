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

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.ParcelUuid;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class is used to create a fake bluetooth socket.
 * It allows setting if the connection to the socket succeeds or fails
 * (using {link {@link #connectionSucceed}}) and also if reading from inputStream should fail or
 * succeed (using {@link #failReading}.
 * It is also possible to read the last data written to inputStream using
 * {@link #getLastTransmittedString()}.
 *
 * The class needs to be public to make Robolectric happy.
 */
@Implements(BluetoothSocket.class)
public class ShadowBluetoothSocket implements Closeable {
    /**
     * Stores the configurations for our socket.
     *
     * Use Boolean here instead of boolean to get a NullPointerException if state is not set
     * manually.
     */
    private static Boolean connectionSucceed = null;
    private static Boolean failReading = null;
    private static byte[] stringToTransmit = null;

    /**
     * Our input stream that will either return an empty value or throws an IOException,
     * depending on configuration.
     */
    private final static InputStream receivedStringWriter = new InputStream() {
        @Override
        public int read() throws IOException {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                // Ignore
            }
            if (failReading) {
                throw new IOException("Reading error");
            } else {
                if (stringToTransmit == null) {
                    return -1;
                } else {
                    return stringToTransmit.length;
                }
            }
        }

        @Override
        public int read(byte[] b) throws IOException {
            if (failReading) {
                throw new IOException("Reading error");
            } else {
                if (stringToTransmit != null) {
                    int transmissionLength = Math.min(stringToTransmit.length, b.length);
                    System.arraycopy(stringToTransmit, 0, b, 0, transmissionLength);

                    if (stringToTransmit.length > b.length) {
                        System.arraycopy(stringToTransmit, 0, stringToTransmit, b.length,
                                stringToTransmit.length - b.length);
                    } else {
                        stringToTransmit = null;
                    }

                    return transmissionLength;
                } else {
                    return -1;
                }
            }
        }
    };

    /** Stream to receive the data, can be returned by {@link #getLastTransmittedString()} */
    private final static ByteArrayOutputStream transmittedStringWriter
            = new ByteArrayOutputStream();

    @Implementation
    protected void __constructor__(int type, int fd, boolean auth, boolean encrypt,
                        BluetoothDevice device, int port, ParcelUuid uuid) throws IOException {
        // Empty dummy
    }

    @Implementation
    protected void connect() throws IOException {
        if (!connectionSucceed) {
            throw new IOException("Connection failed");
        }
    }

    @Implementation
    protected InputStream getInputStream() throws IOException {
        return receivedStringWriter;
    }

    @Implementation
    protected OutputStream getOutputStream() throws IOException {
        return transmittedStringWriter;
    }

    @Implementation
    public void close() throws IOException {
        // Do nothing
    }

    /**
     * Set if the connection to this socket should succeed or not.
     *
     * @param connectionSucceed If connection should succeed or not
     */
    static void setConnectionSucceed(boolean connectionSucceed) {
        ShadowBluetoothSocket.connectionSucceed = connectionSucceed;
    }

    /**
     * Sets if reading from the socket should fail.
     *
     * @param failReading If reading should fail
     */
    static void setFailReading(boolean failReading) {
        ShadowBluetoothSocket.failReading = failReading;
    }

    /**
     * Sets the string to transmit on next transmission to given string. Will transmit the given
     * string once. If empty string or null is given, nothing is transmitted.
     *
     * @param stringToTransmit The string to transmit on next transmission
     */
    static void setTransmittedString(String stringToTransmit) {
        if (stringToTransmit != null && stringToTransmit.length() > 0) {
            ShadowBluetoothSocket.stringToTransmit = stringToTransmit.getBytes();
        } else {
            ShadowBluetoothSocket.stringToTransmit = null;
        }
    }

    /**
     * Resets the last transmitted string. Can be used to ensure you get just the data you
     * have written since the last call of this method.
     */
    static void resetLastTransmittedString() {
        transmittedStringWriter.reset();
    }

    /**
     * Returns the last string transmitted via input stream.
     *
     * @return The string received since last call of this method.
     */
    static String getLastTransmittedString() {
        String string = transmittedStringWriter.toString();
        transmittedStringWriter.reset();
        return string;
    }
}
