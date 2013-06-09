package com.katesoft.gserver.games;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;

import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.katesoft.gserver.api.BetWrapper;
import com.katesoft.gserver.commands.Commands.OpenGamePlayReply;
import com.katesoft.gserver.games.roullete.RoulleteCommands.RouletteSpinCommand;
import com.katesoft.gserver.games.roullete.RoulleteCommands.RoulleteBetPositions;
import com.katesoft.gserver.games.roullete.RoulleteCommands.RoulleteSpinReply;
import com.katesoft.gserver.server.AbstractEmbeddedTest;

public class RoulleteEmbeddedTest extends AbstractEmbeddedTest {

    @Test
    public void works() throws InterruptedException, ExecutionException {
        login();
        OpenGamePlayReply openGamePlay = openGamePlay(RouletteGame.class);

        RoulleteBetPositions position = RoulleteBetPositions.values()[RoulleteBetPositions.values().length - 1];
        RouletteSpinCommand req = RouletteSpinCommand.newBuilder().setBet( BetWrapper.mock() ).setPosition( position ).build();
        RoulleteSpinReply reply = c.callAsync( RouletteSpinCommand.cmd, req, openGamePlay ).get().getExtension( RoulleteSpinReply.cmd );
        assertSame(position, reply.getPosition());
        assertNotNull(reply.getBetResult());
        logger.trace(reply.toString());
    }
}
