package com.neopick.adapter.web.controller;

import com.neopick.adapter.web.dto.common.ApiResponse;
import com.neopick.adapter.web.dto.favorite.FavoriteResponse;
import com.neopick.application.favorite.FavoriteUseCase;
import com.neopick.infrastructure.ratelimit.RateLimit;
import io.micrometer.core.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/favorites")
@Tag(name = "Favorites", description = "Teacher favorites management: add, remove, list, and check status")
@SecurityRequirement(name = "bearerAuth")
public class FavoriteController {

    private final FavoriteUseCase favoriteUseCase;

    public FavoriteController(FavoriteUseCase favoriteUseCase) {
        this.favoriteUseCase = favoriteUseCase;
    }

    @PostMapping
    @RateLimit(limit = 20, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.favorites.add", description = "Add favorite")
    @Operation(summary = "Add teacher to favorites", description = "Adds a teacher to the authenticated student's favorites list.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Teacher added to favorites"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Missing teacherId in request body", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Teacher not found", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Already favorited", content = @Content)
    })
    public ApiResponse<FavoriteResponse> add(@RequestBody Map<String, Long> body) {
        var fav = favoriteUseCase.add(body.get("teacherId"));
        return ApiResponse.success(FavoriteResponse.from(fav));
    }

    @DeleteMapping("/{teacherId}")
    @RateLimit(limit = 20, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.favorites.remove", description = "Remove favorite")
    @Operation(summary = "Remove teacher from favorites", description = "Removes a teacher from the authenticated student's favorites list.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Teacher removed from favorites"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Favorite record not found", content = @Content)
    })
    public ApiResponse<Void> remove(
            @Parameter(description = "Teacher ID to remove from favorites") @PathVariable Long teacherId) {
        favoriteUseCase.remove(teacherId);
        return ApiResponse.success();
    }

    @GetMapping
    @RateLimit(limit = 30, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.favorites.list", description = "List favorites")
    @Operation(summary = "List favorite teachers", description = "Returns the authenticated student's favorited teachers, paginated.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Favorites returned successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ApiResponse<List<FavoriteResponse>> list(
            @Parameter(description = "Zero-based page index") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") int size) {
        var favs = favoriteUseCase.list(page, size);
        return ApiResponse.success(favs.stream().map(FavoriteResponse::from).toList());
    }

    @GetMapping("/check/{teacherId}")
    @RateLimit(limit = 30, windowSeconds = 60, scope = "USER")
    @Timed(value = "neopick.favorites.check", description = "Check favorite status")
    @Operation(summary = "Check favorite status", description = "Checks whether a specific teacher is in the authenticated student's favorites list.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Favorite status returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated", content = @Content)
    })
    public ApiResponse<Map<String, Boolean>> check(
            @Parameter(description = "Teacher ID to check") @PathVariable Long teacherId) {
        return ApiResponse.success(Map.of("isFavorited", favoriteUseCase.isFavorited(teacherId)));
    }
}
