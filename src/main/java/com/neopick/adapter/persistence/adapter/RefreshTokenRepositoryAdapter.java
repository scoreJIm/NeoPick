package com.neopick.adapter.persistence.adapter;

import com.neopick.adapter.persistence.entity.RefreshTokenJpaEntity;
import com.neopick.adapter.persistence.repository.RefreshTokenJpaRepository;
import com.neopick.domain.auth.StoredRefreshToken;
import com.neopick.port.security.RefreshTokenRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Component
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {

    private final RefreshTokenJpaRepository jpaRepository;

    public RefreshTokenRepositoryAdapter(RefreshTokenJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void save(UUID userId, String tokenHash, String familyId, LocalDateTime expiresAt) {
        RefreshTokenJpaEntity entity = new RefreshTokenJpaEntity();
        entity.setId(UUID.randomUUID());
        entity.setUserId(userId);
        entity.setTokenHash(tokenHash);
        entity.setFamilyId(familyId);
        entity.setExpiresAt(expiresAt);
        entity.setRevoked(false);
        entity.setCreatedAt(LocalDateTime.now());
        jpaRepository.save(entity);
    }

    @Override
    public Optional<StoredRefreshToken> findByTokenHash(String tokenHash) {
        return jpaRepository.findByTokenHash(tokenHash).map(this::toDomain);
    }

    @Override
    public void markRevoked(String tokenHash, String replacedByTokenHash, LocalDateTime revokedAt) {
        jpaRepository.findByTokenHash(tokenHash).ifPresent(entity -> {
            entity.setRevoked(true);
            entity.setReplacedByToken(replacedByTokenHash);
            entity.setRevokedAt(revokedAt);
            jpaRepository.save(entity);
        });
    }

    @Override
    public void revokeAllByFamilyId(String familyId, LocalDateTime revokedAt) {
        var tokens = jpaRepository.findAllByFamilyId(familyId);
        for (RefreshTokenJpaEntity entity : tokens) {
            if (!entity.isRevoked()) {
                entity.setRevoked(true);
                entity.setRevokedAt(revokedAt);
                jpaRepository.save(entity);
            }
        }
    }

    @Override
    public void deleteAllByUserId(UUID userId) {
        jpaRepository.deleteByUserId(userId);
    }

    private StoredRefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return new StoredRefreshToken(
                entity.getId(),
                entity.getUserId(),
                entity.getTokenHash(),
                entity.getFamilyId(),
                entity.isRevoked(),
                entity.getReplacedByToken(),
                entity.getExpiresAt(),
                entity.getRevokedAt(),
                entity.getCreatedAt()
        );
    }
}
