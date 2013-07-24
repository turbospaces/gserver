package com.katesoft.gserver.server;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.katesoft.gserver.transport.ConnectionType;

public class NettyTcpServerTest extends AbstractEmbeddedTest {
    public NettyTcpServerTest() {
        connectionType = ConnectionType.TCP;
    }

    @Test
    public void works() {
        logger.debug( c.getUserConnection().toString() );

        assertEquals( 1, s.connectionsCount() );
        assertSame( c.getUserConnection(), s.getUserConnection( c.getUserConnection().id() ) );

        login();
    }
}
