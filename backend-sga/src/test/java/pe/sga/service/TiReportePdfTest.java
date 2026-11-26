package pe.sga.service;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import pe.sga.dto.response.AdminAlmacenamientoDTO;
import pe.sga.dto.response.AdminDashboardResponseDTO;
import pe.sga.dto.response.AdminSistemaEstadoDTO;
import pe.sga.entity.AuditoriaEvento;
import pe.sga.entity.Cliente;
import pe.sga.entity.Sede;
import pe.sga.entity.Usuario;
import pe.sga.entity.UsuarioCliente;
import pe.sga.entity.enums.RolUsuario;
import pe.sga.repository.*;
import pe.sga.service.impl.ReporteJasperServiceImpl;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("unitaria")
class TiReportePdfTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioClienteRepository usuarioClienteRepository;
    @Mock private SedeRepository sedeRepository;
    @Mock private CatalogoPuertoRepository puertoRepository;
    @Mock private CatalogoTipoCargaRepository tipoCargaRepository;
    @Mock private CatalogoMercanciaRepository mercanciaRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private ICotizacionService cotizacionService;
    @Mock private AuditoriaEventoRepository auditoriaRepository;
    @Mock private IAdminSistemaService adminSistemaService;

    private ReporteJasperServiceImpl buildService() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        ReporteJasperServiceImpl service = new ReporteJasperServiceImpl(
                usuarioRepository,
                usuarioClienteRepository,
                sedeRepository,
                puertoRepository,
                tipoCargaRepository,
                mercanciaRepository,
                clienteRepository,
                cotizacionService,
                templateEngine
        );
        service.setAuditoriaRepository(auditoriaRepository);
        service.setAdminSistemaService(adminSistemaService);
        return service;
    }

    @Test
    void exportarUsuariosPdf_generaPdfValidoConEstadoYNombre() {
        Sede sede = Sede.builder().nombre("Sede Callao").build();
        Usuario u = Usuario.builder()
                .username("admin")
                .nombres("Carlos")
                .apellidos("Alvarez")
                .correoInstitucional("admin@sga.pe")
                .telefono("+51 999 888 777")
                .cargoTitulo("Administrador TI")
                .rol(RolUsuario.ADMINISTRADOR_TI)
                .sede(sede)
                .estadoActivo(true)
                .build();
        when(usuarioRepository.findAll()).thenReturn(List.of(u));
        when(usuarioRepository.count()).thenReturn(1L);

        ByteArrayInputStream pdf = buildService().exportarUsuariosPdf(false);
        byte[] bytes = pdf.readAllBytes();
        assertTrue(bytes.length > 500);
        assertTrue(new String(bytes, 0, 5, StandardCharsets.US_ASCII).startsWith("%PDF-"));
    }

    @Test
    void exportarUsuariosPortalPdf_generaPdfValidoConRucYEstado() {
        Cliente cliente = Cliente.builder()
                .razonSocial("Camposol S.A.")
                .numeroDocumento("20370146994")
                .contactoRepresentante("María Pérez")
                .correoContacto("ops@camposol.pe")
                .build();
        UsuarioCliente uc = UsuarioCliente.builder()
                .username("20370146994")
                .cliente(cliente)
                .estadoActivo(true)
                .build();
        when(usuarioClienteRepository.findAllWithCliente()).thenReturn(List.of(uc));
        when(usuarioClienteRepository.count()).thenReturn(1L);

        ByteArrayInputStream pdf = buildService().exportarUsuariosPortalPdf(false);
        byte[] bytes = pdf.readAllBytes();
        assertTrue(bytes.length > 500);
        assertTrue(new String(bytes, 0, 5, StandardCharsets.US_ASCII).startsWith("%PDF-"));
    }

    @Test
    void exportarAuditoriaTiPdf_generaPdfValidoConFiltros() {
        AuditoriaEvento ev = AuditoriaEvento.builder()
                .idEvento(1L)
                .fecha(LocalDateTime.now())
                .usuarioActor("admin")
                .rolActor("ADMINISTRADOR_TI")
                .modulo("SEGURIDAD_TI")
                .accion("LOGIN_EXITOSO")
                .entidad("USUARIO")
                .detalle("Acceso válido")
                .ipOrigen("127.0.0.1")
                .resultado("EXITO")
                .build();

        when(auditoriaRepository.buscarConFiltros(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(ev)));

        ByteArrayInputStream pdf = buildService().exportarAuditoriaTiPdf(
                null, "SEGURIDAD_TI", "EXITO", LocalDateTime.now().minusDays(7), LocalDateTime.now());
        byte[] bytes = pdf.readAllBytes();
        assertTrue(bytes.length > 500);
        assertTrue(new String(bytes, 0, 5, StandardCharsets.US_ASCII).startsWith("%PDF-"));
    }

    @Test
    void exportarSistemaTiPdf_generaPdfValidoConEntidades() {
        AdminSistemaEstadoDTO estado = AdminSistemaEstadoDTO.builder()
                .appNombre("sga-backend")
                .appVersion("0.5.1")
                .perfilActivo("local")
                .javaVersion("21")
                .osNombre("Linux amd64")
                .bdConectada(true)
                .bdMotor("MySQL")
                .bdVersion("8.0")
                .bdLatenciaMs(3)
                .jvmHeapUsadoMb(128)
                .jvmHeapMaximoMb(512)
                .jvmHeapPorcentaje(25)
                .build();
        AdminDashboardResponseDTO dash = AdminDashboardResponseDTO.builder()
                .sistema(estado)
                .usuariosTotal(10)
                .usuariosActivos(8)
                .usuariosInactivos(2)
                .portalUsuariosTotal(5)
                .portalUsuariosActivos(4)
                .portalUsuariosInactivos(1)
                .auditoriaTotalPeriodo(20)
                .auditoriaExitosPeriodo(19)
                .auditoriaFallosPeriodo(1)
                .loginsFallidosPeriodo(1)
                .build();
        AdminAlmacenamientoDTO storage = AdminAlmacenamientoDTO.builder()
                .disponible(true)
                .discoOcupacionPct(40)
                .discoTotalMb(10000)
                .discoUsadoMb(4000)
                .discoLibreMb(6000)
                .archivosSincronizados(12)
                .archivosEnDisco(12)
                .registrosBdActivos(12)
                .archivosFaltantesEnDisco(0)
                .archivosHuerfanosEnDisco(0)
                .storageTamanoBytes(102400)
                .porEntidad(List.of(AdminAlmacenamientoDTO.GrupoStorageDTO.builder()
                        .clave("EXPEDIENTE")
                        .etiqueta("Expedientes")
                        .cantidad(12)
                        .tamanoBytes(102400)
                        .build()))
                .build();

        when(adminSistemaService.obtenerDashboard(7, "TODOS")).thenReturn(dash);
        when(adminSistemaService.obtenerDiagnosticoAlmacenamiento(null, null, null, null, 0, 1)).thenReturn(storage);

        ByteArrayInputStream pdf = buildService().exportarSistemaTiPdf(7);
        byte[] bytes = pdf.readAllBytes();
        assertTrue(bytes.length > 500);
        assertTrue(new String(bytes, 0, 5, StandardCharsets.US_ASCII).startsWith("%PDF-"));
    }
}
