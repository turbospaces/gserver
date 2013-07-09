package com.katesoft.gserver.domain.support;

import static java.lang.System.currentTimeMillis;
import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.util.ReflectionUtils.findField;
import static org.springframework.util.ReflectionUtils.makeAccessible;
import static org.springframework.util.ReflectionUtils.setField;

import java.lang.reflect.Field;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.encrypt.Encryptors;
import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionData;
import org.springframework.social.connect.ConnectionFactory;
import org.springframework.social.connect.ConnectionFactoryLocator;
import org.springframework.social.connect.ConnectionKey;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.support.AbstractConnection;

@SuppressWarnings({ "unchecked", "rawtypes" })
public class RedisSocialUsersConnectionRepositoryTest {
    JedisConnectionFactory cf;
    StringRedisTemplate template;
    RedisSocialUsersConnectionRepository repo;
    ConnectionFactoryLocator locator;

    @Before
    public void before() {
        cf = new JedisConnectionFactory();
        cf.afterPropertiesSet();

        locator = mock( ConnectionFactoryLocator.class );
        when( locator.getConnectionFactory( "facebook" ) ).thenReturn( new ConnectionFactory( "facebook", null, null ) {
            @Override
            public Connection createConnection(final ConnectionData data) {
                return new AbstractConnection( data, null ) {
                    @Override
                    public Object getApi() {
                        return null;
                    }
                    @Override
                    public ConnectionData createData() {
                        return data;
                    }
                };
            }
        } );

        template = new StringRedisTemplate( cf );
        template.afterPropertiesSet();
        template.execute( new RedisCallback<Void>() {
            @Override
            public Void doInRedis(RedisConnection connection) throws DataAccessException {
                connection.flushAll();
                return null;
            }
        } );

        repo = new RedisSocialUsersConnectionRepository( template, locator, Encryptors.noOpText() );
    }

    @After
    public void after() {
        cf.destroy();
    }
    @Test
    public void test() throws SecurityException, IllegalArgumentException {
        String profileUserId = String.valueOf( currentTimeMillis() );
        ConnectionRepository cr = repo.createConnectionRepository( "gserver" );
        Connection<?> connection = mock( Connection.class );
        ConnectionData data = new ConnectionData(
                "facebook",
                profileUserId,
                "gsever-display-name",
                "http://facebook.com/id" + profileUserId,
                "http://facebook.com/id_img_" + profileUserId,
                "some-access-token",
                "some-secret",
                "some-refresh-token",
                Long.MAX_VALUE );
        when( connection.createData() ).thenReturn( data );
        cr.addConnection( connection );

        Field f = findField( ConnectionData.class, "secret" );
        makeAccessible( f );
        setField( f, data, "secret-x" );

        cr.updateConnection( connection );
        AbstractConnection c = (AbstractConnection) cr.getConnection( new ConnectionKey( "facebook", profileUserId ) );
        ConnectionData clone = c.createData();

        assertEquals( data.getAccessToken(), clone.getAccessToken() );
        assertEquals( data.getDisplayName(), clone.getDisplayName() );
        assertEquals( data.getExpireTime(), clone.getExpireTime() );
        assertEquals( data.getImageUrl(), clone.getImageUrl() );
        assertEquals( data.getProfileUrl(), clone.getProfileUrl() );
        assertEquals( data.getProviderId(), clone.getProviderId() );
        assertEquals( data.getProviderUserId(), clone.getProviderUserId() );
        assertEquals( data.getRefreshToken(), clone.getRefreshToken() );
        assertEquals( data.getSecret(), clone.getSecret() );
    }
}
