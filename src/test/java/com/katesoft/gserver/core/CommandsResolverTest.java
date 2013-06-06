package com.katesoft.gserver.core;

import static junit.framework.Assert.assertSame;

import java.util.AbstractMap;

import org.junit.Test;

import com.katesoft.gserver.commands.Commands.BaseCommand;
import com.katesoft.gserver.commands.Commands.LoginCommand;
import com.katesoft.gserver.core.Commands;
import com.katesoft.gserver.core.CommandsQualifierCodec;

public class CommandsResolverTest {

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Test
    public void works() {
        Commands.newMessageRegistry();
        CommandsQualifierCodec.DefaultCommansResolver r = new CommandsQualifierCodec.DefaultCommansResolver();
        LoginCommand logCmd = LoginCommand.newBuilder().setPlayerId("playerX").setCredentials("tokenX").setClientPlatform("flash").build();
        r.qualifierWriter().apply(new AbstractMap.SimpleEntry(BaseCommand.newBuilder(), logCmd));
        assertSame(LoginCommand.class, r.qualifierToType().apply(LoginCommand.class.getSimpleName()));
        r.close();
    }
}
