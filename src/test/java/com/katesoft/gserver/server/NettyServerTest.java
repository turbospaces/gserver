package com.katesoft.gserver.server;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HostAndPort;
import com.katesoft.gserver.core.UserConnection;
import com.katesoft.gserver.misc.Misc;
import com.katesoft.gserver.transport.NettyServer;
import com.katesoft.gserver.transport.NettyTcpClient;

public class NettyServerTest {
    Logger logger = LoggerFactory.getLogger( getClass() );

    @Test
    public void x() {
        HostAndPort hostAndPort = HostAndPort.fromParts( Misc.shortHostname(), Misc.nextAvailablePort() );
        NettyServer s = new NettyServer();
        s.startServer( hostAndPort );

        NettyTcpClient c = new NettyTcpClient( hostAndPort );
        c.run();
        UserConnection uc = s.awaitForHandshake( c );
        logger.debug( uc.toString() );

        assertEquals( 1, s.connectionsCount() );
        assertSame( uc, s.getUserConnection( uc.id() ) );

        c.close();
        s.close();
    }
}
