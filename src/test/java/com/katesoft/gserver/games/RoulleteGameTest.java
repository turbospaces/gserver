package com.katesoft.gserver.games;

import org.junit.Before;
import org.junit.Test;

import com.katesoft.gserver.api.BetWrapper;
import com.katesoft.gserver.api.CommandWrapperEvent;
import com.katesoft.gserver.api.GamePlayContext;
import com.katesoft.gserver.games.roullete.RoulleteCommands.RouletteSpinRequest;
import com.katesoft.gserver.games.roullete.RoulleteCommands.RoulleteBetPositions;

public class RoulleteGameTest {
	RouletteGame game;

	@Before
	public void setup() {
		game = new RouletteGame();
		game.init(GamePlayContext.MOCK);
	}

	@Test
	public void testPositionsWithNumber0() throws Exception {
		game.getGameCommandInterpreter().interpretCommand(
				CommandWrapperEvent.mock(
						RouletteSpinRequest.cmd,
						RouletteSpinRequest.newBuilder()
								.setPosition(RoulleteBetPositions.number_0)
								.setBet(BetWrapper.mock()).build()));
	}
}
