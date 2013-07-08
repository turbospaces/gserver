package com.katesoft.gserver.domain.support;

import java.util.List;
import java.util.Set;

import org.springframework.social.connect.Connection;
import org.springframework.social.connect.ConnectionRepository;
import org.springframework.social.connect.UsersConnectionRepository;

public class RedisSocialUsersConnectionRepository implements UsersConnectionRepository {

    @Override
    public List<String> findUserIdsWithConnection(Connection<?> connection) {
        return null;
    }
    @Override
    public Set<String> findUserIdsConnectedTo(String providerId, Set<String> providerUserIds) {
        return null;
    }
    @Override
    public ConnectionRepository createConnectionRepository(String userId) {
        return null;
    }
}
