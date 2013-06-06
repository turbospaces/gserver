package com.katesoft.gserver.games;

import java.util.concurrent.ExecutionException;

import org.junit.Test;

import com.google.common.base.Optional;
import com.katesoft.gserver.commands.Commands.Bet;
import com.katesoft.gserver.commands.Commands.Coin;
import com.katesoft.gserver.commands.Commands.CoinQuantitySlice;
import com.katesoft.gserver.commands.Commands.LoginCommand;
import com.katesoft.gserver.games.roullete.RoulleteCommands.RouletteSpinRequest;
import com.katesoft.gserver.games.roullete.RoulleteCommands.RoulleteBetPositions;
import com.katesoft.gserver.server.AbstractEmbeddedTest;

public class RoulleteTest extends AbstractEmbeddedTest {

    @Test
    public void works() throws InterruptedException, ExecutionException {
        Optional<LoginCommand> loginCommand = login();

        RoulleteBetPositions position = RoulleteBetPositions.values()[RoulleteBetPositions.values().length - 1];
        Bet bet = Bet
                .newBuilder()
                .addSlices( CoinQuantitySlice.newBuilder().setQuantity( RND.nextInt( 10 ) ).setCoin( Coin.TEN ).build() )
                .addSlices( CoinQuantitySlice.newBuilder().setQuantity( RND.nextInt( 20 ) ).setCoin( Coin.FIVE ).build() )
                .addSlices( CoinQuantitySlice.newBuilder().setQuantity( RND.nextInt( 50 ) ).setCoin( Coin.TWO ).build() )
                .addSlices( CoinQuantitySlice.newBuilder().setQuantity( RND.nextInt( 100 ) ).setCoin( Coin.ONE ).build() )
                .build();

        RouletteSpinRequest req = RouletteSpinRequest.newBuilder().setBet( bet ).setPosition( position ).build();
        c.callAsync( RouletteSpinRequest.cmd, req, loginCommand ).get().getExtension( RouletteSpinRequest.cmd );
    }
}
