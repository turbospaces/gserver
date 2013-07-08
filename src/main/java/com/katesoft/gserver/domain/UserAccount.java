package com.katesoft.gserver.domain;

import java.util.Collection;
import java.util.Collections;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@SuppressWarnings("serial")
public class UserAccount extends UserAccountBase implements UserDetails, BO {
    public UserAccount() {}
    public UserAccount(UserAccountBase accountBase) {
        super( accountBase );
    }
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singleton( new SimpleGrantedAuthority( "ROLE_USER" ) );
    }
    @Override
    public String getUsername() {
        return super.getUsername();
    }
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }
    @Override
    public String getPrimaryKey() {
        return getUsername();
    }
    public boolean isInternalAccount() {
        return getProviderType() == ProviderType.INTERNAL;
    }
}
