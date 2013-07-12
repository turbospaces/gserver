package com.katesoft.gserver.domain.support;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.encrypt.TextEncryptor;
import org.springframework.security.web.authentication.rememberme.InvalidCookieException;
import org.springframework.security.web.authentication.rememberme.PersistentTokenBasedRememberMeServices;

import com.katesoft.gserver.core.Encryptors;
import com.katesoft.gserver.domain.WebsocketLoginToken;

public class RedisPersistentTokenBasedRememberMeServices extends PersistentTokenBasedRememberMeServices {
    // TODO: externalize password
    private final TextEncryptor encryptor = Encryptors.textEncryptor( getClass().getName(), false );
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
    @Override
    public String getCookieName() {
        return super.getCookieName();
    }
    public RedisPersistentTokenRepository getTokenRepository() {
        return tokenRepository;
    }
    public WebsocketLoginToken encodeWebsocketLoginToken(String rememberMeCookieValue) {
        WebsocketLoginToken wsLogin = new WebsocketLoginToken();
        wsLogin.setToken( encryptor.encrypt( rememberMeCookieValue ) );
        // TODO: set expiration
        wsLogin.setExpires( null );
        return wsLogin;
    }
    public String[] decodeWebsocketLoginToken(String value) {
        return decodeCookie( encryptor.decrypt( value ) );
    }
}
