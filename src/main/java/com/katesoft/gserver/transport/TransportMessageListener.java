package com.katesoft.gserver.transport;

import java.util.EventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands.BaseCommand;

public interface TransportMessageListener extends EventListener {
    void onMessage(BaseCommand cmd, UserConnection userConnection) throws Exception;

    public static final class EchoMessageListener implements TransportMessageListener {
        private final Logger logger = LoggerFactory.getLogger( TransportMessageListener.class );

        @Override
        public void onMessage(BaseCommand cmd, UserConnection userConnection) {
            logger.debug( "onMessage({})={}", cmd.getQualifier(), cmd );

            userConnection.writeSync( cmd );
        }
    }
}
