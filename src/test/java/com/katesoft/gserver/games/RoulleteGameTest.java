package com.katesoft.gserver.games;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.katesoft.gserver.api.BetWrapper;
import com.katesoft.gserver.api.CommandWrapperEvent;
import com.katesoft.gserver.api.GamePlayContext;
import com.katesoft.gserver.core.CommandsQualifierCodec;
import com.katesoft.gserver.games.RouletteGame.PositionPayout;
import com.katesoft.gserver.games.roullete.RoulleteCommands.RouletteSpinRequest;
import com.katesoft.gserver.games.roullete.RoulleteCommands.RoulleteBetPositions;

public class RoulleteGameTest {
    Logger logger = LoggerFactory.getLogger( getClass() );
    RouletteGame game;
    GamePlayContext.RTP ctx = new GamePlayContext.RTP();

    @Before
    public void setup() {
        CommandsQualifierCodec.DEFAULT.get();
        game = new RouletteGame();
        game.init( ctx );
    }

    @Test
    public void testPositionsWithNumber0() throws Exception {
        testPosition( RoulleteBetPositions.number_0 );
    }

    private void testPosition(RoulleteBetPositions position) throws Exception {
        PositionPayout positionPayout = RouletteGame.ALL.get( position );
        int payout = positionPayout.getPayout();

        for ( int i = 0; i < payout * 10; i++ ) {
            game.getGameCommandInterpreter().interpretCommand(
                    CommandWrapperEvent.mock(
                            RouletteSpinRequest.cmd,
                            RouletteSpinRequest.newBuilder().setPosition( position ).setBet( BetWrapper.mock() ).build() ) );
        }
        int actualPayout = ctx.totalCount( false ) / ctx.totalCount( true );
        logger.debug(
                "position={},payout={},actualPayout={}(:::) Wins={},Loses={}, totalWin={}",
                positionPayout.getPosition(),
                positionPayout.getPayout(),
                actualPayout,
                ctx.totalCount( true ),
                ctx.totalCount( false ),
                ctx.totalWin() );
    }
}
