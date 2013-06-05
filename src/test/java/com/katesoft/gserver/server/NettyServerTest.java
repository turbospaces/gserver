package com.katesoft.gserver.server;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.google.common.net.HostAndPort;
import com.katesoft.gserver.misc.Misc;
import com.katesoft.gserver.transport.NettyServer;
import com.katesoft.gserver.transport.NettyTcpClient;

public class NettyServerTest {
    @Test
    public void x() throws InterruptedException {
        HostAndPort hostAndPort = HostAndPort.fromParts( Misc.shortHostname(), Misc.nextAvailablePort() );
        NettyServer s = new NettyServer();
        s.startServer( hostAndPort );

        NettyTcpClient c = new NettyTcpClient( hostAndPort );
        c.run();
        s.awaitForHandshake(c);
        
        assertEquals( 1, s.connectionsCount() );

        c.close();
        s.close();
    }
}
