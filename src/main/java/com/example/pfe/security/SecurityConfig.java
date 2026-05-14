package com.example.pfe.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.Arrays;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter) {
        this.jwtAuthFilter = jwtAuthFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> {
                    // ── Public auth endpoints only ────────────────────────
                    auth.requestMatchers("/auth/login", "/auth/forgot-password").permitAll();

                    // ── /auth/me requires a valid token ───────────────────
                    auth.requestMatchers(HttpMethod.GET, "/auth/me").authenticated();

                    // ── Camunda / WebSocket ───────────────────────────────
                    auth.requestMatchers("/camunda/**", "/engine-rest/**").permitAll();
                    auth.requestMatchers("/ws/**").permitAll();
                    auth.requestMatchers("/api/camunda/**").permitAll();
                    auth.requestMatchers("/api/camunda/download/**").authenticated();

                    // ── Notifications ─────────────────────────────────────
                    auth.requestMatchers("/api/notifications/**").authenticated();

                    // ── /api/users/me — any authenticated role ────────────
                    auth.requestMatchers(HttpMethod.GET, "/api/users/me").authenticated();
                    auth.requestMatchers(HttpMethod.PUT, "/api/users/me").authenticated();

                    // ── /api/users/** — ADMIN and CHEF_EQUIPE only ────────
                    auth.requestMatchers("/api/users/**")
                            .hasAnyAuthority("ROLE_ADMIN", "ROLE_CHEF_EQUIPE");

                    // ── Everything else requires authentication ────────────
                    auth.anyRequest().authenticated();
                })
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(Arrays.asList("http://localhost:4200"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(Arrays.asList("*", "Content-Type", "Authorization"));
        configuration.setExposedHeaders(Arrays.asList("Authorization"));
        configuration.setAllowCredentials(true);
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}