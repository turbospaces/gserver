package com.katesoft.gserver.server;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.katesoft.gserver.commands.Commands.Geti18nMessagesReply;
import com.katesoft.gserver.domain.Entities.i18n;
import com.katesoft.gserver.transport.ConnectionType;

public class NettyWebsocketsServerTest extends AbstractEmbeddedTest {
    public NettyWebsocketsServerTest() {
        connectionType = ConnectionType.WEBSOCKETS;
    }

    @Test
    public void works() throws InterruptedException, ExecutionException {
        assertEquals( 1, s.connectionsCount() );
        assertSame( uc, s.getUserConnection( uc.id() ) );

        login();

        ResourceBundle resourceBundle = ResourceBundle.getBundle( "messages" );
        Geti18nMessagesReply messages = geti18nMessages( resourceBundle.keySet() );
        List<i18n> valuesList = messages.getValuesList();
        logger.debug( valuesList.toString() );
    }
}
