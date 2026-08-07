package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.adapter.web.dto.favorite.FavoriteResponse;
import com.neopick.application.favorite.FavoriteUseCase;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/favorites")
public class FavoriteController {

    private final FavoriteUseCase favoriteUseCase;

    public FavoriteController(FavoriteUseCase favoriteUseCase) {
        this.favoriteUseCase = favoriteUseCase;
    }

    @PostMapping
    public ApiResponse<FavoriteResponse> add(@RequestBody Map<String, Long> body) {
        var fav = favoriteUseCase.add(body.get("teacherId"));
        return ApiResponse.success(FavoriteResponse.from(fav));
    }

    @DeleteMapping("/{teacherId}")
    public ApiResponse<Void> remove(@PathVariable Long teacherId) {
        favoriteUseCase.remove(teacherId);
        return ApiResponse.success();
    }

    @GetMapping
    public ApiResponse<List<FavoriteResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        var favs = favoriteUseCase.list(page, size);
        return ApiResponse.success(favs.stream().map(FavoriteResponse::from).toList());
    }

    @GetMapping("/check/{teacherId}")
    public ApiResponse<Map<String, Boolean>> check(@PathVariable Long teacherId) {
        return ApiResponse.success(Map.of("isFavorited", favoriteUseCase.isFavorited(teacherId)));
    }
}
