package com.katesoft.gserver.server;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;

import org.junit.Test;

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
    }
}
