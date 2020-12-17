package com.skorka.pingpong3;

/**
 * WebSocket Client
 *
 * @author Michal Bialoskorski
 */
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.websocket.ClientEndpoint;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import org.json.JSONObject;

/**
 *
 * @author skorka
 */
@ClientEndpoint
public class WSClient {

    private static int transaction_id = 0;
    // current transaction_id
    private static int current_transaction = -1;

    // -1: unknown
    // 0: request sent
    // 1: answer received
    private static int transaction_status = -1;

    // current session_id
    private static int session_id = -1;
    private static Session session_ws = null;
    // main loop condition
    private static boolean do_ping_loop = true;
    // true after connection, before create_session request
    private static boolean connected = false;

    // 0 no error
    // != 0 error
    private static int errno = 0;

    /**
     * Just after handshake. Requests create_session
     *
     * @param session
     */
    @OnOpen
    public void onOpen(Session session) {
        if (connected) {
            try {
                session.close();
            } catch (IOException ex) {
                Logger.getLogger(WSClient.class.getName()).log(Level.SEVERE, null, ex);
            }
            return;
        }
        try {
            session_id = -1;
            current_transaction = -1;
            transaction_id = 0;
            do_ping_loop = true;
            session_ws = null;
            JSONObject req = new JSONObject();
            req.put("request", "create_session");
            req.put("transaction_id", get_next_transaction_id());
            String msg = req.toString();
            System.out.println("Wysylam: " + msg);
            session.getBasicRemote().sendText(msg);
            session_ws = session;
            connected = true;
        } catch (IOException ex) {
            Logger.getLogger(WSClient.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Called on incoming message
     *
     * @param message
     * @throws PingPongException
     */
    @OnMessage
    public void onMessage(String message) throws PingPongException {
        System.out.println("Odebralem: " + message);
        try {
            JSONObject ans = new JSONObject(message);

            int ans_id = ans.getInt("transaction_id");
            if (ans_id != transaction_id) {
                throw new PingPongException("Zly numer transakcji");
            }
            if (session_id == -1) {
                session_id = ans.getInt("session_id");
            }
            if (ans.has("message")) {
                String msg = ans.getString("message");
                if (!msg.equals("pong")) {
                    throw new PingPongException("zly pong");
                }
            }
            transaction_status = 1;
        } catch (org.json.JSONException e) {
            throw new PingPongException("problem z JSON");
        }
    }

    /**
     * Called on all exceptions from ClientEndpoint
     *
     * @param t the exception
     */
    @OnError
    public void onError(Throwable t) {
        //      System.out.println(t);
        System.out.println("OnError: " + t);
        errno = 1;
        do_ping_loop = false;
    }

    /**
     * Sets statements for new transaction
     *
     * @return new trnasaction_id
     */
    private static int get_next_transaction_id() {
        transaction_id++;
        current_transaction = transaction_id;
        transaction_status = 0;
        return current_transaction;
    }

    /**
     * Closes Session socket
     *
     * @throws IOException
     */
    public static void close() throws IOException {
        session_id = -1;
        if (session_ws != null && session_ws.isOpen()) {
            session_ws.close();
        }
    }

    /**
     * Waits for create_session process complete
     *
     * @param timeout for create_session in milliseconds
     * @throws com.skorka.pingpong3.PingPongException
     *
     */
    public static void wait_for_session(int timeout) throws PingPongException {
        int t = timeout;
        int dt = timeout / 10;
        if (dt > 100) {
            dt = 100;
        }
        while (session_id == -1) {
            try {
                TimeUnit.MILLISECONDS.sleep(dt);
                t = t - dt;
            } catch (InterruptedException ex) {
                Logger.getLogger(WSClient.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (t <= 0) {
                throw new PingPongException("Przekroczony czas utowrzenia sesji");
            }
        }
    }

    /**
     * Waits for the connection to open
     *
     * @param timeout
     * @throws PingPongException
     */
    public static void wait_for_connection(int timeout) throws PingPongException {
        int t = timeout;
        int dt = timeout / 10;
        if (dt > 100) {
            dt = 100;
        }
        while (!connected) {
            try {
                TimeUnit.MILLISECONDS.sleep(dt);
                t = t - dt;
            } catch (InterruptedException ex) {
                Logger.getLogger(WSClient.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (t <= 0) {
                throw new PingPongException("Przekroczony czas na polaczenie");
            }
        }
    }

    /**
     * The main program loop
     *
     * @param interval of ping requests
     * @throws PingPongException
     */
    public static void ping(int interval) throws PingPongException {
        JSONObject req = new JSONObject();
        req.put("request", "ping");
        req.put("session_id", session_id);

        while (do_ping_loop && session_ws != null) {
            req.put("transaction_id", get_next_transaction_id());
            try {
                session_ws.getBasicRemote().sendText(req.toString());
                TimeUnit.MILLISECONDS.sleep(interval);
                if (transaction_status != 1) {
                    throw new PingPongException("Brak odpowiedzi na ping");
                }
            } catch (Exception ex) {
                throw new PingPongException(ex.toString() + " Exception w ping:");
            }
        }
        if (errno > 0) {
            throw new PingPongException("Exception w ping");
        }
    }
}
