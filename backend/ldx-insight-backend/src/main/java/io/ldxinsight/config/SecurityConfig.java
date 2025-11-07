package io.ldxinsight.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final AuthenticationProvider authenticationProvider;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Cho phép error endpoint
                        .requestMatchers("/error").permitAll()
                        
                        // Cho phép OPTIONS requests
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() 
                        
                        // 1. Cho phép Swagger
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        
                        // 2. Cho phép API Đăng ký / Đăng nhập / Đăng xuất
                        .requestMatchers("/api/v1/auth/**").permitAll()

                        // 3. Cho phép các API CÔNG KHAI (tất cả methods)
                        .requestMatchers("/api/v1/datasets/**").permitAll()
                        .requestMatchers("/api/v1/stats/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/datasets/*/download.csv").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/datasets/*/download").permitAll()
                        
                        // 4. Tất cả các API còn lại đều phải xác thực
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"error\":\"Access denied\"}");
                        })
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        
        // Cho phép tất cả localhost ports và production domains
        cfg.setAllowedOriginPatterns(List.of(
            "http://localhost:*",
            "http://127.0.0.1:*",
            "https://haui-hit-h2k.site",
            "https://www.haui-hit-h2k.site"
        ));
        
        cfg.setAllowCredentials(true); // Bật credentials để hỗ trợ cookie
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS", "HEAD"));
        cfg.setAllowedHeaders(List.of("*")); 
        cfg.setExposedHeaders(List.of(
            "Authorization", 
            "Content-Disposition", 
            "X-Total-Count",
            "Set-Cookie",
            "Access-Control-Allow-Origin",
            "Access-Control-Allow-Credentials"
        ));
        cfg.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg); 
        return source;
    }
}