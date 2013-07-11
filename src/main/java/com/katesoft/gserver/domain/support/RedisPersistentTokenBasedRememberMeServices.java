package com.katesoft.gserver.domain.support;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;

public class RedisPersistentTokenBasedRememberMeServices extends PersistentTokenBasedRememberMeServices {
    private final RedisPersistentTokenRepository tokenRepository;

    public RedisPersistentTokenBasedRememberMeServices(String key,
                                                       UserDetailsService userDetailsService,
                                                       RedisPersistentTokenRepository tokenRepository) {
        super( key, userDetailsService, tokenRepository );
        this.tokenRepository = tokenRepository;
    }

    @Override
    public String generateSeriesData() {
        return super.generateSeriesData();
    }
    @Override
    public String generateTokenData() {
        return super.generateTokenData();
    }
    @Override
    public String[] decodeCookie(String cookieValue) throws InvalidCookieException {
        return super.decodeCookie( cookieValue );
    }
    @Override
    public String encodeCookie(String[] cookieTokens) {
        return super.encodeCookie( cookieTokens );
    }
    public RedisPersistentTokenRepository getTokenRepository() {
        return tokenRepository;
    }
}
