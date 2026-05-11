package com.tenantcore.coreservice.config;

import com.tenantcore.common.context.UserContext;
import com.tenantcore.common.security.SecurityConstants;
import com.tenantcore.coreservice.auth.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            if (authorization != null && authorization.startsWith(SecurityConstants.BEARER_PREFIX)) {
                String token = authorization.substring(SecurityConstants.BEARER_PREFIX.length()).trim();
                Claims claims = jwtService.parseToken(token);

                String userIdValue = claims.get(SecurityConstants.USER_ID_CLAIM, String.class);
                String tenantCode = claims.get(SecurityConstants.TENANT_CODE_CLAIM, String.class);
                String username = claims.get(SecurityConstants.USERNAME_CLAIM, String.class);
                List<String> roles = readStringListClaim(claims, SecurityConstants.ROLES_CLAIM);
                List<String> permissions = readStringListClaim(claims, SecurityConstants.PERMISSIONS_CLAIM);

                if (userIdValue != null && tenantCode != null && username != null) {
                    Collection<SimpleGrantedAuthority> authorities = new ArrayList<>();
                    for (String role : roles) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
                    }
                    for (String permission : permissions) {
                        authorities.add(new SimpleGrantedAuthority(permission));
                    }

                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            username,
                            null,
                            authorities
                    );
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    UserContext.setCurrentUser(new UserContext.CurrentUser(
                            UUID.fromString(userIdValue),
                            tenantCode,
                            username,
                            username,
                            Set.copyOf(roles),
                            Set.copyOf(permissions)
                    ));
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            SecurityContextHolder.clearContext();
            UserContext.clear();
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }

    @SuppressWarnings("unchecked")
    private List<String> readStringListClaim(Claims claims, String claimName) {
        Object value = claims.get(claimName);
        if (value == null) {
            return List.of();
        }
        if (value instanceof List<?> rawList) {
            List<String> result = new ArrayList<>();
            for (Object item : rawList) {
                if (item != null) {
                    result.add(item.toString());
                }
            }
            return result;
        }
        return List.of();
    }
}
