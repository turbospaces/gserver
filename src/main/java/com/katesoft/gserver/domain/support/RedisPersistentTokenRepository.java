package com.katesoft.gserver.domain.support;

import java.util.Date;
import java.util.Iterator;

import org.springframework.data.redis.core.BoundHashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.support.collections.DefaultRedisList;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;
import org.springframework.security.web.authentication.rememberme.PersistentTokenRepository;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.katesoft.gserver.domain.BO;
import com.katesoft.gserver.domain.RedisNamingConvention;

public class RedisPersistentTokenRepository implements PersistentTokenRepository {
    private final RedisNamingConvention namingConvention;

    public RedisPersistentTokenRepository(StringRedisTemplate template) {
        namingConvention = new RedisNamingConvention(template, PersistentRememberMeToken.class);
    }

    @Override
    public void createNewToken(final PersistentRememberMeToken token) {
        PersistentRememberMeTokenBOAdapter bo = new PersistentRememberMeTokenBOAdapter(token);
        long id = namingConvention.save(bo.getPrimaryKey(), new Function<BoundHashOperations<String, String, String>, Void>() {
            @Override
            public Void apply(BoundHashOperations<String, String, String> ops) {
                ops.put("user_name", token.getUsername());
                ops.put("token_value", token.getTokenValue());
                ops.put("series", token.getSeries());
                ops.put("date", String.valueOf(token.getDate().getTime()));
                return null;
            }
        });
        namingConvention.newList(token.getUsername(), "series").add(String.valueOf(id));
    }
    @Override
    public void updateToken(String series, final String tokenValue, final Date lastUsed) {
        namingConvention.update(series, new Function<BoundHashOperations<String, String, String>, Void>() {
            @Override
            public Void apply(BoundHashOperations<String, String, String> ops) {
                ops.put("token_value", tokenValue);
                ops.put("date", String.valueOf(lastUsed.getTime()));
                return null;
            }
        });
    }
    @Override
    public PersistentRememberMeToken getTokenForSeries(String seriesId) {
        Optional<PersistentRememberMeTokenBOAdapter> opt = namingConvention.findByPrimaryKey(seriesId, PersistentRememberMeTokenBOAdapter.fromHashOps());
        if (opt.isPresent()) {
            return opt.get().token;
        }
        return null;
    }
    @Override
    public void removeUserTokens(String username) {
        DefaultRedisList<String> series = namingConvention.newList(username, "series");
        for (Iterator<String> it = series.iterator(); it.hasNext(); ) {
            namingConvention.deleteByGeneratedId(it.next(), new Function<BoundHashOperations<String, String, String>, String>() {
                @Override
                public String apply(BoundHashOperations<String, String, String> ops) {
                    return ops.get("series");
                }
            });
            it.remove();
        }
    }

    private static class PersistentRememberMeTokenBOAdapter implements BO {
        private final PersistentRememberMeToken token;

        private PersistentRememberMeTokenBOAdapter(PersistentRememberMeToken token) {
            this.token = token;
        }

        @Override
        public String getPrimaryKey() {
            return token.getSeries();
        }

        public static Function<BoundHashOperations<String, String, String>, PersistentRememberMeTokenBOAdapter> fromHashOps() {
            return new Function<BoundHashOperations<String, String, String>, PersistentRememberMeTokenBOAdapter>() {
                @Override
                public PersistentRememberMeTokenBOAdapter apply(BoundHashOperations<String, String, String> ops) {
                    PersistentRememberMeToken token = new PersistentRememberMeToken(
                            ops.get("user_name"),
                            ops.get("series"),
                            ops.get("token_value"),
                            new Date(Long.parseLong(ops.get("date")))
                    );
                    return new PersistentRememberMeTokenBOAdapter(token);
                }
            };
        }
    }
}
