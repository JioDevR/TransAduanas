package pe.sga.service.impl;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import pe.sga.dto.portal.PortalCotizacionDTO;
import pe.sga.dto.reporte.UsuarioPortalReporteItemDTO;
import pe.sga.dto.reporte.UsuarioReporteItemDTO;
import pe.sga.dto.response.AdminAlmacenamientoDTO;
import pe.sga.dto.response.AdminDashboardResponseDTO;
import pe.sga.dto.response.AdminSistemaEstadoDTO;
import pe.sga.dto.response.CotizacionResponseDTO;
import pe.sga.entity.AuditoriaEvento;
import pe.sga.entity.CatalogoMercancia;
import pe.sga.entity.CatalogoPuerto;
import pe.sga.entity.CatalogoTipoCarga;
import pe.sga.entity.Cliente;
import pe.sga.entity.Sede;
import pe.sga.entity.Usuario;
import pe.sga.entity.UsuarioCliente;
import pe.sga.entity.enums.EstadoCotizacion;
import pe.sga.repository.AuditoriaEventoRepository;
import pe.sga.repository.CatalogoMercanciaRepository;
import pe.sga.repository.CatalogoPuertoRepository;
import pe.sga.repository.CatalogoTipoCargaRepository;
import pe.sga.repository.ClienteRepository;
import pe.sga.repository.SedeRepository;
import pe.sga.repository.UsuarioClienteRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.IAdminSistemaService;
import pe.sga.service.ICotizacionService;
import pe.sga.service.IReporteJasperService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    private final ClienteRepository clienteRepository;
    private final ICotizacionService cotizacionService;
    private final SpringTemplateEngine templateEngine;

    private AuditoriaEventoRepository auditoriaRepository;
    private IAdminSistemaService adminSistemaService;

    @Autowired(required = false)
    public void setAuditoriaRepository(AuditoriaEventoRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @Autowired(required = false)
    public void setAdminSistemaService(IAdminSistemaService adminSistemaService) {
        this.adminSistemaService = adminSistemaService;
    }

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
    // 01C. AUDITORÍA TI Y ESTADO DEL SISTEMA TI (HTML / OPENHTMLTOPDF)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportarAuditoriaTiPdf(
            String buscar,
            String modulo,
            String resultado,
            LocalDateTime desde,
            LocalDateTime hasta) {
        List<AuditoriaEvento> eventos = auditoriaRepository == null
                ? List.of()
                : auditoriaRepository.buscarConFiltros(
                        buscar != null ? buscar.trim() : null,
                        modulo != null ? modulo.trim() : null,
                        resultado != null ? resultado.trim() : null,
                        desde,
                        hasta,
                        PageRequest.of(0, 500)
                ).getContent();

        long totalExitos = eventos.stream()
                .filter(e -> !"FALLO".equalsIgnoreCase(e.getResultado()) && !"ERROR".equalsIgnoreCase(e.getResultado()))
                .count();
        long totalFallos = eventos.size() - totalExitos;

        List<Map<String, String>> items = eventos.stream().map(ev -> {
            Map<String, String> row = new HashMap<>();
            row.put("fechaTexto", ev.getFecha() != null ? ev.getFecha().format(REPORT_DATE_FORMATTER) : "—");
            row.put("usuarioActor", defaultIfBlank(ev.getUsuarioActor(), "SISTEMA"));
            row.put("rolActor", defaultIfBlank(ev.getRolActor(), "SIN ROL"));
            row.put("accion", defaultIfBlank(ev.getAccion(), "—"));
            row.put("moduloEntidad", defaultIfBlank(ev.getModulo(), "SISTEMA")
                    + (ev.getEntidad() != null && !ev.getEntidad().isBlank() ? " · " + ev.getEntidad() : ""));
            row.put("detalle", defaultIfBlank(ev.getDetalle(), "—"));
            row.put("ipOrigen", defaultIfBlank(ev.getIpOrigen(), "—"));
            row.put("resultado", defaultIfBlank(ev.getResultado(), "EXITO"));
            return row;
        }).toList();

        String alcance = buildAuditoriaAlcance(modulo, resultado, desde, hasta);
        Map<String, Object> variables = buildTiReportVariables(
                "Bitácora de Auditoría y Seguridad TI",
                "ADMINISTRACIÓN TI // SEGURIDAD Y TRAZABILIDAD",
                "Bitácora de Auditoría TI",
                alcance,
                "Eventos en Informe",
                items.size(),
                totalExitos
        );
        variables.put("items", items);
        variables.put("totalExitos", totalExitos);
        variables.put("totalFallos", totalFallos);

        return renderHtmlToPdf("reports/reporte-auditoria-ti", variables);
    }

    private String buildAuditoriaAlcance(String modulo, String resultado, LocalDateTime desde, LocalDateTime hasta) {
        List<String> partes = new ArrayList<>();
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        if (desde != null && hasta != null) {
            partes.add("Del " + desde.format(dayFmt) + " al " + hasta.format(dayFmt));
        } else if (desde != null) {
            partes.add("Desde " + desde.format(dayFmt));
        } else if (hasta != null) {
            partes.add("Hasta " + hasta.format(dayFmt));
        } else {
            partes.add("Histórico completo");
        }
        partes.add((modulo != null && !modulo.isBlank()) ? "Módulo: " + modulo : "Todos los módulos");
        if (resultado != null && !resultado.isBlank()) {
            partes.add("Estado: " + resultado);
        }
        return String.join(" · ", partes);
    }

    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportarSistemaTiPdf(int dias) {
        int safeDias = Math.min(Math.max(1, dias), 90);
        AdminDashboardResponseDTO dash = adminSistemaService != null
                ? adminSistemaService.obtenerDashboard(safeDias, "TODOS")
                : AdminDashboardResponseDTO.builder().diasPeriodo(safeDias).build();
        AdminSistemaEstadoDTO sistema = dash.getSistema() != null
                ? dash.getSistema()
                : AdminSistemaEstadoDTO.builder().build();
        AdminAlmacenamientoDTO storage = adminSistemaService != null
                ? adminSistemaService.obtenerDiagnosticoAlmacenamiento(null, null, null, null, 0, 1)
                : AdminAlmacenamientoDTO.builder().build();

        String estadoBd = sistema.isBdConectada()
                ? "UP · " + sistema.getBdLatenciaMs() + " ms"
                : "DOWN";
        long totalActivas = dash.getUsuariosActivos() + dash.getPortalUsuariosActivos();
        String cuentasActivas = totalActivas + " activas";
        String usoDisco = storage.getDiscoOcupacionPct() + "%";
        List<Map<String, Object>> entidadesStorage = storage.getPorEntidad() == null
                ? List.of()
                : storage.getPorEntidad().stream().map(g -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("etiqueta", defaultIfBlank(g.getEtiqueta(), g.getClave()));
                    m.put("cantidad", g.getCantidad());
                    m.put("tamanoTexto", formatBytesSimple(g.getTamanoBytes()));
                    return m;
                }).toList();
        int totalSecciones = entidadesStorage.isEmpty() ? 3 : 4;

        Map<String, Object> variables = buildTiReportVariables(
                "Informe Técnico de Sistema y Almacenamiento TI",
                "ADMINISTRACIÓN TI // INFRAESTRUCTURA",
                "Informe Técnico de Estado del Sistema",
                "Ventana operativa de " + safeDias + " días",
                "Secciones Evaluadas",
                totalSecciones,
                totalSecciones
        );
        variables.put("dash", dash);
        variables.put("storage", storage);
        variables.put("entidadesStorage", entidadesStorage);
        variables.put("estadoBdTexto", estadoBd);
        variables.put("bdConectada", sistema.isBdConectada());
        variables.put("cuentasActivasTexto", cuentasActivas);
        variables.put("usoDiscoTexto", usoDisco);
        variables.put("bdDetalle", defaultIfBlank(sistema.getBdMotor(), "MySQL") + " " + defaultIfBlank(sistema.getBdVersion(), ""));
        variables.put("jvmDetalle", sistema.getJvmHeapUsadoMb() + " MB / " + sistema.getJvmHeapMaximoMb() + " MB");
        variables.put("jvmPorcentaje", sistema.getJvmHeapPorcentaje());
        variables.put("runtimeDetalle", "Java " + defaultIfBlank(sistema.getJavaVersion(), "21") + " · " + defaultIfBlank(sistema.getOsNombre(), "Linux"));
        variables.put("appVersionTexto", "v" + defaultIfBlank(sistema.getAppVersion(), "0.5.1") + " (" + defaultIfBlank(sistema.getPerfilActivo(), "default") + ")");
        variables.put("discoDetalle", storage.getDiscoUsadoMb() + " MB usados de " + storage.getDiscoTotalMb() + " MB (" + storage.getDiscoLibreMb() + " MB libres)");
        variables.put("storageTamanoTexto", formatBytesSimple(storage.getStorageTamanoBytes()));

        return renderHtmlToPdf("reports/reporte-sistema-ti", variables);
    }

    private String formatBytesSimple(long bytes) {
        if (bytes < 1024) return bytes + " B";
        double kb = bytes / 1024.0;
        if (kb < 1024) return String.format(Locale.US, "%.1f KB", kb);
        double mb = kb / 1024.0;
        if (mb < 1024) return String.format(Locale.US, "%.2f MB", mb);
        return String.format(Locale.US, "%.2f GB", mb / 1024.0);
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
    // 06. GESTIÓN COMERCIAL - REPORTE EJECUTIVO DE MÉTRICAS (HTML / OPENHTMLTOPDF)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public ByteArrayInputStream exportarReporteComercialPdf(
            LocalDate desde,
            LocalDate hasta,
            Integer idUsuarioComercial,
            String regimen,
            Usuario usuarioActual) {

        List<CotizacionResponseDTO> base = cotizacionService.listar(false, null, null, idUsuarioComercial, usuarioActual);
        List<CotizacionResponseDTO> filtradas = base.stream()
                .filter(c -> desde == null || (c.getFechaEmision() != null && !c.getFechaEmision().isBefore(desde)))
                .filter(c -> hasta == null || (c.getFechaEmision() != null && !c.getFechaEmision().isAfter(hasta)))
                .filter(c -> regimen == null || regimen.isBlank() || "ALL".equalsIgnoreCase(regimen)
                        || regimen.equalsIgnoreCase(c.getRegimenAduanero()))
                .toList();

        int totalCotizaciones = filtradas.size();
        BigDecimal totalMonto = filtradas.stream()
                .map(c -> safeDecimal(c.getTotalCotizado()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalAbonado = filtradas.stream()
                .map(c -> safeDecimal(c.getTotalAbonado()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalSaldoPendiente = filtradas.stream()
                .map(c -> safeDecimal(c.getSaldoPendiente()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long aprobadasCount = filtradas.stream()
                .filter(c -> c.getEstado() == EstadoCotizacion.APROBADA)
                .count();
        double conversionRate = totalCotizaciones > 0
                ? (aprobadasCount * 100.0) / totalCotizaciones
                : 0.0;
        BigDecimal ticketPromedio = totalCotizaciones > 0
                ? totalMonto.divide(BigDecimal.valueOf(totalCotizaciones), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        List<Cliente> clientes = clienteRepository.findAll();
        long clientesEfectivos = clientes.stream().filter(cl -> !Boolean.TRUE.equals(cl.getEsProspecto())).count();
        long prospectos = clientes.size() - clientesEfectivos;
        int sinAsignarCount = cotizacionService.listarSinAsignar().size();

        // Desglose por estado
        List<Map<String, Object>> estadosBreakdown = new ArrayList<>();
        for (EstadoCotizacion estado : EstadoCotizacion.values()) {
            List<CotizacionResponseDTO> delEstado = filtradas.stream()
                    .filter(c -> c.getEstado() == estado)
                    .toList();
            BigDecimal montoEstado = delEstado.stream()
                    .map(c -> safeDecimal(c.getTotalCotizado()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            double pct = totalCotizaciones > 0 ? (delEstado.size() * 100.0) / totalCotizaciones : 0.0;

            Map<String, Object> row = new HashMap<>();
            row.put("label", estado.name().replace('_', ' '));
            row.put("count", delEstado.size());
            row.put("porcentajeFmt", String.format(Locale.US, "%.1f%%", pct));
            row.put("montoFmt", formatUsd(montoEstado));
            estadosBreakdown.add(row);
        }

        // Desglose por régimen
        Map<String, List<CotizacionResponseDTO>> porRegimen = new LinkedHashMap<>();
        for (CotizacionResponseDTO c : filtradas) {
            String key = defaultIfBlank(c.getRegimenAduanero(), "OTROS");
            porRegimen.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
        }
        List<Map<String, Object>> regimenesBreakdown = porRegimen.entrySet().stream()
                .map(entry -> {
                    BigDecimal montoReg = entry.getValue().stream()
                            .map(c -> safeDecimal(c.getTotalCotizado()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    Map<String, Object> row = new HashMap<>();
                    row.put("label", entry.getKey().replace('_', ' '));
                    row.put("count", entry.getValue().size());
                    row.put("monto", montoReg);
                    row.put("montoFmt", formatUsd(montoReg));
                    return row;
                })
                .sorted((a, b) -> ((BigDecimal) b.get("monto")).compareTo((BigDecimal) a.get("monto")))
                .toList();

        // Top Clientes (máx 8)
        Map<String, List<CotizacionResponseDTO>> porCliente = new LinkedHashMap<>();
        for (CotizacionResponseDTO c : filtradas) {
            String key = defaultIfBlank(c.getClienteNumeroDocumento(), "S/D") + "|" + defaultIfBlank(c.getClienteRazonSocial(), "Cliente N/A");
            porCliente.computeIfAbsent(key, k -> new ArrayList<>()).add(c);
        }
        List<Map<String, Object>> topClientes = porCliente.values().stream()
                .map(lista -> {
                    CotizacionResponseDTO sample = lista.get(0);
                    BigDecimal montoCl = lista.stream()
                            .map(c -> safeDecimal(c.getTotalCotizado()))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    long aprobCl = lista.stream()
                            .filter(c -> c.getEstado() == EstadoCotizacion.APROBADA)
                            .count();
                    Map<String, Object> row = new HashMap<>();
                    row.put("razonSocial", defaultIfBlank(sample.getClienteRazonSocial(), "Cliente N/A"));
                    row.put("documento", defaultIfBlank(sample.getClienteNumeroDocumento(), "—"));
                    row.put("count", lista.size());
                    row.put("aprobadas", aprobCl);
                    row.put("monto", montoCl);
                    row.put("montoFmt", formatUsd(montoCl));
                    return row;
                })
                .sorted((a, b) -> ((BigDecimal) b.get("monto")).compareTo((BigDecimal) a.get("monto")))
                .limit(8)
                .toList();

        // Detalle de cotizaciones recientes (máx 40)
        DateTimeFormatter isoDate = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        List<Map<String, Object>> cotizacionesDetalle = filtradas.stream()
                .sorted(Comparator.comparing(
                        CotizacionResponseDTO::getIdCotizacion,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(40)
                .map(c -> {
                    Map<String, Object> row = new HashMap<>();
                    row.put("codigo", defaultIfBlank(c.getCodigoCotizacion(), "—"));
                    row.put("fecha", c.getFechaEmision() != null ? c.getFechaEmision().format(isoDate) : "—");
                    row.put("cliente", defaultIfBlank(c.getClienteRazonSocial(), "Cliente N/A"));
                    row.put("asesor", defaultIfBlank(c.getUsuarioComercialNombre(), "Sin asignar"));
                    row.put("regimen", defaultIfBlank(c.getRegimenAduanero(), "OTROS").replace('_', ' '));
                    String est = c.getEstado() != null ? c.getEstado().name() : "PENDIENTE";
                    row.put("estado", est.replace('_', ' '));
                    row.put("badgeClass", resolveEstadoBadgeClass(c.getEstado()));
                    row.put("totalFmt", formatUsd(safeDecimal(c.getTotalCotizado())));
                    return row;
                })
                .toList();

        String periodoTexto = buildPeriodoTexto(desde, hasta, isoDate);
        String alcanceTexto = resolveAlcanceTexto(idUsuarioComercial, usuarioActual);
        String regimenTexto = (regimen != null && !regimen.isBlank() && !"ALL".equalsIgnoreCase(regimen))
                ? regimen.replace('_', ' ')
                : "Todos los regímenes";
        String usuarioEmisor = usuarioActual != null
                ? defaultIfBlank(usuarioActual.getNombreCompleto(), usuarioActual.getUsername())
                : "Sistema SGA";

        Map<String, Object> variables = new HashMap<>();
        variables.put("periodoTexto", periodoTexto);
        variables.put("alcanceTexto", alcanceTexto);
        variables.put("regimenTexto", regimenTexto);
        variables.put("usuarioEmisor", usuarioEmisor);
        variables.put("fechaGeneracion", LocalDateTime.now().format(REPORT_DATE_FORMATTER));
        variables.put("totalCotizaciones", totalCotizaciones);
        variables.put("totalMontoCotizadoFmt", formatUsd(totalMonto));
        variables.put("aprobadasCount", aprobadasCount);
        variables.put("conversionRateFmt", String.format(Locale.US, "%.1f%%", conversionRate));
        variables.put("ticketPromedioFmt", formatUsd(ticketPromedio));
        variables.put("totalAbonadoFmt", formatUsd(totalAbonado));
        variables.put("totalSaldoPendienteFmt", formatUsd(totalSaldoPendiente));
        variables.put("totalClientes", clientes.size());
        variables.put("clientesEfectivos", clientesEfectivos);
        variables.put("prospectos", prospectos);
        variables.put("sinAsignarCount", sinAsignarCount);
        variables.put("estadosBreakdown", estadosBreakdown);
        variables.put("regimenesBreakdown", regimenesBreakdown);
        variables.put("topClientes", topClientes);
        variables.put("cotizacionesDetalle", cotizacionesDetalle);

        return renderHtmlToPdf("reports/reporte-comercial", variables);
    }

    private BigDecimal safeDecimal(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private String formatUsd(BigDecimal amount) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(2);
        nf.setMaximumFractionDigits(2);
        return "$" + nf.format(safeDecimal(amount));
    }

    private String resolveEstadoBadgeClass(EstadoCotizacion estado) {
        if (estado == null) return "badge-neutral";
        return switch (estado) {
            case APROBADA -> "badge-ok";
            case PENDIENTE, EN_REVISION -> "badge-warn";
            case OBSERVADA, RECHAZADA, ANULADA -> "badge-danger";
        };
    }

    private String buildPeriodoTexto(LocalDate desde, LocalDate hasta, DateTimeFormatter fmt) {
        if (desde != null && hasta != null) {
            return desde.format(fmt) + " al " + hasta.format(fmt);
        }
        if (desde != null) {
            return "Desde " + desde.format(fmt);
        }
        if (hasta != null) {
            return "Hasta " + hasta.format(fmt);
        }
        return "Histórico completo";
    }

    private String resolveAlcanceTexto(Integer idUsuarioComercial, Usuario usuarioActual) {
        if (usuarioActual != null && usuarioActual.getRol() != null
                && "AGENTE_COMERCIAL".equalsIgnoreCase(usuarioActual.getRol().name())) {
            return "Cartera de " + defaultIfBlank(usuarioActual.getNombreCompleto(), usuarioActual.getUsername());
        }
        if (idUsuarioComercial != null) {
            return usuarioRepository.findById(idUsuarioComercial)
                    .map(u -> "Asesor: " + defaultIfBlank(u.getNombreCompleto(), u.getUsername()))
                    .orElse("Asesor #" + idUsuarioComercial);
        }
        return "Consolidado General";
    }

    // =========================================================================
    // HELPERS DE MAPEO Y METADATOS TI
    // =========================================================================
    private UsuarioReporteItemDTO mapUsuarioToReporteItem(Usuario usuario) {
        String cargoFallback = usuario.getRol() != null ? usuario.getRol().getEtiqueta() : "—";
        String nombre = defaultIfBlank(usuario.getNombreCompleto(), "—");
        return UsuarioReporteItemDTO.builder()
                .username(defaultIfBlank(usuario.getUsername(), "—"))
                .nombreCompleto(nombre)
                .correo(defaultIfBlank(usuario.getCorreoInstitucional(), "—"))
                .numero(defaultIfBlank(usuario.getTelefono(), "—"))
                .cargo(defaultIfBlank(usuario.getCargoTitulo(), cargoFallback))
                .sede(usuario.getSede() != null ? defaultIfBlank(usuario.getSede().getNombre(), "Sin sede") : "Sin sede")
                .estado(Boolean.TRUE.equals(usuario.getEstadoActivo()) ? "Activo" : "Inactivo")
                .build();
    }

    private UsuarioPortalReporteItemDTO mapUsuarioPortalToReporteItem(UsuarioCliente uc) {
        Cliente cliente = uc.getCliente();
        return UsuarioPortalReporteItemDTO.builder()
                .username(defaultIfBlank(uc.getUsername(), "—"))
                .empresa(cliente != null ? defaultIfBlank(cliente.getRazonSocial(), "—") : "—")
                .ruc(cliente != null ? defaultIfBlank(cliente.getNumeroDocumento(), "—") : "—")
                .representante(cliente != null ? defaultIfBlank(cliente.getContactoRepresentante(), "—") : "—")
                .correo(cliente != null ? defaultIfBlank(cliente.getCorreoContacto(), "—") : "—")
                .estado(Boolean.TRUE.equals(uc.getEstadoActivo()) ? "Activo" : "Suspendido")
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
        String cleaned = sanitizeXmlText(value);
        return cleaned != null && !cleaned.isBlank() ? cleaned.trim() : fallback;
    }

    private String sanitizeXmlText(String value) {
        if (value == null) return null;
        return value.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
    }

    // =========================================================================
    // 06. PROFORMA DE COTIZACIÓN B2B (PORTAL CLIENTE)
    // =========================================================================
    @Override
    public ByteArrayInputStream exportarCotizacionPortalPdf(PortalCotizacionDTO cotizacion) {
        if (cotizacion == null) {
            throw new IllegalArgumentException("La cotización no puede ser nula");
        }

        boolean pendienteLiquidacion = cotizacion.getIdUsuarioComercial() == null
                || safeDecimal(cotizacion.getTotalCotizado()).compareTo(BigDecimal.ZERO) <= 0;
        boolean conTransporte = Boolean.TRUE.equals(cotizacion.getServicioTransporte());
        boolean esExportacion = "EXPORTACION_DEFINITIVA".equalsIgnoreCase(cotizacion.getRegimenAduanero());

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String codigo = defaultIfBlank(
                cotizacion.getCodigoCotizacion(),
                cotizacion.getIdCotizacion() != null ? "COT-" + cotizacion.getIdCotizacion() : "COT-BORRADOR"
        );
        String estadoRaw = defaultIfBlank(cotizacion.getEstado(), "PENDIENTE").toUpperCase(Locale.ROOT);
        String estadoPagoRaw = defaultIfBlank(cotizacion.getEstadoPago(), "SIN_PAGO").toUpperCase(Locale.ROOT);

        Map<String, Object> variables = new HashMap<>();
        variables.put("codigoCotizacion", codigo);
        variables.put("fechaEmisionFmt", cotizacion.getFechaEmision() != null
                ? cotizacion.getFechaEmision().format(dateFormatter)
                : "En evaluación");
        variables.put("validezDias", cotizacion.getValidezDias() != null ? cotizacion.getValidezDias() : 30);
        variables.put("fechaGeneracion", LocalDateTime.now().format(REPORT_DATE_FORMATTER));

        variables.put("razonSocialCliente", defaultIfBlank(cotizacion.getRazonSocialCliente(), "Cliente B2B"));
        variables.put("rucCliente", defaultIfBlank(cotizacion.getRucCliente(), "—"));
        variables.put("aceptadaPorCliente", Boolean.TRUE.equals(cotizacion.getAceptadaPorCliente()));

        variables.put("estadoLabel", estadoRaw.replace('_', ' '));
        variables.put("estadoBadgeClass", resolvePortalEstadoBadgeClass(estadoRaw));
        variables.put("estadoPagoLabel", resolveEstadoPagoLabel(estadoPagoRaw));
        variables.put("estadoPagoBadgeClass", resolveEstadoPagoBadgeClass(estadoPagoRaw));

        variables.put("regimenLabel", resolveRegimenLabel(cotizacion.getRegimenAduanero()));
        variables.put("nombreTipoCarga", defaultIfBlank(cotizacion.getNombreTipoCarga(), "Carga General"));
        variables.put("nombreMercancia", defaultIfBlank(cotizacion.getNombreMercancia(), "Mercancía General"));
        variables.put("codigoMercancia", defaultIfBlank(cotizacion.getCodigoMercancia(), "N/A"));
        variables.put("puertoEnvio", defaultIfBlank(cotizacion.getNombrePuertoEnvio(), "Origen"));
        variables.put("puertoDestino", defaultIfBlank(cotizacion.getNombrePuertoDestino(), "Destino"));
        variables.put("asesorNombre", defaultIfBlank(cotizacion.getUsuarioComercialNombre(), "Pendiente de asignación"));

        variables.put("servicioTransporte", conTransporte);
        variables.put("transporteResumen", resolveTransporteResumen(conTransporte, cotizacion.getCostoTransporteInterno(), pendienteLiquidacion));
        variables.put("transporteRuta",
                defaultIfBlank(cotizacion.getOrigenProvincia(), defaultIfBlank(cotizacion.getOrigenDepartamento(), "Origen"))
                        + " -> "
                        + defaultIfBlank(cotizacion.getDestinoProvincia(), defaultIfBlank(cotizacion.getDestinoDepartamento(), "Destino")));

        variables.put("valorFobFmt", formatUsd(cotizacion.getValorFob()));
        variables.put("fleteFmt", formatPendingCost(cotizacion.getMontoFlete(), pendienteLiquidacion));
        variables.put("seguroFmt", formatPendingCost(cotizacion.getMontoSeguroEstimado(), pendienteLiquidacion));
        variables.put("honorariosFmt", formatPendingCost(cotizacion.getMontoHonorarios(), pendienteLiquidacion));
        variables.put("transporteInternoFmt", formatPendingCost(cotizacion.getCostoTransporteInterno(), pendienteLiquidacion));
        variables.put("tributosFmt", esExportacion
                ? "Inafecto ($0.00)"
                : formatPendingCost(cotizacion.getMontoTributos(), pendienteLiquidacion));
        variables.put("totalServicioFmt", pendienteLiquidacion
                ? "En liquidación ($0.00)"
                : formatUsd(cotizacion.getTotalCotizado()));

        variables.put("totalCotizadoFmt", formatUsd(cotizacion.getTotalCotizado()));
        variables.put("totalAbonadoFmt", formatUsd(cotizacion.getTotalAbonado()));
        variables.put("saldoPendienteFmt", formatUsd(cotizacion.getSaldoPendiente()));

        String motivoObs = sanitizeXmlText(cotizacion.getMotivoObservacion());
        String obsComercial = sanitizeXmlText(cotizacion.getObservaciones());
        variables.put("motivoObservacion",
                "OBSERVADA".equals(estadoRaw) && motivoObs != null && !motivoObs.isBlank()
                        ? motivoObs.trim()
                        : null);
        variables.put("observaciones",
                obsComercial != null && !obsComercial.isBlank()
                        ? obsComercial.trim()
                        : null);

        return renderHtmlToPdf("reports/proforma-cotizacion-b2b", variables);
    }

    private String formatPendingCost(BigDecimal value, boolean pendienteLiquidacion) {
        BigDecimal safe = safeDecimal(value);
        if (pendienteLiquidacion && safe.compareTo(BigDecimal.ZERO) <= 0) {
            return "Por asesor";
        }
        return formatUsd(safe);
    }

    private String resolveTransporteResumen(boolean conTransporte, BigDecimal costo, boolean pendienteLiquidacion) {
        if (!conTransporte) {
            return "No incluido";
        }
        BigDecimal safe = safeDecimal(costo);
        if (safe.compareTo(BigDecimal.ZERO) > 0) {
            return formatUsd(safe) + " (Flota SGA)";
        }
        return pendienteLiquidacion ? "Solicitado (Por asesor)" : "Incluido";
    }

    private String resolveRegimenLabel(String regimen) {
        if (regimen == null || regimen.isBlank()) {
            return "Régimen Aduanero";
        }
        return switch (regimen.trim().toUpperCase(Locale.ROOT)) {
            case "IMPORTACION_DEFINITIVA" -> "Importación para el Consumo";
            case "EXPORTACION_DEFINITIVA" -> "Exportación Definitiva";
            default -> regimen.replace('_', ' ');
        };
    }

    private String resolvePortalEstadoBadgeClass(String estado) {
        return switch (estado) {
            case "APROBADA" -> "badge-ok";
            case "PENDIENTE", "EN_REVISION" -> "badge-warn";
            case "OBSERVADA", "RECHAZADA", "ANULADA" -> "badge-danger";
            default -> "badge-neutral";
        };
    }

    private String resolveEstadoPagoLabel(String estadoPago) {
        return switch (estadoPago) {
            case "PAGADO_TOTAL" -> "Pagado Total";
            case "APROBADO" -> "Aprobado";
            case "ADELANTO_APROBADO" -> "Adelanto Aprobado";
            case "PENDIENTE_REVISION" -> "En Revisión Contable";
            case "SIN_PAGO" -> "Sin Pago";
            case "PENDIENTE" -> "Pendiente";
            case "OBSERVADO" -> "Pago Observado";
            case "RECHAZADO" -> "Pago Rechazado";
            default -> estadoPago.replace('_', ' ');
        };
    }

    private String resolveEstadoPagoBadgeClass(String estadoPago) {
        return switch (estadoPago) {
            case "PAGADO_TOTAL", "APROBADO", "ADELANTO_APROBADO" -> "badge-ok";
            case "PENDIENTE_REVISION" -> "badge-info";
            case "OBSERVADO", "RECHAZADO" -> "badge-danger";
            default -> "badge-warn";
        };
    }

    // =========================================================================
    // MOTOR DE RENDERIZADO HTML CORPORATIVO A PDF
    // =========================================================================
    private ByteArrayInputStream renderHtmlToPdf(String templateName, Map<String, Object> variables) {
        try {
            Context context = new Context();
            context.setVariables(variables);
            String html = sanitizeXmlText(templateEngine.process(templateName, context));

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
