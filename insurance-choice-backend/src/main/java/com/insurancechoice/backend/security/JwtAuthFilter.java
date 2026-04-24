package com.insurancechoice.backend.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    @Autowired
    private JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtUtil.validateToken(token)) {
                String email        = jwtUtil.extractEmail(token);
                String userObjectId = jwtUtil.extractUserObjectId(token);
                List<String> roles  = jwtUtil.extractRoles(token);

                List<SimpleGrantedAuthority> authorities = roles == null
                        ? List.of()
                        : roles.stream().map(SimpleGrantedAuthority::new).collect(Collectors.toList());

                // principal = email, credentials = userObjectId (accessible in controllers)
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(email, userObjectId, authorities);

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        chain.doFilter(request, response);
    }
}
