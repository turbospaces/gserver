package com.katesoft.gserver.domain.support;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import com.google.common.base.Optional;
import com.katesoft.gserver.domain.RedisDomainRepository;
import com.katesoft.gserver.domain.UserAccountBO;

public class RedisUserDetailsService implements UserDetailsService {
    private final RedisDomainRepository repo;

    public RedisUserDetailsService(RedisDomainRepository repo) {
        this.repo = repo;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<UserAccountBO> opt = repo.findUserAccount( username );
        if ( opt.isPresent() ) {
            return opt.get();
        }
        throw new UsernameNotFoundException( String.format( "UserAccount=%s does not exist", username ) );
    }
}
