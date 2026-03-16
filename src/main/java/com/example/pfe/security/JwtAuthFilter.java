package com.example.pfe.security;

import com.example.pfe.models.User;
import com.example.pfe.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserRepository userRepository;

    public JwtAuthFilter(JwtService jwtService, UserRepository userRepository) {
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        try {
            String authHeader = request.getHeader("Authorization");
            System.out.println("=== JWT FILTER ===");
            System.out.println("Auth Header: " + authHeader);

            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                System.out.println("No Bearer token, continuing filter chain");
                filterChain.doFilter(request, response);
                return;
            }

            String token = authHeader.substring(7);
            System.out.println("Token: " + token.substring(0, Math.min(20, token.length())) + "...");

            String email = jwtService.extractEmail(token);
            System.out.println("Email from token: " + email);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // Find user in database
                var userOptional = userRepository.findByEmail(email);

                if (userOptional.isPresent()) {
                    User user = userOptional.get();
                    System.out.println("User found in DB: " + user.getEmail() + " with role: " + user.getRole());

                    // Validate token
                    if (jwtService.isTokenValid(token, user)) {
                        String role = jwtService.extractRole(token);
                        System.out.println("Role from token: " + role);
                        System.out.println("Creating auth with ROLE_" + role);

                        UsernamePasswordAuthenticationToken authToken =
                                new UsernamePasswordAuthenticationToken(
                                        email,
                                        null,
                                        List.of(new SimpleGrantedAuthority("ROLE_" + role))
                                );

                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        System.out.println("Authentication set in context");
                        System.out.println("Authorities: " + authToken.getAuthorities());
                    } else {
                        System.out.println("Token validation failed");
                    }
                } else {
                    System.out.println("User not found in DB for email: " + email);
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            System.err.println("JWT authentication error: " + e.getMessage());
            e.printStackTrace();
            filterChain.doFilter(request, response);
        }
    }
}