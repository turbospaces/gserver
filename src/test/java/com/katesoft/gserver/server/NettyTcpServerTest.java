package com.katesoft.gserver.server;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.katesoft.gserver.transport.ConnectionType;

public class NettyTcpServerTest extends AbstractEmbeddedTest {
    public NettyTcpServerTest() {
        connectionType = ConnectionType.TCP;
    }

    @Test
    public void works() throws InterruptedException, ExecutionException {
        logger.debug( uc.toString() );

        assertEquals( 1, s.connectionsCount() );
        assertSame( uc, s.getUserConnection( uc.id() ) );

        login();
    }
}