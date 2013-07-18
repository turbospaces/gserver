package com.katesoft.gserver.api;

import java.util.UUID;

import org.springframework.core.NestedCheckedException;

import com.katesoft.gserver.commands.Commands.BaseCommand;

@SuppressWarnings("serial")
public class AbstractProtocolException extends NestedCheckedException {
    private final UUID uuid = UUID.randomUUID();

    public AbstractProtocolException(String message, Throwable cause) {
        super( message, cause );
    }
    public AbstractProtocolException(String message) {
        super( message );
    }
    public UUID getUuid() {
        return uuid;
    }

    /**
     * server is not aware how to process inbound command (or either game specific command or platform command),
     * possibly due to protocol version collision or any other difficulties.
     */
    public static class UnknownCommadException extends AbstractProtocolException {
        public UnknownCommadException(String gameId) {
            super( "Cmd can't be interpreted by game=%s, check protocol version and details" );
        }
        public UnknownCommadException(BaseCommand cmd) {
            super( "Unknown cmd=%s can't be processed by platform/game" );
        }
    }

    /**
     * to be thrown when the play session considered to be invalid(expired, in-active, locked) or simply doesn't exist.
     */
    public static class InvalidSessionUsageException extends AbstractProtocolException {
        public InvalidSessionUsageException(String sessionId) {
            super( String.format( "There is no such session=%s, please restart game play", sessionId ) );
        }
    }

    /**
     * signals that command qualifier is unknown for platform and games, probably due to protocol version collision.
     */
    public static class UnknownCommandQualifierException extends AbstractProtocolException {
        public UnknownCommandQualifierException(String qualifier) {
            super( String.format( "Unknown cmd=%s, check protocol version", qualifier ) );
        }
    }
}
