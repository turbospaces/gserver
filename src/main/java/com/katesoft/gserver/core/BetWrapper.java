package com.katesoft.gserver.core;

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
    public int betAmount() {
        int betAmount = 0;
        for ( CoinQuantitySlice slice : bet.getSlicesList() ) {
            Coin coin = slice.getCoin();
            Integer quantity = slice.getQuantity();
            betAmount += coin.getNumber() * quantity;
        }
        return betAmount;
    }
    public boolean isWin() {
        return win;
    }
}
