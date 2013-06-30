package com.katesoft.gserver.core;

import static junit.framework.Assert.assertSame;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Test;

import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.LoginCommand;

public class CommandsResolverTest {

    @SuppressWarnings({ })
    @Test
    public void works() {
        Commands.newMessageRegistry();
        CommandsQualifierCodec.DefaultCommandsCodec r = new CommandsQualifierCodec.DefaultCommandsCodec();
        LoginCommand logCmd = LoginCommand.newBuilder().setPlayerId( "playerX" ).setCredentials( "tokenX" ).setClientPlatform( "flash" ).build();
        r.codec().apply( ImmutablePair.of( BaseCommand.newBuilder(), (Object) logCmd ) );
        assertSame( LoginCommand.class, r.decodec().apply( LoginCommand.class.getSimpleName() ) );
        r.close();
    }
}
