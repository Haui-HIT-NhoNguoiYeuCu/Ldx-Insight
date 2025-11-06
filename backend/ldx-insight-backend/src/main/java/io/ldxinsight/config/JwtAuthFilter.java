package io.ldxinsight.config;

import io.ldxinsight.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie; // <-- Import Cookie
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value; // <-- Import Value
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    @Value("${jwt.cookie-name}")
    private String jwtCookieName; // Lấy tên cookie từ config

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        // 💡 SỬA ĐỔI: Không đọc từ Header, đọc từ Cookie
        final String jwt = extractJwtFromCookie(request);
        final String username;

        // 1. Nếu không có token, bỏ qua
        if (jwt == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Trích xuất username từ token
        username = jwtService.extractUsername(jwt);

        // 3. Nếu có username và user chưa được xác thực
        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            // 4. Nếu token hợp lệ
            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        // Chuyển tiếp request
        filterChain.doFilter(request, response);
    }

    /**
     * Hàm tiện ích để trích xuất JWT từ mảng cookie
     */
    private String extractJwtFromCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null; // Không có cookie nào
        }
        for (Cookie cookie : cookies) {
            if (jwtCookieName.equals(cookie.getName())) {
                return cookie.getValue(); // Tìm thấy cookie của chúng ta
            }
        }
        return null; // Không tìm thấy cookie
    }
}