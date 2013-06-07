package com.katesoft.gserver.api;

import static com.google.common.base.Objects.toStringHelper;
import static com.katesoft.gserver.api.GamePlayContext.MOCK;

import com.katesoft.gserver.commands.Commands.Bet;
import com.katesoft.gserver.commands.Commands.Coin;
import com.katesoft.gserver.commands.Commands.CoinQuantitySlice;

public class BetWrapper {
    private final Bet bet;
    private final boolean win;

    public BetWrapper(Bet bet, boolean win) {
        this.bet = bet;
        this.win = win;
    }

    public int betAmountUnsigned() {
        int betAmount = 0;
        for ( CoinQuantitySlice slice : bet.getSlicesList() ) {
            Coin coin = slice.getCoin();
            Integer quantity = slice.getQuantity();
            switch ( coin ) {
                case ONE:
                    betAmount += 1 * quantity;
                    break;
                case TWO:
                    betAmount += 2 * quantity;
                    break;
                case FIVE:
                    betAmount += 5 * quantity;
                    break;
                case TEN:
                    betAmount += 10 * quantity;
                    break;
                case TWENTY_FIVE:
                    betAmount += 25 * quantity;
                    break;
                case FIFTY:
                    betAmount += 50 * quantity;
                    break;
                case HUNDRED:
                    betAmount += 100 * quantity;
                    break;
                default:
                    break;
            }
        }
        return betAmount;
    }

    public boolean isWin() {
        return win;
    }

    @Override
    public String toString() {
        return toStringHelper( this ).add( "betAmount", betAmountUnsigned() ).add( "bet", bet ).add( "win", isWin() ).toString();
    }

    public static Bet mock() {
        return Bet
                .newBuilder()
                .addSlices( CoinQuantitySlice.newBuilder().setQuantity( MOCK.rng().nextInt( 3 ) ).setCoin( Coin.TEN ).build() )
                .addSlices( CoinQuantitySlice.newBuilder().setQuantity( MOCK.rng().nextInt( 17 ) ).setCoin( Coin.FIVE ).build() )
                .addSlices( CoinQuantitySlice.newBuilder().setQuantity( MOCK.rng().nextInt( 53 ) ).setCoin( Coin.TWO ).build() )
                .addSlices( CoinQuantitySlice.newBuilder().setQuantity( MOCK.rng().nextInt( 145 ) ).setCoin( Coin.ONE ).build() )
                .build();
    }
}
