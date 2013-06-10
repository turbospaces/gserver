package com.katesoft.gserver.games;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;

import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.katesoft.gserver.api.BetWrapper;
import com.katesoft.gserver.commands.Commands.OpenGamePlayReply;
import com.katesoft.gserver.games.roullete.RoulleteCommands.RouletteBetPosition;
import com.katesoft.gserver.games.roullete.RoulleteCommands.RouletteSpinCommand;
import com.katesoft.gserver.games.roullete.RoulleteCommands.RouletteSpinReply;
import com.katesoft.gserver.server.AbstractEmbeddedTest;

public class RouletteEmbeddedTest extends AbstractEmbeddedTest {

    @Test
    public void works() throws InterruptedException, ExecutionException {
        login();
        OpenGamePlayReply openGamePlay = openGamePlay( RouletteGame.class );

        RouletteBetPosition position = RouletteBetPosition.values()[RouletteBetPosition.values().length - 4];
        RouletteSpinCommand req = RouletteSpinCommand.newBuilder().setBet( BetWrapper.mock() ).setPosition( position ).build();
        RouletteSpinReply reply = c.callAsync( RouletteSpinCommand.cmd, req, openGamePlay ).get().getExtension( RouletteSpinReply.cmd );
        assertSame( position, reply.getPosition() );
        assertNotNull( reply.getBetResult() );
        logger.trace( reply.toString() );
    }
}
