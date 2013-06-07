package com.katesoft.gserver.games;

import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.google.common.base.Optional;
import com.katesoft.gserver.api.BetWrapper;
import com.katesoft.gserver.commands.Commands.LoginCommand;
import com.katesoft.gserver.games.roullete.RoulleteCommands.RouletteSpinRequest;
import com.katesoft.gserver.games.roullete.RoulleteCommands.RoulleteBetPositions;
import com.katesoft.gserver.server.AbstractEmbeddedTest;

public class RoulleteEmbeddedTest extends AbstractEmbeddedTest {

    @Test
    public void works() throws InterruptedException, ExecutionException {
        Optional<LoginCommand> loginCommand = login();

        RoulleteBetPositions position = RoulleteBetPositions.values()[RoulleteBetPositions.values().length - 1];
        RouletteSpinRequest req = RouletteSpinRequest.newBuilder().setBet( BetWrapper.mock() ).setPosition( position ).build();
        c.callAsync( RouletteSpinRequest.cmd, req, loginCommand ).get().getExtension( RouletteSpinRequest.cmd );
    }
}
