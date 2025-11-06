package io.ldxinsight.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
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
                        // 1. Cho phép Swagger
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                        // 2. Cho phép API Đăng ký / Đăng nhập / Đăng xuất
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        // 3. Cho phép các API CÔNG KHAI
                        .requestMatchers(HttpMethod.GET, "/api/v1/datasets").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/datasets/{id}").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/datasets/category/{category}").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/datasets/{id}/view").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/datasets/{id}/download").permitAll()
                        .requestMatchers("/api/v1/stats/**").permitAll()
                        // 4. Tất cả các API còn lại đều phải xác thực
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        // 💡 ĐÃ XÓA DÒNG .httpBasic()

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of(
                "http://localhost:3000",          // Nuxt.js local
                "http://localhost:8080",          // Swagger local
                "https://api.haui-hit-h2k.site",  // Domain API 
                "https://haui-hit-h2k.site"      // Domain Frontend
        ));
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","OPTIONS","PATCH"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}