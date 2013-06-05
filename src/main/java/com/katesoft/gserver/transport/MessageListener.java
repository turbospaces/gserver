package com.katesoft.gserver.transport;

import java.util.EventListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands.BaseCommand;

public interface MessageListener extends EventListener {
    void onMessage(BaseCommand cmd, UserConnection userConnection) throws Exception;

    public static final class EchoMessageListener implements MessageListener {
        private final Logger logger = LoggerFactory.getLogger( MessageListener.class );

        @Override
        public void onMessage(BaseCommand cmd, UserConnection userConnection) throws InterruptedException, ClassNotFoundException {
            Class<?> clazz = Class.forName( cmd.getQualifier() );
            logger.debug( "onMessage({})={}", clazz.getSimpleName(), cmd );

            userConnection.writeSync( cmd );
        }
    }
}
