package com.katesoft.gserver.core;

import static junit.framework.Assert.assertSame;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;

import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.GeneratedMessage;
import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.BaseCommand.Builder;
import com.katesoft.gserver.commands.Commands.LoginCommand;
import com.katesoft.gserver.commands.Commands.MessageHeaders;

public class CommandsResolverTest {

    @SuppressWarnings({})
    @Test
    public void works() throws Exception {
        ExtensionRegistry messageRegistry = CommandsBuilder.newMessageRegistry();

        CommandsQualifierCodec r = new CommandsQualifierCodec.ProtoCommandsCodec( messageRegistry );
        LoginCommand logCmd = LoginCommand.newBuilder().setToken( "tokenX" ).setClientPlatform( "flash" ).build();
        Builder bcmdb = BaseCommand.newBuilder();
        bcmdb.setHeaders(
                MessageHeaders
                        .newBuilder()
                        .setCorrelationID( "corr-1" )
                        .setMessageTimestamp( System.currentTimeMillis() )
                        .setSequenceNumber( 1 )
                        .build() ).setExtension( LoginCommand.cmd, logCmd );
        r.encoder().apply( ImmutablePair.of( bcmdb, (GeneratedMessage) logCmd ) );

        BaseCommand bcmd = bcmdb.build();
        assertSame( LoginCommand.class, r.decoder().apply( bcmd ) );
    }
}
