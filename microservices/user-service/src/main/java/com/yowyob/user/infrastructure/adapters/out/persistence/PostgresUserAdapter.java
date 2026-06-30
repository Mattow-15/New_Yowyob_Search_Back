package com.yowyob.user.infrastructure.adapters.out.persistence;

import com.yowyob.user.application.ports.out.SearchHistoryRepositoryPort;
import com.yowyob.user.application.ports.out.UserRepositoryPort;
import com.yowyob.user.domain.model.SearchHistory;
import com.yowyob.user.domain.model.UserProfile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class PostgresUserAdapter implements UserRepositoryPort, SearchHistoryRepositoryPort {

    private final UserProfileJpaRepository userProfileJpaRepository;
    private final SearchHistoryJpaRepository searchHistoryJpaRepository;

    // --- UserProfilePort implementation ---

    @Override
    @Transactional(readOnly = true)
    public Optional<UserProfile> findByUserId(UUID userId) {
        return userProfileJpaRepository.findByUserId(userId)
                .map(this::toDomain);
    }

    @Override
    @Transactional
    public UserProfile save(UserProfile userProfile) {
        UserProfileJpaEntity entity = toEntity(userProfile);
        UserProfileJpaEntity savedEntity = userProfileJpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfile> findAll() {
        return userProfileJpaRepository.findAll().stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserProfile> findByUpdatedAtAfter(LocalDateTime updatedAfter) {
        return userProfileJpaRepository.findByUpdatedAtAfter(updatedAfter).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    // --- SearchHistoryPort implementation ---

    @Override
    @Transactional
    public SearchHistory save(SearchHistory searchHistory) {
        SearchHistoryJpaEntity entity = toEntity(searchHistory);
        SearchHistoryJpaEntity savedEntity = searchHistoryJpaRepository.save(entity);
        return toDomain(savedEntity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchHistory> findByUserIdOrderBySearchedAtDesc(UUID userId) {
        return searchHistoryJpaRepository.findByUserIdOrderBySearchedAtDesc(userId).stream()
                .map(this::toDomain)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteByUserId(UUID userId) {
        searchHistoryJpaRepository.deleteByUserId(userId);
    }

    // --- Mappings ---

    private UserProfile toDomain(UserProfileJpaEntity entity) {
        if (entity == null) return null;
        return UserProfile.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .email(entity.getEmail())
                .firstName(entity.getFirstName())
                .lastName(entity.getLastName())
                .bio(entity.getBio())
                .phoneNumber(entity.getPhoneNumber())
                .address(entity.getAddress())
                .city(entity.getCity())
                .country(entity.getCountry())
                .avatarUrl(entity.getAvatarUrl())
                .socialLinksJson(entity.getSocialLinksJson())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    private UserProfileJpaEntity toEntity(UserProfile domain) {
        if (domain == null) return null;
        return UserProfileJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .email(domain.getEmail())
                .firstName(domain.getFirstName())
                .lastName(domain.getLastName())
                .bio(domain.getBio())
                .phoneNumber(domain.getPhoneNumber())
                .address(domain.getAddress())
                .city(domain.getCity())
                .country(domain.getCountry())
                .avatarUrl(domain.getAvatarUrl())
                .socialLinksJson(domain.getSocialLinksJson())
                .createdAt(domain.getCreatedAt())
                .updatedAt(domain.getUpdatedAt())
                .build();
    }

    private SearchHistory toDomain(SearchHistoryJpaEntity entity) {
        if (entity == null) return null;
        return SearchHistory.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .query(entity.getQuery())
                .searchedAt(entity.getSearchedAt())
                .build();
    }

    private SearchHistoryJpaEntity toEntity(SearchHistory domain) {
        if (domain == null) return null;
        return SearchHistoryJpaEntity.builder()
                .id(domain.getId())
                .userId(domain.getUserId())
                .query(domain.getQuery())
                .searchedAt(domain.getSearchedAt())
                .build();
    }
}
