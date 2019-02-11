package com.inverita.vpnapptask.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Class is providing methods for getting information from Internet.
 */
public class InternetUtils {
    public InternetUtils() {
    }

    /**
     * Method returns your current external IP address.
     *
     * @return IP address.
     * @throws IOException exception in case of being unable to connect to server.
     */
    public String getMyOwnIP() throws IOException {
        final String hostname = "ifconfig.co";
        final int port = 80;
        final int timeout = 20000;

        final StringBuilder resp = new StringBuilder();
        final Socket client = new Socket();
        // Setting Keep Alive forces creation of the underlying socket, otherwise getFD returns -1
        client.setKeepAlive(true);

        client.connect(new InetSocketAddress(hostname, port), timeout);
        client.shutdownOutput();
        final BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
        while (true) {
            final String line = in.readLine();
            if (line == null) {
                return resp.toString();
            }
            resp.append(line);
        }
    }
}
