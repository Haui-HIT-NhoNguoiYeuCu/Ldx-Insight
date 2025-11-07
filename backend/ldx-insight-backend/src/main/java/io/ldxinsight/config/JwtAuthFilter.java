package io.ldxinsight.config;

import io.ldxinsight.service.JwtService;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Value("${jwt.cookie-name}")
    private String jwtCookieName;

    // Các prefix/route public – chỉ để skip parsing nặng khi KHÔNG có token
    private static final Set<String> PUBLIC_PREFIXES = new HashSet<>(Arrays.asList(
            "/swagger-ui/", "/v3/api-docs", "/api/v1/auth/", "/api/v1/stats", "/api/v1/datasets"
    ));

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // Nếu đã có Authentication thì bỏ qua (ví dụ filter khác đã set)
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Thử đọc JWT từ Cookie trước, rồi đến Authorization header
        String jwt = extractJwtFromCookie(request);
        if (jwt == null) {
            jwt = extractJwtFromAuthorizationHeader(request);
        }

        // Không có token: để cho chain tiếp tục (endpoints public sẽ pass do SecurityConfig)
        if (jwt == null || jwt.isBlank()) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            final String username = jwtService.extractUsername(jwt);
            if (username != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.isTokenValid(jwt, userDetails)) {
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    userDetails, null, userDetails.getAuthorities());

                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } else {
                    log.debug("[JWT] Token invalid for user: {}", username);
                }
            }
        } catch (ExpiredJwtException e) {
            log.debug("[JWT] Token expired: {}", e.getMessage());
            // (Tuỳ bạn) Có thể set header để FE biết và tự logout/refresh:
            // response.setHeader("X-JWT-Status", "expired");
        } catch (SignatureException e) {
            log.warn("[JWT] Invalid signature: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.warn("[JWT] Malformed token: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("[JWT] General JWT error: {}", e.getMessage());
        } catch (Exception e) {
            log.error("[JWT] Unexpected error", e);
        }

        filterChain.doFilter(request, response);
    }

    private String extractJwtFromCookie(HttpServletRequest request) {
        if (jwtCookieName == null) return null;
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;

        for (Cookie cookie : cookies) {
            if (jwtCookieName.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private String extractJwtFromAuthorizationHeader(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null) return null;
        if (!header.startsWith("Bearer ")) return null;
        return header.substring(7);
    }

    
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Chỉ skip khi không có Authorization & không có cookie token
        boolean hasAnyToken = extractJwtFromCookie(request) != null
                || extractJwtFromAuthorizationHeader(request) != null;
        if (hasAnyToken) return false;
        return PUBLIC_PREFIXES.stream().anyMatch(path::startsWith);
    }
}
