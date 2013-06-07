package com.katesoft.gserver.api;

import static com.katesoft.gserver.api.GamePlayContext.MOCK;

import com.google.common.base.Objects;
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
		for (CoinQuantitySlice slice : bet.getSlicesList()) {
			Coin coin = slice.getCoin();
			Integer quantity = slice.getQuantity();
			betAmount += coin.getNumber() * quantity;
		}
		return betAmount;
	}

	public boolean isWin() {
		return win;
	}

	@Override
	public String toString() {
		return Objects.toStringHelper(this)
				.add("betAmount", betAmountUnsigned()).add("bet", bet)
				.add("win", isWin()).toString();
	}

	public static Bet mock() {
		return Bet
				.newBuilder()
				.addSlices(
						CoinQuantitySlice.newBuilder()
								.setQuantity(MOCK.rng().nextInt(17))
								.setCoin(Coin.TEN).build())
				.addSlices(
						CoinQuantitySlice.newBuilder()
								.setQuantity(MOCK.rng().nextInt(27))
								.setCoin(Coin.FIVE).build())
				.addSlices(
						CoinQuantitySlice.newBuilder()
								.setQuantity(MOCK.rng().nextInt(53))
								.setCoin(Coin.TWO).build())
				.addSlices(
						CoinQuantitySlice.newBuilder()
								.setQuantity(MOCK.rng().nextInt(145))
								.setCoin(Coin.ONE).build()).build();
	}
}
