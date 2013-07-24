package com.katesoft.gserver.server;

import java.util.concurrent.atomic.AtomicLong;

import org.junit.Test;

import com.google.common.base.Throwables;
import com.google.common.util.concurrent.ListenableFuture;
import com.katesoft.gserver.commands.Commands.OpenGamePlayReply;
import com.katesoft.gserver.games.RouletteGame;
import com.katesoft.gserver.games.roulette.RoulleteCommands.GetRoulettePositionInfoCommand;
import com.katesoft.gserver.transport.NettyTcpClient;

public class NettyPerformanceTest extends AbstractEmbeddedTest {
    @Test
    public void run() throws InterruptedException {
        final int clients = Runtime.getRuntime().availableProcessors() / 2;
        final NettyTcpClient[] tcpClients = new NettyTcpClient[clients];
        final Thread[] threads = new Thread[clients];
        final AtomicLong iterations = new AtomicLong();
        final AtomicLong now = new AtomicLong();
        try {
            for ( int i = 0; i < clients; i++ ) {
                final NettyTcpClient client = newClient();
                String token = newRememberMeToken( newUserAccount( "user-" + i ) );
                tcpClients[i] = client;
                login( client, token );
            }
            now.addAndGet( System.currentTimeMillis() );
            for ( int i = 0; i < clients; i++ ) {
                final NettyTcpClient client = tcpClients[i];
                Thread t = new Thread( new Runnable() {
                    @SuppressWarnings("rawtypes")
                    @Override
                    public void run() {
                        OpenGamePlayReply openGamePlay = openGamePlay( RouletteGame.class, client );

                        for ( int j = 0; j < 128; j++ ) {
                            ListenableFuture[] replies = new ListenableFuture[4096];
                            for ( int i = 0; i < replies.length; i++ ) {
                                replies[i] = client.callAsync( GetRoulettePositionInfoCommand.cmd, GetRoulettePositionInfoCommand
                                        .newBuilder()
                                        .build(), openGamePlay.getSessionId() );
                            }
                            for ( int i = 0; i < replies.length; i++ ) {
                                iterations.incrementAndGet();
                                try {
                                    replies[i].get();
                                }
                                catch ( Exception e ) {
                                    Throwables.propagate( e );
                                }
                            }
                        }
                    }
                } );
                t.start();
                threads[i] = t;
            }
        }
        finally {
            for ( int i = 0; i < clients; i++ ) {
                threads[i].join();
            }
            long took = ( System.currentTimeMillis() - now.get() );
            logger.info( "took = {} ms, TPS = {}", took, iterations.get() * 1000 / took );
            for ( int i = 0; i < clients; i++ ) {
                NettyTcpClient client = tcpClients[i];
                if ( client != null ) {
                    client.close();
                }
            }
        }
    }
}
