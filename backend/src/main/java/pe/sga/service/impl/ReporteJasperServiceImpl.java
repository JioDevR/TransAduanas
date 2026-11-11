package pe.sga.service.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import pe.sga.dto.reporte.UsuarioPortalReporteItemDTO;
import pe.sga.dto.reporte.UsuarioReporteItemDTO;
import pe.sga.entity.CatalogoMercancia;
import pe.sga.entity.CatalogoPuerto;
import pe.sga.entity.CatalogoTipoCarga;
import pe.sga.entity.Cliente;
import pe.sga.entity.Sede;
import pe.sga.entity.Usuario;
import pe.sga.entity.UsuarioCliente;
import pe.sga.repository.CatalogoMercanciaRepository;
import pe.sga.repository.CatalogoPuertoRepository;
import pe.sga.repository.CatalogoTipoCargaRepository;
import pe.sga.repository.SedeRepository;
import pe.sga.repository.UsuarioClienteRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.IReporteJasperService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReporteJasperServiceImpl implements IReporteJasperService {

    private static final DateTimeFormatter REPORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UsuarioRepository usuarioRepository;
    private final UsuarioClienteRepository usuarioClienteRepository;
    private final SedeRepository sedeRepository;
    private final CatalogoPuertoRepository puertoRepository;
    private final CatalogoTipoCargaRepository tipoCargaRepository;
    private final CatalogoMercanciaRepository mercanciaRepository;
    private final SpringTemplateEngine templateEngine;

    // =========================================================================
    // 01. USUARIOS INTERNOS - ADMINISTRACIÓN TI (HTML / OPENHTMLTOPDF)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportarUsuariosPdf() {
        return exportarUsuariosPdf(true);
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportarUsuariosPdf(boolean soloActivos) {
        List<Usuario> usuarios = (soloActivos
                ? usuarioRepository.findByEstadoActivoTrue()
                : usuarioRepository.findAll())
                .stream()
                .sorted(Comparator.comparing(
                        Usuario::getUsername,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        List<UsuarioReporteItemDTO> items = usuarios.stream()
                .map(this::mapUsuarioToReporteItem)
                .toList();

        long totalSedes = items.stream()
                .map(UsuarioReporteItemDTO::getSede)
                .filter(s -> !"Sin sede".equals(s))
                .distinct()
                .count();

        Map<String, Object> variables = buildTiReportVariables(
                soloActivos ? "Listado de Usuarios Internos Activos" : "Listado General de Usuarios Internos",
                "ADMINISTRACIÓN TI // PERSONAL INTERNO",
                soloActivos ? "Listado de Usuarios Internos Activos" : "Listado General de Usuarios Internos",
                soloActivos ? "Solo cuentas activas" : "Todas las cuentas registradas",
                soloActivos ? "Usuarios Activos" : "Usuarios en Listado",
                items.size(),
                usuarioRepository.count()
        );
        variables.put("items", items);
        variables.put("totalSedes", totalSedes);

        return renderHtmlToPdf("reports/reporte-usuarios", variables);
    }

    // =========================================================================
    // 01B. USUARIOS PORTAL B2B - ADMINISTRACIÓN TI (HTML / OPENHTMLTOPDF)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportarUsuariosPortalPdf() {
        return exportarUsuariosPortalPdf(true);
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportarUsuariosPortalPdf(boolean soloActivos) {
        List<UsuarioCliente> usuariosPortal = (soloActivos
                ? usuarioClienteRepository.findByEstadoActivoTrueWithCliente()
                : usuarioClienteRepository.findAllWithCliente())
                .stream()
                .sorted(Comparator.comparing(
                        UsuarioCliente::getUsername,
                        Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();

        List<UsuarioPortalReporteItemDTO> items = usuariosPortal.stream()
                .map(this::mapUsuarioPortalToReporteItem)
                .toList();

        long totalEmpresas = items.stream()
                .map(UsuarioPortalReporteItemDTO::getEmpresa)
                .filter(e -> !"—".equals(e))
                .distinct()
                .count();

        Map<String, Object> variables = buildTiReportVariables(
                soloActivos ? "Listado de Usuarios Activos - Portal B2B" : "Listado General de Usuarios - Portal B2B",
                "ADMINISTRACIÓN TI // PORTAL CLIENTES B2B",
                soloActivos ? "Listado de Usuarios Activos del Portal B2B" : "Listado General de Usuarios del Portal B2B",
                soloActivos ? "Solo cuentas activas" : "Todas las cuentas registradas",
                soloActivos ? "Cuentas Activas" : "Cuentas en Listado",
                items.size(),
                usuarioClienteRepository.count()
        );
        variables.put("items", items);
        variables.put("totalEmpresas", totalEmpresas);

        return renderHtmlToPdf("reports/reporte-usuarios-portal", variables);
    }

    // =========================================================================
    // 02. SEDES Y AGENCIAS TERRITORIALES (CORPORATE HTML / OPENHTMLTOPDF)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportarSedesPdf() {
        List<Sede> sedes = sedeRepository.findAll();

        long operativas = sedes.stream()
                .filter(s -> "ACTIVO".equalsIgnoreCase(s.getEstado()) || "OPERATIVO".equalsIgnoreCase(s.getEstado()))
                .count();
        long inactivas = sedes.size() - operativas;
        long totalDistritos = sedes.stream()
                .map(Sede::getDistrito)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        Map<String, Object> variables = new HashMap<>();
        variables.put("items", sedes);
        variables.put("totalSedes", sedes.size());
        variables.put("totalOperativas", operativas);
        variables.put("totalInactivas", inactivas);
        variables.put("totalDistritos", totalDistritos > 0 ? totalDistritos : 1);
        variables.put("fechaGeneracion", LocalDateTime.now().format(REPORT_DATE_FORMATTER));

        return renderHtmlToPdf("reports/reporte-sedes", variables);
    }

    // =========================================================================
    // 03. PUERTOS Y TERMINALES ADUANEROS (CORPORATE HTML / OPENHTMLTOPDF)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportarPuertosPdf() {
        List<CatalogoPuerto> puertos = puertoRepository.findAll();

        long maritimos = puertos.stream()
                .filter(p -> p.getTipo() != null && p.getTipo().toUpperCase().contains("MARIT"))
                .count();
        long activos = puertos.stream()
                .filter(p -> "ACTIVO".equalsIgnoreCase(p.getEstado()))
                .count();
        long otros = puertos.size() - maritimos;

        Map<String, Object> variables = new HashMap<>();
        variables.put("items", puertos);
        variables.put("totalPuertos", puertos.size());
        variables.put("totalMaritimos", maritimos);
        variables.put("totalOtros", otros);
        variables.put("totalActivos", activos);
        variables.put("fechaGeneracion", LocalDateTime.now().format(REPORT_DATE_FORMATTER));

        return renderHtmlToPdf("reports/reporte-puertos", variables);
    }

    // =========================================================================
    // 04. TIPOS DE CARGA & CONTENEDOR (CORPORATE HTML / OPENHTMLTOPDF)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportarTiposCargaPdf() {
        List<CatalogoTipoCarga> tipos = tipoCargaRepository.findAll();

        long activos = tipos.stream()
                .filter(t -> "ACTIVO".equalsIgnoreCase(t.getEstado()))
                .count();
        long fcl = tipos.stream()
                .filter(t -> (t.getCodigo() != null && t.getCodigo().toUpperCase().contains("CONT"))
                        || (t.getNombre() != null && t.getNombre().toUpperCase().contains("CONTENEDOR")))
                .count();
        long lcl = tipos.size() - fcl;

        Map<String, Object> variables = new HashMap<>();
        variables.put("items", tipos);
        variables.put("totalTipos", tipos.size());
        variables.put("totalActivos", activos);
        variables.put("totalFcl", fcl);
        variables.put("totalLcl", lcl);
        variables.put("fechaGeneracion", LocalDateTime.now().format(REPORT_DATE_FORMATTER));

        return renderHtmlToPdf("reports/reporte-tipos-carga", variables);
    }

    // =========================================================================
    // 05. MERCANCÍAS & SUBPARTIDAS ARANCELARIAS (CORPORATE HTML / OPENHTMLTOPDF)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportarMercanciasPdf() {
        List<CatalogoMercancia> mercancias = mercanciaRepository.findAll();

        long vuce = mercancias.stream()
                .filter(m -> Boolean.TRUE.equals(m.getRequierePermiso()))
                .count();
        long libre = mercancias.size() - vuce;
        long activos = mercancias.stream()
                .filter(m -> "ACTIVO".equalsIgnoreCase(m.getEstado()))
                .count();

        Map<String, Object> variables = new HashMap<>();
        variables.put("items", mercancias);
        variables.put("totalMercancias", mercancias.size());
        variables.put("totalVuce", vuce);
        variables.put("totalLibre", libre);
        variables.put("totalActivos", activos);
        variables.put("fechaGeneracion", LocalDateTime.now().format(REPORT_DATE_FORMATTER));

        return renderHtmlToPdf("reports/reporte-mercancias", variables);
    }

    // =========================================================================
    // HELPERS DE MAPEO Y METADATOS TI
    // =========================================================================
    private UsuarioReporteItemDTO mapUsuarioToReporteItem(Usuario usuario) {
        String cargoFallback = usuario.getRol() != null ? usuario.getRol().getEtiqueta() : "—";
        return UsuarioReporteItemDTO.builder()
                .username(defaultIfBlank(usuario.getUsername(), "—"))
                .correo(defaultIfBlank(usuario.getCorreoInstitucional(), "—"))
                .numero(defaultIfBlank(usuario.getTelefono(), "—"))
                .cargo(defaultIfBlank(usuario.getCargoTitulo(), cargoFallback))
                .sede(usuario.getSede() != null ? defaultIfBlank(usuario.getSede().getNombre(), "Sin sede") : "Sin sede")
                .build();
    }

    private UsuarioPortalReporteItemDTO mapUsuarioPortalToReporteItem(UsuarioCliente uc) {
        Cliente cliente = uc.getCliente();
        return UsuarioPortalReporteItemDTO.builder()
                .username(defaultIfBlank(uc.getUsername(), "—"))
                .empresa(cliente != null ? defaultIfBlank(cliente.getRazonSocial(), "—") : "—")
                .representante(cliente != null ? defaultIfBlank(cliente.getContactoRepresentante(), "—") : "—")
                .correo(cliente != null ? defaultIfBlank(cliente.getCorreoContacto(), "—") : "—")
                .build();
    }

    private Map<String, Object> buildTiReportVariables(
            String tituloDocumento,
            String kickerTexto,
            String tituloReporte,
            String alcanceTexto,
            String tituloMetrica1,
            int totalListado,
            long totalRegistrados) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("tituloDocumento", tituloDocumento);
        variables.put("kickerTexto", kickerTexto);
        variables.put("tituloReporte", tituloReporte);
        variables.put("alcanceTexto", alcanceTexto);
        variables.put("tituloMetrica1", tituloMetrica1);
        variables.put("totalListado", totalListado);
        variables.put("totalRegistrados", totalRegistrados);
        variables.put("fechaGeneracion", LocalDateTime.now().format(REPORT_DATE_FORMATTER));
        return variables;
    }

    private String defaultIfBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value.trim() : fallback;
    }

    // =========================================================================
    // MOTOR DE RENDERIZADO HTML CORPORATIVO A PDF
    // =========================================================================
    private ByteArrayInputStream renderHtmlToPdf(String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            String html = templateEngine.process(templateName, context);

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(os);
            builder.run();

            return new ByteArrayInputStream(os.toByteArray());
        } catch (Exception e) {
            log.error("Error al renderizar PDF corporativo HTML con plantilla {}: {}", templateName, e.getMessage(), e);
            throw new RuntimeException("Error al generar PDF corporativo (" + templateName + "): " + e.getMessage(), e);
        }
    }
}
