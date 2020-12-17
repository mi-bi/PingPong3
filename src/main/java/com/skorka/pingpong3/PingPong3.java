/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.skorka.pingpong3;

import java.io.IOException;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;
import javax.websocket.DeploymentException;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;

/**
 *
 * @author Michal Bialoskorski
 */
public class PingPong3 {
    
    // 5000ms for connection attempt
    private static int connection_timeout = 5000;
    // 3000ms for create_session 
    private static int session_timeout = 3000;
    // 5000ms for handshake websocket
    private static int handshake_timeout = 1000;
    // 3000ms ping interval
    private static int ping_interval = 3000;
/**
 * Makes connection to WS server
 * @param retries number of additional retries
 * @param uri WebSocket server URI
 * @throws PingPongException 
 */
    public static void connect(int retries, URI uri) throws PingPongException {
        int retry = retries;
        while (retry >= 0) {
            try {

                ClientManager cm = ClientManager.createClient();
                // 5 sek. na polaczenie
                cm.getProperties().put(ClientProperties.HANDSHAKE_TIMEOUT, handshake_timeout);
                
                System.out.println("Lacze sie z " + uri);
                cm.asyncConnectToServer(WSClient.class, uri);
                WSClient.wait_for_connection(connection_timeout);
                retry = -2;

            } catch (DeploymentException | PingPongException ex ) {
                System.out.println("Problemy z nawiazaniem polaczenia");
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
                WSClient.wait_for_session(session_timeout);
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
/**
 * Prints info and help message
 */
    public static void phelp() {
        System.out.println("PingPong Client");
        System.out.println("");
        System.out.println("Uzycie: PingPong3 IP PORT");
        System.out.println("");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            phelp();
            return;
        }
  
        String address = "ws://" + args[0] + ":" + args[1] + "/";
        System.out.println("ustawiam adres na:" + address);
        URI uri = URI.create(address);
        
        try {
            connect(3, uri);
        } catch (PingPongException ex) {
            System.out.println(ex.toString());
            return;
        }
        
        int retry = 1;
        while (retry >= 0) {
            try {
                WSClient.ping(ping_interval);
                retry = -2;
            } catch (PingPongException e) {
                System.out.println(e.toString());
                try {
                    WSClient.close();
                } catch (IOException ex) {
                    System.out.println(ex);
                }
                retry--;
                if (retry >= 0)
                    try {
                    connect(0, uri);
                } catch (PingPongException ex) {
                    System.out.println(ex.toString());
                }
            }
        }
        if (retry == -1) {
            System.out.println("Drugi blad w komunikacji. Koniec.");
        }
    }
}
