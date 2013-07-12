package com.katesoft.gserver.domain;

import java.util.Date;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;

import com.katesoft.gserver.domain.support.RedisPersistentTokenBasedRememberMeServices;
import com.katesoft.gserver.domain.support.RedisPersistentTokenRepository;
import com.katesoft.gserver.domain.support.RedisUserDetailsService;

public abstract class AbstractDomainTest {
    protected static RedisPersistentTokenBasedRememberMeServices rememberMeServices;
    protected static JedisConnectionFactory cf;
    protected static StringRedisTemplate template;
    protected static RedisDomainRepository repo;
    protected static RedisPersistentTokenRepository tokenRepo;

    protected String loginToken;

    @BeforeClass
    public static void beforeClass() {
        cf = new JedisConnectionFactory();
        cf.afterPropertiesSet();

        template = new StringRedisTemplate( cf );
        template.afterPropertiesSet();

        repo = new RedisDomainRepository( template );
        tokenRepo = new RedisPersistentTokenRepository( template );

        RedisUserDetailsService userDetailsService = new RedisUserDetailsService( repo );
        rememberMeServices = new RedisPersistentTokenBasedRememberMeServices( "XXX", userDetailsService, tokenRepo ) {};
    }
    @AfterClass
    public static void afterClass() {
        cf.destroy();
    }
    @Before
    public void before() {
        template.execute( new RedisCallback<Void>() {
            @Override
            public Void doInRedis(RedisConnection connection) {
                connection.flushAll();
                return null;
            }
        } );

        UserAccountBO userAccount = new UserAccountBO();
        userAccount.setEnabled( true );
        userAccount.setUsername( "user-xxx" );
        userAccount.setFirstname( "Big" );
        userAccount.setLastname( "Boss" );
        userAccount.setPassword( "password-xxx" );
        userAccount.setProvider( "facebook" );
        userAccount.setProviderUserId( Long.valueOf( System.currentTimeMillis() ).toString() );
        userAccount.setEmail( "not-really@email.com" );

        repo.saveUserAccount( userAccount );

        String tokenSeries = rememberMeServices.generateSeriesData();
        String tokenValue = rememberMeServices.generateTokenData();
        
        String rememberMe = rememberMeServices.encodeCookie( new String[] { tokenSeries, tokenValue } );
        loginToken = rememberMeServices.encodeWebsocketLoginToken( rememberMe ).getToken();

        PersistentRememberMeToken token = new PersistentRememberMeToken( userAccount.getUsername(), tokenSeries, tokenValue, new Date() );
        tokenRepo.createNewToken( token );
    }
}
