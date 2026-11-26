package pe.sga.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import pe.sga.entity.Usuario;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration-ms:86400000}")
    private long jwtExpirationMs;

    @jakarta.annotation.PostConstruct
    public void validateConfiguration() {
        getSigningKey();
    }

    private SecretKey getSigningKey() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("JWT_SECRET es obligatorio y debe contener al menos 32 bytes aleatorios");
        }
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Usuario usuario) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(usuario.getUsername())
                .claim("tipoUsuario", "PERSONAL_INTERNO")
                .claim("idUsuario", usuario.getIdUsuario())
                .claim("rol", usuario.getRol().name())
                .claim("correo", usuario.getCorreoInstitucional())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public String generateTokenForPortal(pe.sga.entity.UsuarioCliente usuarioCliente) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        return Jwts.builder()
                .subject(usuarioCliente.getUsername())
                .claim("tipoUsuario", "CLIENTE_PORTAL")
                .claim("idUsuarioCliente", usuarioCliente.getIdUsuarioCliente())
                .claim("idCliente", usuarioCliente.getCliente().getIdCliente())
                .claim("ruc", usuarioCliente.getCliente().getNumeroDocumento())
                .claim("razonSocial", usuarioCliente.getCliente().getRazonSocial())
                .claim("correo", usuarioCliente.getCorreoElectronico())
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
    }

    public Claims getClaimsFromToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getUsernameFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}
