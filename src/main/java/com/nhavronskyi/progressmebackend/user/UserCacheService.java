package com.nhavronskyi.progressmebackend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final UserRepository userRepository;

    @Cacheable(value = "users", key = "#userId")
    public Optional<User> findById(UUID userId) {
        return userRepository.findById(userId)
                .map(UserCacheService::toDetachedCopy);
    }

    @CacheEvict(value = "users", key = "#userId")
    public void evict(UUID userId) {
        // evicts the cached user entry
    }

    private static User toDetachedCopy(User managed) {
        User copy = new User();
        copy.setId(managed.getId());
        copy.setEmail(managed.getEmail());
        copy.setName(managed.getName());
        copy.setAuthProvider(managed.getAuthProvider());
        copy.setProviderId(managed.getProviderId());
        copy.setAvatarUrl(managed.getAvatarUrl());
        copy.setCreatedAt(managed.getCreatedAt());
        copy.setUpdatedAt(managed.getUpdatedAt());
        // passwordHash intentionally excluded
        return copy;
    }
}
