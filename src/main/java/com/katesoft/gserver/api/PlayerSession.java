package com.katesoft.gserver.api;

import java.io.Closeable;

import com.katesoft.gserver.domain.Entities.BetLimits;
import com.katesoft.gserver.domain.Entities.Coins;
import com.katesoft.gserver.domain.GameBO;

public interface PlayerSession extends Closeable {
    /**
     * get unique identifier of player session.</p>
     * if there are several instances of game play server, game play can be continued on any of the nodes (precondition:
     * player session information has been stored in external data store like redis or relation database). </p>
     * 
     * @return player unique session id.
     */
    String id();

    /**
     * given 1-to-many mapping between user connection and player sessions, get physical connection link.</p>
     * user can trigger several game play sessions for different games from one connection (that's the valid case). </p>
     * 
     * @return associated game.
     */
    UserConnection getAssociatedUserConnection();

    /**
     * get the game instance created for corresponding {@link GameBO}.</p>
     * 
     * @return game instance.
     */
    Game getAssociatedGame();

    /**
     * @return the associated game definition from which the game has been created.
     */
    GameBO getAssociatedGameDefinition();

    /**
     * given 1-to-many mapping between player and player sessions (even that player can have multiple player session
     * across all game servers ), get associated player.</p>
     * 
     * @return player.
     */
    Player getPlayer();

    /**
     * get the unmodifiable bet limits for this session(meaning that limits are being assigned to session during
     * opening game play session(i.e. game login) and can't be changed until player finishes game play even if they are
     * being changed in original data store). </p>
     * 
     * @return bet limits that have been assigned to player session.
     */
    BetLimits getBetLimits();

    /**
     * get the unmodifiable coins configuration for this session(meaning that coins configuration are being assigned to
     * session during opening game play session(i.e. game login) and can't be changed until player finishes game play
     * even if they are being changed in original data store). </p>
     * 
     * @return coins configuration that has been assigned to player session.
     */
    Coins getCoins();

    /**
     * close player session, persist the necessary data if needed in order to continue
     * game play.
     */
    @Override
    void close();
}
