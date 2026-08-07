package com.neopick.application.favorite;

import com.neopick.domain.favorite.Favorite;
import com.neopick.domain.favorite.FavoriteRepository;
import com.neopick.port.security.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class FavoriteUseCase {

    private final FavoriteRepository favoriteRepository;
    private final SecurityContext securityContext;

    public FavoriteUseCase(FavoriteRepository favoriteRepository, SecurityContext securityContext) {
        this.favoriteRepository = favoriteRepository;
        this.securityContext = securityContext;
    }

    @Transactional
    public Favorite add(Long teacherId) {
        String userId = securityContext.requireCurrentUserId();
        if (favoriteRepository.exists(userId, teacherId)) {
            throw new IllegalStateException("Already favorited");
        }
        return favoriteRepository.save(new Favorite(userId, teacherId));
    }

    @Transactional
    public void remove(Long teacherId) {
        String userId = securityContext.requireCurrentUserId();
        favoriteRepository.delete(userId, teacherId);
    }

    public List<Favorite> list(int page, int size) {
        String userId = securityContext.requireCurrentUserId();
        return favoriteRepository.findByStudentId(userId, page, size);
    }

    public boolean isFavorited(Long teacherId) {
        String userId = securityContext.requireCurrentUserId();
        return favoriteRepository.exists(userId, teacherId);
    }
}
