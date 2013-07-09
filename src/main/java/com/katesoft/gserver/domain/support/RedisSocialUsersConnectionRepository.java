package com.katesoft.gserver.domain.support;

import static com.katesoft.gserver.domain.support.RedisSocialConnectionRepository.fromHashOperations;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.util.List;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionData;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionKey;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;

import com.google.common.base.Optional;
import com.katesoft.gserver.domain.RedisNamingConvention;
import com.katesoft.gserver.domain.support.RedisSocialConnectionRepository.ConnectionDataBOAdapter;

public class RedisSocialUsersConnectionRepository implements UsersConnectionRepository {
    private final TextEncryptor textEncryptor;
    private final ConnectionFactoryLocator connectionFactoryLocator;
    private final StringRedisTemplate template;
    private final RedisNamingConvention connectionDataNamingConvention;

    public RedisSocialUsersConnectionRepository(StringRedisTemplate template,
                                                ConnectionFactoryLocator connectionFactoryLocator,
                                                TextEncryptor textEncryptor) {
        this.template = template;
        this.connectionFactoryLocator = connectionFactoryLocator;
        this.textEncryptor = textEncryptor;
        this.connectionDataNamingConvention = new RedisNamingConvention( template, ConnectionData.class );
    }
    @Override
    public List<String> findUserIdsWithConnection(Connection<?> connection) {
        ConnectionKey key = connection.getKey();
        String pk = RedisSocialConnectionRepository.toPrimaryKey( key.getProviderId(), key.getProviderUserId() );
        Optional<ConnectionDataBOAdapter> opt = connectionDataNamingConvention.findByPrimaryKey( pk, fromHashOperations( textEncryptor ) );
        if ( opt.isPresent() ) {
            return singletonList( opt.get().getUser() );
        }
        return emptyList();
    }
    @Override
    public Set<String> findUserIdsConnectedTo(String providerId, Set<String> providerUserIds) {
        throw new UnsupportedOperationException();
    }
    @Override
    public ConnectionRepository createConnectionRepository(String userId) {
        return new RedisSocialConnectionRepository( connectionDataNamingConvention, template, connectionFactoryLocator, textEncryptor, userId );
    }
}
