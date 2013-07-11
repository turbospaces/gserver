package com.katesoft.gserver.domain;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Suppliers.memoize;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.support.atomic.RedisAtomicLong;
import org.springframework.data.redis.support.collections.DefaultRedisList;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Supplier;

public class RedisNamingConvention {
    private final Logger logger = LoggerFactory.getLogger( getClass() );
    private final StringRedisTemplate template;
    private final ValueOperations<String, String> opsForValue;
    private final String namespace;
    private final Supplier<RedisAtomicLong> idSequence = memoize( new Supplier<RedisAtomicLong>() {
        @Override
        public RedisAtomicLong get() {
            return new RedisAtomicLong( "seq_" + namespace, template.getConnectionFactory() );
        }
    } );

    public RedisNamingConvention(StringRedisTemplate template, Class<?> persistentClass) {
        this.template = template;
        this.opsForValue = template.opsForValue();

        String simpleName = persistentClass.getSimpleName().toLowerCase();
        int idx = simpleName.indexOf( "bo" );
        if ( idx > 0 ) {
            this.namespace = simpleName.substring( 0, idx );
        }
        else {
            this.namespace = simpleName;
        }
    }
    public Long
            save(String pk, Function<BoundHashOperations<String, String, String>, Void> mapper) throws ConcurrencyFailureException, DuplicateKeyException {
        String x = toPrimaryKey( pk );

        if ( opsForValue.get( x ) == null ) {
            Long id = idSequence.get().incrementAndGet();
            boolean added = opsForValue.setIfAbsent( x, id.toString() );

            if ( added ) {
                BoundHashOperations<String, String, String> ops = template.boundHashOps( toEntityUid( id.toString() ) );
                mapper.apply( ops );
                return id;
            }
            else {
                String exception = String.format( "detected concurrent modification exception for key = %s", x );
                logger.warn( exception );
                throw new ConcurrencyFailureException( exception );
            }
        }

        throw new DuplicateKeyException( x );
    }
    public void update(String pk, Function<BoundHashOperations<String, String, String>, Void> mapper) throws ConcurrencyFailureException {
        String x = toPrimaryKey( pk );
        String entityUid = opsForValue.get( x );
        if ( entityUid != null ) {
            BoundHashOperations<String, String, String> ops = template.boundHashOps( toEntityUid( entityUid ) );
            mapper.apply( ops );
        }
        else {
            String exception = String.format( "object with primary key = {} has been updated or deleted in another transaction", x );
            logger.warn( exception );
            throw new ConcurrencyFailureException( exception );
        }
    }
    public <T extends BO> Optional<T> findByPrimaryKey(String pk, Function<BoundHashOperations<String, String, String>, T> mapper) {
        checkNotNull( pk, "primary key not provided" );

        String x = toPrimaryKey( pk );
        String uid = opsForValue.get( x );
        T entity = null;

        if ( uid != null ) {
            entity = findByGeneratedId( Long.parseLong( uid ), mapper );
        }

        return Optional.fromNullable( entity );
    }
    public <T extends BO> T findByGeneratedId(Long generatedId, Function<BoundHashOperations<String, String, String>, T> mapper) {
        BoundHashOperations<String, String, String> ops = template.boundHashOps( toEntityUid( generatedId.toString() ) );
        return mapper.apply( ops );
    }
    public String findFieldValueByGeneratedId(String generatedId, Function<BoundHashOperations<String, String, String>, String> mapper) {
        BoundHashOperations<String, String, String> ops = template.boundHashOps( toEntityUid( generatedId ) );
        return mapper.apply( ops );
    }
    public void
            deleteByGeneratedId(String generatedId, Function<BoundHashOperations<String, String, String>, String> idMapper) throws ConcurrencyFailureException {
        String idFieldValue = findFieldValueByGeneratedId( generatedId, idMapper );
        deleteByPrimaryKey( idFieldValue );
    }
    public void deleteByPrimaryKey(String pk) throws ConcurrencyFailureException {
        String x = toPrimaryKey( pk );
        String entityUid = opsForValue.get( x );
        if ( entityUid != null ) {
            BoundHashOperations<String, String, String> ops = template.boundHashOps( toEntityUid( entityUid ) );
            Set<String> keys = ops.keys();
            for ( String key : keys ) {
                ops.delete( key );
            }
            template.delete( x );
        }
        else {
            String exception = String.format( "object with primary key = {} has been deleted in another transaction", x );
            logger.warn( exception );
            throw new ConcurrencyFailureException( exception );
        }
    }
    //
    // naming conventions
    //
    public DefaultRedisList<String> newEntitiesList() {
        return new DefaultRedisList<String>( namespace + "s", template );
    }
    public DefaultRedisList<String> newList(String pk, String namespaceX) {
        return new DefaultRedisList<String>( this.namespace + ":" + pk + ":" + namespaceX, template );
    }
    private String toPrimaryKey(String pk) {
        return namespace + ":" + pk + ":id";
    }
    private String toEntityUid(String entityUid) {
        return namespace + ":" + entityUid;
    }
}
