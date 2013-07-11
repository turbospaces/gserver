package com.katesoft.gserver.domain;

import java.util.Iterator;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.collections.DefaultRedisList;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSet.Builder;
import com.googlecode.protobuf.format.JsonFormat;
import com.googlecode.protobuf.format.JsonFormat.ParseException;
import com.katesoft.gserver.domain.Entities.BetLimits;
import com.katesoft.gserver.domain.Entities.Coins;

public class RedisDomainRepository {
    private final RedisNamingConvention accountNamingConvention, gameNamingConvention, playerSessionConvention;
    private final DefaultRedisList<String> userAccounts, games;

    public RedisDomainRepository(StringRedisTemplate template) {
        accountNamingConvention = new RedisNamingConvention( template, UserAccountBO.class );
        gameNamingConvention = new RedisNamingConvention( template, GameBO.class );
        playerSessionConvention = new RedisNamingConvention( template, PlayerSessionBO.class );

        userAccounts = accountNamingConvention.newEntitiesList();
        games = gameNamingConvention.newEntitiesList();
    }

    public void saveGame(final GameBO game) throws ConcurrencyFailureException, DuplicateKeyException {
        Long id = gameNamingConvention.save( game.getPrimaryKey(), new Function<BoundHashOperations<String, String, String>, Void>() {
            @Override
            public Void apply(BoundHashOperations<String, String, String> ops) {
                ops.put( "shortcut", game.getPrimaryKey() );
                ops.put( "display_name", game.getDisplayName() );
                ops.put( "game_class_name", game.getGameClassName() );
                return null;
            }
        } );
        games.add( id.toString() );
    }
    public void saveUserAccount(final UserAccountBO account) throws ConcurrencyFailureException, DuplicateKeyException {
        Long id = accountNamingConvention.save( account.getPrimaryKey(), new Function<BoundHashOperations<String, String, String>, Void>() {
            @Override
            public Void apply(BoundHashOperations<String, String, String> ops) {
                ops.put( "provider", account.getProvider() );
                if ( account.getProviderUserId() != null ) {
                    ops.put( "provider_user_id", account.getProviderUserId() );
                }
                ops.put( "first_name", account.getFirstname() );
                ops.put( "last_name", account.getLastname() );
                ops.put( "user_name", account.getUsername() );
                ops.put( "email", account.getEmail() );
                ops.put( "password", account.getPassword() );
                ops.put( "enabled", Boolean.toString( account.isEnabled() ) );
                return null;
            }
        } );
        userAccounts.add( id.toString() );
    }
    public void savePlayerSession(final PlayerSessionBO playerSession) {
        playerSessionConvention.save( playerSession.getPrimaryKey(), new Function<BoundHashOperations<String, String, String>, Void>() {
            @Override
            public Void apply(BoundHashOperations<String, String, String> ops) {
                ops.put( "session_id", playerSession.sessionId );
                ops.put( "user_connection_id", playerSession.userConnectionId );
                ops.put( "player_id", playerSession.playerId );
                ops.put( "game_id", playerSession.game.getPrimaryKey() );
                ops.put( "bet_limits", JsonFormat.printToString( playerSession.betLimits ) );
                ops.put( "coins", JsonFormat.printToString( playerSession.coins ) );
                return null;
            }
        } );
    }
    public Optional<PlayerSessionBO> findPlayerSession(String pk) {
        return playerSessionConvention.findByPrimaryKey( pk, new Function<BoundHashOperations<String, String, String>, PlayerSessionBO>() {
            @Override
            public PlayerSessionBO apply(BoundHashOperations<String, String, String> ops) {
                for ( ;; ) {
                    try {
                        String sessionId = ops.get( "session_id" );
                        String connectionId = ops.get( "user_connection_id" );
                        String playerId = ops.get( "player_id" );
                        GameBO game = findGame( ops.get( "game_id" ) ).get();

                        BetLimits.Builder betLimits = BetLimits.newBuilder();
                        Coins.Builder coins = Coins.newBuilder();

                        JsonFormat.merge( ops.get( "bet_limits" ), betLimits );
                        JsonFormat.merge( ops.get( "coins" ), coins );

                        return new PlayerSessionBO( sessionId, playerId, connectionId, betLimits.build(), coins.build(), game );
                    }
                    catch ( ParseException e ) {
                        Throwables.propagate( e );
                    }
                }
            }
        } );
    }
    public Optional<GameBO> findGame(String pk) {
        return gameNamingConvention.findByPrimaryKey( pk, GAME_MAPPER );
    }
    public Optional<UserAccountBO> findUserAccount(final String pk) {
        return accountNamingConvention.findByPrimaryKey( pk, USER_ACCOUNT_MAPPER );
    }
    public ImmutableSet<GameBO> findAllGames() {
        Builder<GameBO> b = ImmutableSet.builder();
        for ( Iterator<String> it = games.iterator(); it.hasNext(); ) {
            Long generatedId = Long.parseLong( it.next() );
            b.add( gameNamingConvention.findByGeneratedId( generatedId, GAME_MAPPER ) );
        }
        return b.build();
    }
    public void deleteUserAccount(final UserAccountBO account) {
        Long generatedId = accountNamingConvention.deleteByPrimaryKey( account.getPrimaryKey() );
        userAccounts.remove( generatedId.toString() );
    }
    public static <T extends BO> T required(Optional<T> opt, Class<T> bo, Object pk) throws EmptyResultDataAccessException {
        if ( opt.isPresent() ) {
            return opt.get();
        }
        String msg = String.format( "unable to find %s definition by key=%s", bo.getSimpleName(), pk );
        throw new EmptyResultDataAccessException( msg, 1 );
    }

    //
    // MAPPER(s)
    //
    private static final Function<BoundHashOperations<String, String, String>, GameBO> GAME_MAPPER = new Function<BoundHashOperations<String, String, String>, GameBO>() {
        @Override
        public GameBO apply(BoundHashOperations<String, String, String> ops) {
            return new GameBO( ops.get( "shortcut" ), ops.get( "display_name" ), ops.get( "game_class_name" ) );
        }
    };
    private static final Function<BoundHashOperations<String, String, String>, UserAccountBO> USER_ACCOUNT_MAPPER = new Function<BoundHashOperations<String, String, String>, UserAccountBO>() {
        @Override
        public UserAccountBO apply(BoundHashOperations<String, String, String> userOps) {
            UserAccountBO account = new UserAccountBO();
            account.setProvider( userOps.get( "provider" ) );
            account.setProviderUserId( userOps.get( "provider_user_id" ) );
            account.setFirstname( userOps.get( "first_name" ) );
            account.setLastname( userOps.get( "last_name" ) );
            account.setUsername( userOps.get( "user_name" ) );
            account.setEmail( userOps.get( "email" ) );
            account.setPassword( userOps.get( "password" ) );
            account.setEnabled( Boolean.parseBoolean( userOps.get( "enabled" ) ) );
            return account;
        }
    };
}
