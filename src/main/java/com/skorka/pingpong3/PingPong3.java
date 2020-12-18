/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.skorka.pingpong3;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Future;
import javax.websocket.DeploymentException;
import javax.websocket.Session;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;

/**
 *
 * @author Michal Bialoskorski
 */
public class PingPong3 {

    public int connection_timeout = 5000;
    public int session_timeout = 3000;
    public int handshake_timeout = 1000;

    WSClient wsc;

    /**
     * Makes connection to WS server
     *
     * @param retries number of additional retries
     * @param uri WebSocket server URI
     * @throws PingPongException
     */
    public void connect(int retries, URI uri) throws PingPongException {
        int retry = retries;
        ClientManager cm = ClientManager.createClient();
        cm.getProperties().put(ClientProperties.HANDSHAKE_TIMEOUT, handshake_timeout);
        Future<Session> conn = null;
        wsc = new WSClient();

        while (retry >= 0) {
            try {

                System.out.println("Lacze sie z " + uri);
                conn = cm.asyncConnectToServer(WSClient.class, uri);
                wsc.wait_for_connection(connection_timeout);
                retry = -2;
            } catch (DeploymentException | PingPongException ex) {
                System.out.println("Problemy z nawiazaniem polaczenia");
                if (conn != null) {
                    conn.cancel(true);
                }
                retry--;
            }

        }
        if (retry == -1) {
            System.out.println("Nie udalo sie polaczyc");
            throw new PingPongException("Problem z polaczeniem sie");
        }

        retry = 3;
        while (retry >= 0) {
            try {
                wsc.wait_for_session(session_timeout);
                retry = -2;
            } catch (PingPongException ex) {
                System.out.println(ex);
                retry--;
            }
        }
        if (retry == -1) {
            throw new PingPongException("Nipowodzenie tworzenia sesji");
        }
    }

    public void ping(int interval) throws PingPongException {
        wsc.ping(interval);

    }

    public void close() throws IOException {
        wsc.close();

    }

}
