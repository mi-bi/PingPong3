/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.skorka.pingpong3;

import java.io.IOException;
import java.net.URI;

/**
 *
 * @author skorka
 */
public class Main {

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
        // 3000ms ping interval
        int ping_interval = 3000;

        if (args.length != 2) {
            phelp();
            return;
        }

        String address = "ws://" + args[0] + ":" + args[1] + "/";
        System.out.println("ustawiam adres na:" + address);
        URI uri = URI.create(address);
        PingPong3 pingpong = new PingPong3();

        pingpong.connection_timeout = 5000;
        pingpong.session_timeout = 3000;
        pingpong.handshake_timeout = 1000;

        try {
            pingpong.connect(3, uri);
        } catch (PingPongException ex) {
            System.out.println(ex);
            return;
        }

        int retry = 2;

        while (retry >= 0) {
            try {
                pingpong.ping(ping_interval);
                retry = -2;
            } catch (PingPongException e) {
                System.out.println(e);
                try {
                    pingpong.close();
                } catch (IOException ex) {
                    System.out.println(ex);
                }
                retry--;
                if (retry >= 0)
                    try {
                    pingpong = new PingPong3();
                    pingpong.connect(0, uri);
                } catch (PingPongException ex) {
                    System.out.println(ex);
                }
            }
        }
        if (retry == -1) {
            System.out.println("Drugi blad w komunikacji. Koniec.");
        }
        /*        
        while(true)
        {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException ex) {
                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
            }
            
        }
         */
    }
}
