package com.katesoft.gserver.domain;

import static com.google.common.base.Objects.toStringHelper;

import java.util.Date;

public class WebsocketLoginToken {
    /**
     * when the web-socket login token to be considered as expired.
     */
    private Date expires;
    /**
     * server URL.
     */
    private String url;
    /**
     * token value.
     */
    private String token;

    public String getUrl() {
        return url;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public String getToken() {
        return token;
    }
    public void setToken(String token) {
        this.token = token;
    }
    public Date getExpires() {
        return expires;
    }
    public void setExpires(Date expires) {
        this.expires = expires;
    }
    @Override
    public String toString() {
        return toStringHelper( this ).add( "token", getToken() ).add( "expires", getExpires() ).add( "url", getUrl() ).toString();
    }
}
