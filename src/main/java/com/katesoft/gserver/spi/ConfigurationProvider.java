package com.katesoft.gserver.spi;

import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.domain.Entities.BetLimits;
import com.katesoft.gserver.domain.Entities.Coins;
import com.katesoft.gserver.domain.GameBO;

public interface ConfigurationProvider {
    BetLimits getBetLimits(GameBO g, Player p);
    Coins getCoins(GameBO g, Player p);
}
