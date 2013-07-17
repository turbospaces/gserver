package com.katesoft.gserver.games;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertSame;

import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.katesoft.gserver.api.BetWrapper;
import com.katesoft.gserver.commands.Commands.OpenGamePlayReply;
import com.katesoft.gserver.games.roulette.RoulleteCommands.GetRoulettePositionInfoCommand;
import com.katesoft.gserver.games.roulette.RoulleteCommands.GetRoulettePositionInfoReply;
import com.katesoft.gserver.games.roulette.RoulleteCommands.RouletteBetPosition;
import com.katesoft.gserver.games.roulette.RoulleteCommands.RouletteSpinCommand;
import com.katesoft.gserver.games.roulette.RoulleteCommands.RouletteSpinReply;
import com.katesoft.gserver.server.AbstractEmbeddedTest;
import com.katesoft.gserver.transport.ConnectionType;

public class RouletteEmbeddedTest extends AbstractEmbeddedTest {
    public RouletteEmbeddedTest() {
        connectionType = ConnectionType.WEBSOCKETS;
    }

    @Test
    public void works() throws InterruptedException, ExecutionException {
        login();
        OpenGamePlayReply openGamePlay = openGamePlay( RouletteGame.class );
        openGamePlay( RouletteGame.class );
        assertEquals( 1, repo.findUserPlayerSessions( username ).size() );

        GetRoulettePositionInfoReply infoReply = c
                .callAsync(
                        GetRoulettePositionInfoCommand.cmd,
                        GetRoulettePositionInfoCommand.newBuilder().build(),
                        openGamePlay.getSessionId(),
                        true )
                .get()
                .getExtension( GetRoulettePositionInfoReply.cmd );
        System.out.println( infoReply.toString() );

        RouletteBetPosition position = RouletteBetPosition.values()[RouletteBetPosition.values().length - 4];
        RouletteSpinCommand req = RouletteSpinCommand.newBuilder().setBet( BetWrapper.mock() ).setPosition( position ).build();

        RouletteSpinReply reply = c
                .callAsync( RouletteSpinCommand.cmd, req, openGamePlay.getSessionId(), true )
                .get()
                .getExtension( RouletteSpinReply.cmd );

        assertSame( position, reply.getPosition() );
        assertNotNull( reply.getBetResult() );
        logger.trace( reply.toString() );

        logout( openGamePlay.getSessionId() );
        assertEquals( 0, repo.findUserPlayerSessions( username ).size() );
    }
}
