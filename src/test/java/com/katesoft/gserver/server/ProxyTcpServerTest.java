package com.katesoft.gserver.server;

import static com.google.common.net.HostAndPort.fromParts;
import static com.katesoft.gserver.misc.Misc.nextAvailablePort;
import static com.katesoft.gserver.misc.Misc.shortHostname;
import static org.apache.commons.lang3.tuple.ImmutablePair.of;

import java.util.concurrent.ExecutionException;

import org.junit.After;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.net.HostAndPort;
import com.katesoft.gserver.api.TransportServer;
import com.katesoft.gserver.transport.NettyServer;
import com.katesoft.gserver.transport.ProxyServer;

public class ProxyTcpServerTest extends AbstractEmbeddedTest {
    HostAndPort proxyHostAndPort = fromParts( shortHostname(), nextAvailablePort() );
    NettyServer anotherServer;
    ProxyServer proxy;

    @Override
    protected ProxyServer setupProxy(NettyServer ns, HostAndPort actualPort) {
        TransportServer.TransportServerSettings settings = TransportServer.TransportServerSettings.avail();
        anotherServer = new NettyServer();
        anotherServer.startServer( settings, s.transportMessageListener() );

        proxy = new ProxyServer( proxyHostAndPort, of( settings.tcp, Optional.of( actualPort ) ) );
        proxy.run();

        return proxy;
    }
    @After
    public void shutdown() {
        proxy.close();
    }
    @Test
    public void works() throws InterruptedException, ExecutionException {
        login();
        anotherServer.close();
        login();
    }
}
