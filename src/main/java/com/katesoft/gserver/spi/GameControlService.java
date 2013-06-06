package com.katesoft.gserver.spi;

import com.katesoft.gserver.api.Player;
import com.katesoft.gserver.api.PlayerSession;

public interface GameControlService {
    PlayerSession openOrRestorePlayerSession(String gameId, Player player);
}
