package pe.sga.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import pe.sga.controller.AuthController;
import pe.sga.controller.DespachoTransporteController;
import pe.sga.controller.RolController;
import pe.sga.dto.response.UsuarioResponseDTO;
import pe.sga.entity.Cliente;
import pe.sga.entity.Usuario;
import pe.sga.entity.UsuarioCliente;
import pe.sga.entity.enums.RolUsuario;
import pe.sga.exception.GlobalExceptionHandler;
import pe.sga.repository.UsuarioClienteRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.IDespachoTransporteService;
import pe.sga.service.IRolService;
import pe.sga.service.IUsuarioService;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(classes = SecurityConfigTest.TestConfig.class)
@TestPropertySource(properties = {
        "jwt.secret=test_secret_key_for_sga_security_config_verification_2026",
        "jwt.expiration-ms=3600000"
})
public class SecurityConfigTest {

    @Configuration
    @EnableWebMvc
    @Import({
            CorsConfig.class,
            SecurityConfig.class,
            JwtTokenProvider.class,
            JwtAuthenticationFilter.class,
            AuthController.class,
            DespachoTransporteController.class,
            RolController.class,
            GlobalExceptionHandler.class
    })
    static class TestConfig {
        @Bean
        tools.jackson.databind.json.JsonMapper jsonMapper() {
            return tools.jackson.databind.json.JsonMapper.builder().findAndAddModules().build();
        }

        @Bean
        UsuarioRepository usuarioRepository() {
            return mock(UsuarioRepository.class);
        }

        @Bean
        UsuarioClienteRepository usuarioClienteRepository() {
            return mock(UsuarioClienteRepository.class);
        }

        @Bean
        IUsuarioService usuarioService() {
            return mock(IUsuarioService.class);
        }

        @Bean
        IDespachoTransporteService despachoTransporteService() {
            return mock(IDespachoTransporteService.class);
        }

        @Bean
        IRolService rolService() {
            return mock(IRolService.class);
        }
    }

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private FilterChainProxy springSecurityFilterChain;

    @Autowired
    private JwtTokenProvider tokenProvider;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private UsuarioClienteRepository usuarioClienteRepository;

    @Autowired
    private IUsuarioService usuarioService;

    @Autowired
    private IDespachoTransporteService despachoTransporteService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(wac)
                .addFilters(springSecurityFilterChain)
                .build();
    }

    @Test
    void authLogin_sinToken_esPermitidoPorSecurityConfig() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType("application/json")
                        .content("{\"username\":\"admin\",\"password\":\"123456\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void authMe_sinToken_retorna401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void authMe_conTokenInternoValido_retorna200() throws Exception {
        Usuario usuario = Usuario.builder()
                .idUsuario(7)
                .username("jperalta")
                .rol(RolUsuario.ADMINISTRADOR_TI)
                .correoInstitucional("jperalta@transaduanas.pe")
                .estadoActivo(true)
                .build();

        when(usuarioRepository.findByUsername("jperalta")).thenReturn(Optional.of(usuario));
        when(usuarioService.buscarPorId(7)).thenReturn(
                UsuarioResponseDTO.builder()
                        .idUsuario(7)
                        .username("jperalta")
                        .rol(RolUsuario.ADMINISTRADOR_TI.name())
                        .build()
        );

        String token = tokenProvider.generateToken(usuario);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.username").value("jperalta"));
    }

    @Test
    void authMe_conTokenPortal_retorna403Forbidden() throws Exception {
        Cliente cliente = Cliente.builder()
                .idCliente(10)
                .numeroDocumento("20123456789")
                .razonSocial("Importadora SAC")
                .estadoActivo(true)
                .build();
        UsuarioCliente uc = UsuarioCliente.builder()
                .idUsuarioCliente(20)
                .username("portal.user")
                .cliente(cliente)
                .estadoActivo(true)
                .build();

        when(usuarioClienteRepository.findByUsername("portal.user")).thenReturn(Optional.of(uc));

        String token = tokenProvider.generateTokenForPortal(uc);

        mockMvc.perform(get("/api/v1/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void despachoMisDespachos_rolSinPermisoTransporte_retorna403() throws Exception {
        Usuario comercial = Usuario.builder()
                .idUsuario(9)
                .username("comercial")
                .rol(RolUsuario.AGENTE_COMERCIAL)
                .correoInstitucional("comercial@transaduanas.pe")
                .estadoActivo(true)
                .build();

        when(usuarioRepository.findByUsername("comercial")).thenReturn(Optional.of(comercial));
        String token = tokenProvider.generateToken(comercial);

        mockMvc.perform(get("/api/v1/transporte/despachos/mi-despacho-activo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/transporte/despachos/mis-despachos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void despachoMisDespachos_supervisorTransporte_retorna200() throws Exception {
        Usuario supervisor = Usuario.builder()
                .idUsuario(12)
                .username("sup.transporte")
                .rol(RolUsuario.SUPERVISOR_TRANSPORTE)
                .correoInstitucional("sup@transaduanas.pe")
                .estadoActivo(true)
                .build();

        when(usuarioRepository.findByUsername("sup.transporte")).thenReturn(Optional.of(supervisor));
        when(despachoTransporteService.listarMisDespachos(12)).thenReturn(List.of());
        String token = tokenProvider.generateToken(supervisor);

        mockMvc.perform(get("/api/v1/transporte/despachos/mi-despacho-activo")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/transporte/despachos/mis-despachos")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void roles_soloLectura_rechazaPost() throws Exception {
        Usuario admin = Usuario.builder()
                .idUsuario(1)
                .username("admin")
                .rol(RolUsuario.ADMINISTRADOR_TI)
                .correoInstitucional("admin@transaduanas.pe")
                .estadoActivo(true)
                .build();

        when(usuarioRepository.findByUsername("admin")).thenReturn(Optional.of(admin));
        String token = tokenProvider.generateToken(admin);

        mockMvc.perform(get("/api/v1/roles")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/roles")
                        .header("Authorization", "Bearer " + token)
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().is5xxServerError());
    }
}
