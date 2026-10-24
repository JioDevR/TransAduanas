package pe.sga.config;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import pe.sga.repository.UsuarioClienteRepository;
import pe.sga.repository.UsuarioRepository;

import java.io.IOException;
import java.util.Collections;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioClienteRepository usuarioClienteRepository;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String jwt = getJwtFromRequest(request);

        if (StringUtils.hasText(jwt) && tokenProvider.validateToken(jwt)) {
            String username = tokenProvider.getUsernameFromToken(jwt);
            Claims claims = tokenProvider.getClaimsFromToken(jwt);
            String tipoUsuario = claims.get("tipoUsuario", String.class);

            if ("CLIENTE_PORTAL".equals(tipoUsuario)) {
                usuarioClienteRepository.findByUsername(username).ifPresent(uc -> {
                    if (Boolean.TRUE.equals(uc.getEstadoActivo()) && uc.getCliente() != null && Boolean.TRUE.equals(uc.getCliente().getEstadoActivo())) {
                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority("ROLE_CLIENTE_PORTAL");
                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                uc,
                                null,
                                Collections.singletonList(authority)
                        );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                });
            } else {
                usuarioRepository.findByUsername(username).ifPresent(usuario -> {
                    if (Boolean.TRUE.equals(usuario.getEstadoActivo())) {
                        String roleName = "ROLE_" + usuario.getRol().name();
                        SimpleGrantedAuthority authority = new SimpleGrantedAuthority(roleName);

                        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                usuario,
                                null,
                                Collections.singletonList(authority)
                        );
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                });
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
