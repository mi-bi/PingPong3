
package com.skorka.pingpong3;

/**
 *
 * @author Michal Bialoskorski
 */
public class PingPongException extends Exception {

    /**
     * Creates a new instance of <code>PingPongException</code> without detail
     * message.
     */
    public PingPongException() {
    }

    /**
     * Constructs an instance of <code>PingPongException</code> with the
     * specified detail message.
     *
     * @param msg the detail message.
     */
    public PingPongException(String msg) {
        super(msg);
    }
}
