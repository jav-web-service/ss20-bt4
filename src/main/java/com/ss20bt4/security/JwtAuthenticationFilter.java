package com.ss20bt4.security;

import com.ss20bt4.entity.StudentToken;
import com.ss20bt4.repository.StudentTokenRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final StudentTokenRepository studentTokenRepository;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userEmail;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);
        try {
            userEmail = jwtService.extractUsername(jwt);

            if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userEmail);

                // Here we can either check if the token itself is in the DB (if we saved it), 
                // or we extract the session ID. 
                // A common implementation for this specific requirement when only Refresh Tokens are saved
                // is to check if the student has ANY valid token. 
                // Let's get the token ID from claims if present, or just verify the user's tokens.
                // We will implement saving the Access Token itself to the DB to make it simple and strictly follow "chặn các token...".
                // Actually, let's extract "tokenId" from the JWT claims which points to the StudentToken id.
                
                Long tokenId = jwtService.extractClaim(jwt, claims -> claims.get("tokenId", Long.class));
                boolean isRevoked = true;
                
                if (tokenId != null) {
                    StudentToken token = studentTokenRepository.findById(tokenId).orElse(null);
                    if (token != null) {
                        isRevoked = token.getIsRevoked();
                    }
                } else {
                    // Fallback: If no tokenId in claims, just check if the exact JWT is saved in DB and valid.
                    // Or we could check if user has valid tokens.
                    StudentToken token = studentTokenRepository.findByTokenString(jwt).orElse(null);
                    if (token != null) {
                        isRevoked = token.getIsRevoked() || token.getIsExpired();
                    } else {
                        // If we decided to save Access Tokens to DB, and it's not there, it's invalid.
                        // For flexibility, let's assume if it's not in DB, we check if the user has valid tokens
                        // as a fallback for the "session" concept.
                        var validTokens = studentTokenRepository.findAllValidTokensByStudentId(((CustomUserDetails) userDetails).getStudent().getId());
                        isRevoked = validTokens.isEmpty();
                    }
                }

                if (jwtService.isTokenValid(jwt, userDetails) && !isRevoked) {
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
        } catch (Exception e) {
            // Token parsing failed
        }
        
        filterChain.doFilter(request, response);
    }
}
