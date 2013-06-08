package com.katesoft.gserver.games;

import static com.katesoft.gserver.misc.Misc.repeatConcurrently;
import static junit.framework.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.katesoft.gserver.api.BetWrapper;
import com.katesoft.gserver.api.GamePlayContext;
import com.katesoft.gserver.api.PlayerSession;
import com.katesoft.gserver.core.Commands;
import com.katesoft.gserver.core.CommandsQualifierCodec;
import com.katesoft.gserver.games.RouletteGame.PositionPayout;
import com.katesoft.gserver.games.roullete.RoulleteCommands.RouletteSpinRequest;
import com.katesoft.gserver.games.roullete.RoulleteCommands.RoulleteBetPositions;

public class RoulleteGameTest {
    Logger logger = LoggerFactory.getLogger(getClass());
    RouletteGame game;
    GamePlayContext.RTP ctx = new GamePlayContext.RTP();

    @Before
    public void setup() {
        CommandsQualifierCodec.DEFAULT.get();
        game = new RouletteGame();
        game.init(ctx);
    }

    @Test
    public void testPositionsWithNumber0() {
        testPosition(RoulleteBetPositions.number_0);
    }

    private void testPosition(final RoulleteBetPositions position) {
        PositionPayout positionPayout = RouletteGame.ALL.get(position);
        int payout = positionPayout.getPayout();
        final PlayerSession playerSession = Mockito.mock(PlayerSession.class);

        assertTrue(repeatConcurrently(payout * 100, new Runnable() {
            @Override
            public void run() {
                try {
                    game.getGameCommandInterpreter().interpretCommand(
                            Commands.mockCommandEvent(RouletteSpinRequest.cmd,
                                    RouletteSpinRequest.newBuilder().setPosition(position).setBet(BetWrapper.mock()).build(), playerSession));
                }
                catch ( Exception e ) {
                    Throwables.propagate(e);
                }
            }
        }).isEmpty());
        logger.debug("position={},payout={},actualPayout={}(:::) Wins={},Loses={}, totalWin={}", positionPayout.getPosition(),
                positionPayout.getPayout(), ctx.payout(), ctx.winsCount(), ctx.losesCount(), ctx.totalWin());
    }
}
