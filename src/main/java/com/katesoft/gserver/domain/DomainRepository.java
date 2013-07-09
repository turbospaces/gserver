package com.katesoft.gserver.domain;

import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.collections.DefaultRedisList;

import com.google.common.base.Function;
import com.google.common.base.Optional;

public class DomainRepository {
    private final RedisNamingConvention accountNamingConvention;
    private final DefaultRedisList<String> userAccounts;

    public DomainRepository(StringRedisTemplate template) {
        accountNamingConvention = new RedisNamingConvention( template, UserAccount.class );
        userAccounts = accountNamingConvention.newEntitiesList();
    }

    public void saveUserAccount(final UserAccount account) throws ConcurrencyFailureException, DuplicateKeyException {
        long id = accountNamingConvention.save( account.getPrimaryKey(), new Function<BoundHashOperations<String, String, String>, Void>() {
            @Override
            public Void apply(BoundHashOperations<String, String, String> userOps) {
                userOps.put( "provider", account.getProvider() );
                if ( account.getProviderUserId() != null ) {
                    userOps.put( "provider_user_id", account.getProviderUserId() );
                }
                userOps.put( "first_name", account.getFirstname() );
                userOps.put( "last_name", account.getLastname() );
                userOps.put( "user_name", account.getUsername() );
                userOps.put( "email", account.getEmail() );
                userOps.put( "password", account.getPassword() );
                userOps.put( "enabled", Boolean.toString( account.isEnabled() ) );
                return null;
            }
        } );
        userAccounts.add( String.valueOf( id ) );
    }
    public Optional<UserAccount> findUserAccount(final String pk) {
        return accountNamingConvention.findByPrimaryKey( pk, new Function<BoundHashOperations<String, String, String>, UserAccount>() {
            @Override
            public UserAccount apply(BoundHashOperations<String, String, String> userOps) {
                UserAccount account = new UserAccount();
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
        } );
    }
    public void deleteUserAccount(final UserAccount account) {
        accountNamingConvention.deleteByPrimaryKey( account.getPrimaryKey() );
    }
}
