package com.katesoft.gserver.server;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.ExecutionException;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.net.HostAndPort;
import com.katesoft.gserver.api.UserConnection;
import com.katesoft.gserver.commands.Commands.LoginCommand;
import com.katesoft.gserver.misc.Misc;
import com.katesoft.gserver.transport.MessageListener;
import com.katesoft.gserver.transport.NettyServer;
import com.katesoft.gserver.transport.NettyTcpClient;

public class NettyServerTest {
    Logger logger = LoggerFactory.getLogger( getClass() );

    @Test
    public void x() throws InterruptedException, ExecutionException {
        HostAndPort hostAndPort = HostAndPort.fromParts( Misc.shortHostname(), Misc.nextAvailablePort() );
        NettyServer s = new NettyServer();
        s.startServer( hostAndPort, new MessageListener.EchoMessageListener() );

        NettyTcpClient c = new NettyTcpClient( hostAndPort );
        c.run();
        UserConnection uc = s.awaitForHandshake( c );
        logger.debug( uc.toString() );

        assertEquals( 1, s.connectionsCount() );
        assertSame( uc, s.getUserConnection( uc.id() ) );

        LoginCommand logCmd = LoginCommand.newBuilder().setPlayerId( "playerX" ).setToken( "tokenX" ).build();
        c.callAsync( LoginCommand.cmd, logCmd ).get().getExtension( LoginCommand.cmd );

        c.close();
        s.close();
    }
}
