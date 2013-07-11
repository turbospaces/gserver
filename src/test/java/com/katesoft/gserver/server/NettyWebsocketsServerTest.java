package com.katesoft.gserver.server;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.katesoft.gserver.transport.ConnectionType;

public class NettyWebsocketsServerTest extends AbstractEmbeddedTest {
    public NettyWebsocketsServerTest() {
        connectionType = ConnectionType.WEBSOCKETS;
    }

    @Test
    public void works() {
        assertEquals( 1, s.connectionsCount() );
        assertSame( uc, s.getUserConnection( uc.id() ) );

        login();
    }
}
