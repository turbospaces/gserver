package com.katesoft.gserver.domain.support;

import java.util.List;

import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionData;
import org.springframework.social.connect.ConnectionFactory;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionKey;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.util.MultiValueMap;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.katesoft.gserver.domain.BO;
import com.katesoft.gserver.domain.RedisNamingConvention;

public class RedisSocialConnectionRepository implements ConnectionRepository {
    private final Logger logger = LoggerFactory.getLogger( getClass() );
    private final TextEncryptor textEncryptor;
    private final RedisNamingConvention connectionDataNamingConvention;
    private final String userId;
    private final ConnectionFactoryLocator connectionFactoryLocator;

    public RedisSocialConnectionRepository(RedisNamingConvention namingConvention,
                                           StringRedisTemplate template,
                                           ConnectionFactoryLocator connectionFactoryLocator,
                                           TextEncryptor textEncryptor,
                                           String userId) {
        this.connectionFactoryLocator = connectionFactoryLocator;
        this.connectionDataNamingConvention = namingConvention;
        this.textEncryptor = textEncryptor;
        this.userId = userId;
    }
    @Override
    public void updateConnection(Connection<?> connection) {
        ConnectionDataBOAdapter bo = new ConnectionDataBOAdapter( connection.createData(), textEncryptor, userId );
        connectionDataNamingConvention.update( bo.getPrimaryKey(), bo.toHashOperations() );
    }
    @Override
    public void addConnection(Connection<?> connection) {
        ConnectionDataBOAdapter bo = new ConnectionDataBOAdapter( connection.createData(), textEncryptor, userId );
        logger.debug( "adding social connection={} to={} ", ReflectionToStringBuilder.toString( bo.data ), connection.getKey() );
        connectionDataNamingConvention.save( bo.getPrimaryKey(), bo.toHashOperations() );
    }
    @Override
    public void removeConnection(ConnectionKey connectionKey) {
        String pk = toPrimaryKey( connectionKey.getProviderId(), connectionKey.getProviderUserId() );
        connectionDataNamingConvention.deleteByPrimaryKey( pk );
    }
    @Override
    public Connection<?> getConnection(ConnectionKey connectionKey) {
        String pk = toPrimaryKey( connectionKey.getProviderId(), connectionKey.getProviderUserId() );

        Optional<ConnectionDataBOAdapter> opt = connectionDataNamingConvention.findByPrimaryKey( pk, fromHashOperations( textEncryptor ) );
        if ( opt.isPresent() ) {
            ConnectionFactory<?> connectionFactory = connectionFactoryLocator.getConnectionFactory( connectionKey.getProviderId() );
            return connectionFactory.createConnection( opt.get().data );
        }
        return null;
    }
    @Override
    public MultiValueMap<String, Connection<?>> findAllConnections() {
        throw new UnsupportedOperationException();
    }
    @Override
    public List<Connection<?>> findConnections(String providerId) {
        throw new UnsupportedOperationException();
    }
    @Override
    public <A> List<Connection<A>> findConnections(Class<A> apiType) {
        throw new UnsupportedOperationException();
    }
    @Override
    public MultiValueMap<String, Connection<?>> findConnectionsToUsers(MultiValueMap<String, String> providerUserIds) {
        throw new UnsupportedOperationException();
    }
    @Override
    public <A> Connection<A> getConnection(Class<A> apiType, String providerUserId) {
        throw new UnsupportedOperationException();
    }
    @Override
    public <A> Connection<A> getPrimaryConnection(Class<A> apiType) {
        throw new UnsupportedOperationException();
    }
    @Override
    public <A> Connection<A> findPrimaryConnection(Class<A> apiType) {
        throw new UnsupportedOperationException();
    }
    @Override
    public void removeConnections(String providerId) {
        throw new UnsupportedOperationException();
    }

    static String toPrimaryKey(String providerId, String providerUserId) {
        return providerId + "-" + providerUserId;
    }

    static Function<BoundHashOperations<String, String, String>, ConnectionDataBOAdapter> fromHashOperations(final TextEncryptor encryptor) {
        return new Function<BoundHashOperations<String, String, String>, ConnectionDataBOAdapter>() {
            @Override
            public ConnectionDataBOAdapter apply(BoundHashOperations<String, String, String> ops) {
                String userId = ops.get( "user_id" );
                String providerId = ops.get( "provider_id" );
                String providerUserId = ops.get( "provider_user_id" );
                String displayName = ops.get( "display_name" );
                String profileUrl = ops.get( "profile_url" );
                String imageUrl = ops.get( "image_url" );
                String accessToken = encryptor.decrypt( ops.get( "access_token" ) );
                String secret = encryptor.decrypt( ops.get( "secret" ) );
                String refreshToken = encryptor.decrypt( ops.get( "refresh_token" ) );
                Long expire = Long.parseLong( ops.get( "expire_time" ) );
                if ( expire == 0 ) {
                    expire = null;
                }

                ConnectionData cd = new ConnectionData(
                        providerId,
                        providerUserId,
                        displayName,
                        profileUrl,
                        imageUrl,
                        accessToken,
                        secret,
                        refreshToken,
                        expire );

                return new ConnectionDataBOAdapter( cd, encryptor, userId );
            }
        };
    }

    static class ConnectionDataBOAdapter implements BO {
        private final ConnectionData data;
        private final String user;
        private final TextEncryptor encryptor;

        private ConnectionDataBOAdapter(ConnectionData data, TextEncryptor textEncryptor, String userId) {
            this.data = data;
            this.encryptor = textEncryptor;
            this.user = userId;
        }
        @Override
        public String getPrimaryKey() {
            return toPrimaryKey( data.getProviderId(), data.getProviderUserId() );
        }
        public String getUser() {
            return user;
        }

        public Function<BoundHashOperations<String, String, String>, Void> toHashOperations() {
            return new Function<BoundHashOperations<String, String, String>, Void>() {
                @Override
                public Void apply(BoundHashOperations<String, String, String> ops) {
                    ops.put( "user_id", user );
                    ops.put( "provider_id", data.getProviderId() );
                    ops.put( "provider_user_id", data.getProviderUserId() );
                    if ( data.getDisplayName() != null ) {
                        ops.put( "display_name", data.getDisplayName() );
                    }
                    if ( data.getProfileUrl() != null ) {
                        ops.put( "profile_url", data.getProfileUrl() );
                    }
                    if ( data.getImageUrl() != null ) {
                        ops.put( "image_url", data.getImageUrl() );
                    }
                    if ( data.getAccessToken() != null ) {
                        ops.put( "access_token", encryptor.encrypt( data.getAccessToken() ) );
                    }
                    if ( data.getSecret() != null ) {
                        ops.put( "secret", encryptor.encrypt( data.getSecret() ) );
                    }
                    if ( data.getRefreshToken() != null ) {
                        ops.put( "refresh_token", encryptor.encrypt( data.getRefreshToken() ) );
                    }
                    if ( data.getExpireTime() != null ) {
                        ops.put( "expire_time", data.getExpireTime().toString() );
                    }
                    return null;
                }
            };
        }
    }
}
