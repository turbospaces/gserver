package com.katesoft.gserver.domain;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.google.common.base.Optional;

public class RedisUserDetailsService implements UserDetailsService {
    private final DomainRepository repo;

    public RedisUserDetailsService(DomainRepository repo) {
        this.repo = repo;
    }
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserAccount> opt = repo.findUserAccount( username );
        if ( opt.isPresent() ) {
            return opt.get();
        }
        throw new UsernameNotFoundException( String.format( "user with username=%s not found", username ) );
    }
}
