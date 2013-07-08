package com.katesoft.gserver.core;

import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

public class ServerBootstrap {
    private JedisConnectionFactory redisConnectionFactory;

    public ServerBootstrap() {}
    protected void initDataAccessLayer() {
        redisConnectionFactory = new JedisConnectionFactory();
        redisConnectionFactory.afterPropertiesSet();
        StringRedisTemplate redisTemplate = new StringRedisTemplate();
        redisTemplate.setConnectionFactory( redisConnectionFactory );
        redisTemplate.setExposeConnection( true );
        redisTemplate.afterPropertiesSet();
    }
    protected void initServices() {}
    protected void startTransport() {}
}
