package pe.sga.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.test.util.ReflectionTestUtils;
import pe.sga.entity.Cliente;
import pe.sga.entity.Usuario;
import pe.sga.entity.UsuarioCliente;
import pe.sga.entity.enums.RolUsuario;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas Unitarias - JwtTokenProvider")
@Tag("unitaria")
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtSecret", "QA_UNIT_SECRET_KEY_2026_SUPER_SEGURA_32_BYTES_MIN");
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", 3600000L);
    }

    @Test
    @DisplayName("Normal: genera y valida token para personal interno con claims esperados")
    void generateToken_usuarioInternoValido_retornaClaimsCorrectos() {
        Usuario usuario = Usuario.builder()
                .idUsuario(7)
                .username("jgarcia")
                .rol(RolUsuario.AGENTE_ADUANERO)
                .correoInstitucional("jgarcia@transaduanas.pe")
                .build();

        String token = jwtTokenProvider.generateToken(usuario);

        assertNotNull(token);
        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("jgarcia", jwtTokenProvider.getUsernameFromToken(token));

        Claims claims = jwtTokenProvider.getClaimsFromToken(token);
        assertEquals("PERSONAL_INTERNO", claims.get("tipoUsuario", String.class));
        assertEquals(7, claims.get("idUsuario", Integer.class));
        assertEquals("AGENTE_ADUANERO", claims.get("rol", String.class));
        assertEquals("jgarcia@transaduanas.pe", claims.get("correo", String.class));
    }

    @Test
    @DisplayName("Normal: genera y valida token para cliente del portal B2B")
    void generateTokenForPortal_usuarioClienteValido_retornaClaimsB2B() {
        Cliente cliente = Cliente.builder()
                .idCliente(15)
                .numeroDocumento("20601234567")
                .razonSocial("Importaciones Andes SAC")
                .correoContacto("ops@andes.pe")
                .build();

        UsuarioCliente uc = UsuarioCliente.builder()
                .idUsuarioCliente(33)
                .username("portal_andes")
                .cliente(cliente)
                .build();

        String token = jwtTokenProvider.generateTokenForPortal(uc);

        assertTrue(jwtTokenProvider.validateToken(token));
        assertEquals("portal_andes", jwtTokenProvider.getUsernameFromToken(token));

        Claims claims = jwtTokenProvider.getClaimsFromToken(token);
        assertEquals("CLIENTE_PORTAL", claims.get("tipoUsuario", String.class));
        assertEquals(33, claims.get("idUsuarioCliente", Integer.class));
        assertEquals(15, claims.get("idCliente", Integer.class));
        assertEquals("20601234567", claims.get("ruc", String.class));
        assertEquals("Importaciones Andes SAC", claims.get("razonSocial", String.class));
    }

    @Test
    @DisplayName("Límite: secret menor a 32 bytes impide iniciar y firmar tokens")
    void generateToken_secretCorto_rechazado() {
        JwtTokenProvider shortSecretProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(shortSecretProvider, "jwtSecret", "clave_corta_123");
        ReflectionTestUtils.setField(shortSecretProvider, "jwtExpirationMs", 60000L);

        Usuario usuario = Usuario.builder()
                .idUsuario(1)
                .username("admin")
                .rol(RolUsuario.ADMINISTRADOR_TI)
                .correoInstitucional("admin@transaduanas.pe")
                .build();

        assertThrows(io.jsonwebtoken.security.WeakKeyException.class,
                shortSecretProvider::validateConfiguration);
        assertThrows(io.jsonwebtoken.security.WeakKeyException.class,
                () -> shortSecretProvider.generateToken(usuario));
    }

    @Test
    @DisplayName("Límite: token expirado (expiración negativa) retorna false en validateToken y lanza ExpiredJwtException al leer claims")
    void validateToken_tokenExpirado_retornaFalse() {
        ReflectionTestUtils.setField(jwtTokenProvider, "jwtExpirationMs", -1000L);

        Usuario usuario = Usuario.builder()
                .idUsuario(2)
                .username("expirado")
                .rol(RolUsuario.CONTADOR)
                .correoInstitucional("exp@transaduanas.pe")
                .build();

        String expiredToken = jwtTokenProvider.generateToken(usuario);

        assertFalse(jwtTokenProvider.validateToken(expiredToken));
        assertThrows(ExpiredJwtException.class, () -> jwtTokenProvider.getClaimsFromToken(expiredToken));
    }

    @Test
    @DisplayName("Nulo / Vacío: validateToken retorna false ante null o cadena vacía")
    void validateToken_nuloOVacio_retornaFalse() {
        assertFalse(jwtTokenProvider.validateToken(null));
        assertFalse(jwtTokenProvider.validateToken(""));
        assertFalse(jwtTokenProvider.validateToken("   "));
    }

    @Test
    @DisplayName("Excepción: token malformado o firmado con otra clave es rechazado")
    void validateToken_malformadoOFirmaInvalida_retornaFalse() {
        assertFalse(jwtTokenProvider.validateToken("esto.no.es.un.jwt"));
        assertThrows(MalformedJwtException.class, () -> jwtTokenProvider.getUsernameFromToken("token-invalido"));

        JwtTokenProvider otroProvider = new JwtTokenProvider();
        ReflectionTestUtils.setField(otroProvider, "jwtSecret", "OTRA_CLAVE_DISTINTA_PARA_PROBAR_FIRMA_INVALIDA_2026");
        ReflectionTestUtils.setField(otroProvider, "jwtExpirationMs", 60000L);

        Usuario usuario = Usuario.builder()
                .idUsuario(9)
                .username("otro")
                .rol(RolUsuario.AGENTE_COMERCIAL)
                .correoInstitucional("otro@transaduanas.pe")
                .build();

        String tokenOtraFirma = otroProvider.generateToken(usuario);
        assertFalse(jwtTokenProvider.validateToken(tokenOtraFirma));
    }
}
