package id.careerfund.api.utils.filters;

import id.careerfund.api.domains.models.JwtConfig;
import id.careerfund.api.services.JwtServiceImpl;
import id.careerfund.api.services.UserServiceImpl;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class JwtFilter extends OncePerRequestFilter {
    private final JwtConfig jwtConfig;

    private final JwtServiceImpl jwtServiceImpl;

    private final UserServiceImpl userService;

    public JwtFilter(JwtConfig jwtConfig, JwtServiceImpl jwtServiceImpl, UserServiceImpl userService) {
        this.jwtConfig = jwtConfig;
        this.jwtServiceImpl = jwtServiceImpl;
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) {
        try {
            String token = null;
            String email = null;

            String authorizationHeader = request.getHeader("Authorization");
            if (authorizationHeader != null && authorizationHeader.startsWith(jwtConfig.getTokenPrefix())) {
                token = authorizationHeader.substring(7);
                email = jwtServiceImpl.extractUsername(token);
            }

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userService.loadUserByUsername(email);
                if (jwtServiceImpl.validateToken(token, userDetails)) {
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    usernamePasswordAuthenticationToken
                            .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);
                }
            }
            filterChain.doFilter(request, response);
        } catch (MalformedJwtException | SignatureException e) {
            response.setContentType("application/json");
            try {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token is not as expected");
            } catch (IOException ex) {
                ex.printStackTrace();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
        } catch (InvalidBearerTokenException e) {
            response.setContentType("application/json");
            try {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token has expired");
            } catch (IOException ex) {
                ex.printStackTrace();
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
        } catch (Exception e) {
            e.printStackTrace();
            response.setContentType("application/json");
            try {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "There is something wrong with your JWT");
            } catch (IOException ex) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            }
        }
    }
}
