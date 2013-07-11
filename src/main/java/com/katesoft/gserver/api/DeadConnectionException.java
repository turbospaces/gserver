package com.katesoft.gserver.api;

/**
 * exception to be thrown when connection considered to be dead ( missing keep-alive synchronization packets) or when
 * you need to force connection close.
 */
@SuppressWarnings("serial")
public class DeadConnectionException extends RuntimeException {
    public DeadConnectionException(UserConnection uc, String msg) {
        super( String.format( "connection=%s is considered to be dead due to=%s", msg ) );
    }
}
