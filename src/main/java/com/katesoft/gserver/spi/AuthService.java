package com.katesoft.gserver.spi;

import com.katesoft.gserver.api.Player;

public interface AuthService {
    Player login(String playerId, String credentials);
    void logout(Player player);
}
