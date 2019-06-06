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
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Timer;
import java.util.TimerTask;

import de.wohlfrom.presenter.connectors.RemoteControl;

/**
 * Dummy broadcast server that will send out a broadcast to localhost
 * every few milliseconds. 
 */
class BroadcastServer extends TimerTask {
    static final String WIFI_DEVICE_NAME = "Testhost";
    static final String WIFI_IP_ADDRESS = "127.0.0.1";
    
    private Timer broadcastTimer;

    /**
     * Will initialize the server sender thread.
     */
    BroadcastServer() {
        broadcastTimer = new Timer();
    }

    /**
     * Will start the broadcasting of our server and wait until first messages are sent out.
     *
     * @throws InterruptedException If waiting for first messages to be sent out was
     *                              interrupted.
     */
    void start() throws InterruptedException {
        broadcastTimer.schedule(this, 0, 20);

        Thread.sleep(50);
    }

    /**
     * Stops broadcasting.
     */
    void stop() {
        broadcastTimer.cancel();
    }

    @Override
    public void run() {
        try {
            InetAddress host = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
            DatagramSocket socket = new DatagramSocket(null);
            String msg = RemoteControl.SERVICE_ID + "\n" + BroadcastServer.WIFI_DEVICE_NAME;
            DatagramPacket packet = new DatagramPacket(msg.getBytes(), msg.length(),
                    host, DeviceSelector.DEVICE_DISCOVERY_PORT);
            socket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
