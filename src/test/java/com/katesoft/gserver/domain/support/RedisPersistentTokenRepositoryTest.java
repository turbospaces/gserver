package com.katesoft.gserver.domain.support;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.junit.Test;
import org.springframework.security.web.authentication.rememberme.PersistentRememberMeToken;

import com.katesoft.gserver.domain.AbstractDomainTest;

public class RedisPersistentTokenRepositoryTest extends AbstractDomainTest {
    @Test
    public void works() {
        PersistentRememberMeToken token1 = new PersistentRememberMeToken("gserver", "series1", "token-value1", new Date());
        PersistentRememberMeToken token2 = new PersistentRememberMeToken("gserver", "series2", "token-value2", new Date());
        tokenRepo.createNewToken(token1);
        tokenRepo.createNewToken(token2);
        PersistentRememberMeToken clone1 = tokenRepo.getTokenForSeries("series1");
        PersistentRememberMeToken clone2 = tokenRepo.getTokenForSeries("series2");

        assertTrue(EqualsBuilder.reflectionEquals(token1, clone1, false));
        assertTrue(EqualsBuilder.reflectionEquals(token2, clone2, false));

        tokenRepo.updateToken("series2", "token-value2-updated", new Date());
        clone2 = tokenRepo.getTokenForSeries("series2");

        assertEquals("token-value2-updated", clone2.getTokenValue());

        tokenRepo.removeUserTokens("gserver");

        assertTrue(tokenRepo.getTokenForSeries("series1") == null);
        assertTrue(tokenRepo.getTokenForSeries("series2") == null);
    }
}
