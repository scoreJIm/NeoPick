package com.neopick.adapter.web.security;

import com.neopick.port.security.TokenProvider;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@DisplayName("JWT Authentication Filter")
class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;
    private TokenProvider tokenProvider;
    private SecurityContextHolder securityContextHolder;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        tokenProvider = mock(TokenProvider.class);
        securityContextHolder = mock(SecurityContextHolder.class);
        filter = new JwtAuthenticationFilter(tokenProvider, securityContextHolder);
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
    }

    @Nested
    @DisplayName("Valid Bearer token")
    class ValidToken {

        @Test
        @DisplayName("should set authentication for valid token")
        void shouldSetAuthentication() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
            when(tokenProvider.validateToken("valid-token")).thenReturn(true);
            when(tokenProvider.getUserIdFromToken("valid-token")).thenReturn("user-1");
            when(tokenProvider.getRoleFromToken("valid-token")).thenReturn("STUDENT");

            filter.doFilterInternal(request, response, filterChain);

            verify(securityContextHolder).setAuthentication("user-1", "STUDENT");
            verify(filterChain).doFilter(request, response);
            verify(securityContextHolder).clear();
        }
    }

    @Nested
    @DisplayName("Invalid token")
    class InvalidToken {

        @Test
        @DisplayName("should not set auth for invalid token")
        void shouldNotSetAuthForInvalid() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer invalid-token");
            when(tokenProvider.validateToken("invalid-token")).thenReturn(false);

            filter.doFilterInternal(request, response, filterChain);

            verify(securityContextHolder, never()).setAuthentication(anyString(), anyString());
            verify(filterChain).doFilter(request, response);
        }
    }

    @Nested
    @DisplayName("Missing or malformed header")
    class MissingHeader {

        @Test
        @DisplayName("should pass through when no auth header")
        void shouldPassThroughNoHeader() throws Exception {
            when(request.getHeader("Authorization")).thenReturn(null);

            filter.doFilterInternal(request, response, filterChain);

            verify(securityContextHolder, never()).setAuthentication(anyString(), anyString());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("should pass through when header is not Bearer")
        void shouldPassThroughNonBearer() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Basic dXNlcjpwYXNz");

            filter.doFilterInternal(request, response, filterChain);

            verify(securityContextHolder, never()).setAuthentication(anyString(), anyString());
        }

        @Test
        @DisplayName("should not parse empty Bearer prefix")
        void shouldNotParseEmptyBearer() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer ");

            filter.doFilterInternal(request, response, filterChain);

            verify(tokenProvider, never()).validateToken(anyString());
        }
    }

    @Nested
    @DisplayName("Exception safety")
    class ExceptionSafety {

        @Test
        @DisplayName("should clear context even if filter chain throws")
        void shouldClearContextOnException() throws Exception {
            when(request.getHeader("Authorization")).thenReturn("Bearer valid-token");
            when(tokenProvider.validateToken("valid-token")).thenReturn(true);
            when(tokenProvider.getUserIdFromToken("valid-token")).thenReturn("user-1");
            when(tokenProvider.getRoleFromToken("valid-token")).thenReturn("STUDENT");
            doThrow(new ServletException("downstream error"))
                    .when(filterChain).doFilter(request, response);

            try {
                filter.doFilterInternal(request, response, filterChain);
            } catch (ServletException ignored) {
            }

            verify(securityContextHolder).clear();
        }
    }
}
