package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.dto.response.AdminDashboardResponseDTO;
import pe.sga.dto.response.AdminSistemaEstadoDTO;
import pe.sga.dto.response.RolMatrizDTO;
import pe.sga.entity.Usuario;
import pe.sga.entity.UsuarioCliente;
import pe.sga.entity.enums.RolUsuario;
import pe.sga.repository.AuditoriaEventoRepository;
import pe.sga.repository.IDocumentoAdjuntoRepository;
import pe.sga.repository.UsuarioClienteRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.IAdminLogsService;
import pe.sga.service.IAdminSistemaService;
import pe.sga.service.IAuditoriaService;

import javax.sql.DataSource;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.file.*;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminSistemaServiceImpl implements IAdminSistemaService {

    private final DataSource dataSource;
    private final Environment environment;
    private final UsuarioRepository usuarioRepository;
    private final UsuarioClienteRepository usuarioClienteRepository;
    private final IDocumentoAdjuntoRepository documentoRepository;
    private final AuditoriaEventoRepository auditoriaRepository;
    private final IAdminLogsService adminLogsService;
    private final IAuditoriaService auditoriaService;

    @Value("${app.storage.upload-dir:./uploads/documentos}")
    private String uploadDir;

    @Override
    @Transactional(readOnly = true)
    public AdminSistemaEstadoDTO obtenerEstadoSistema() {
        boolean bdConectada = false;
        String bdMotor = "Desconocido";
        String bdVersion = "-";
        long bdLatenciaMs = -1;

        long t0 = System.nanoTime();
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("SELECT 1");
            bdLatenciaMs = Math.max(1L, (System.nanoTime() - t0) / 1_000_000L);
            DatabaseMetaData meta = conn.getMetaData();
            bdConectada = true;
            bdMotor = meta.getDatabaseProductName();
            bdVersion = meta.getDatabaseProductVersion();
        } catch (Exception ex) {
            log.warn("Error verificando conexión BD en estado del sistema: {}", ex.getMessage());
        }

        Runtime rt = Runtime.getRuntime();
        long maxMemMb = rt.maxMemory() / (1024 * 1024);
        long usedMemMb = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
        int memPct = maxMemMb > 0 ? (int) Math.min(100, Math.round((usedMemMb * 100.0) / maxMemMb)) : 0;

        Path storagePath = Paths.get(uploadDir != null && !uploadDir.isBlank() ? uploadDir : "./uploads/documentos")
                .toAbsolutePath().normalize();
        boolean storageDisponible = Files.exists(storagePath) && Files.isDirectory(storagePath);
        AtomicLong archivosDisco = new AtomicLong(0);
        AtomicLong tamanoBytes = new AtomicLong(0);
        long discoLibreMb = 0;
        long discoTotalMb = 0;

        if (storageDisponible) {
            try (Stream<Path> walk = Files.walk(storagePath)) {
                walk.filter(Files::isRegularFile).forEach(p -> {
                    archivosDisco.incrementAndGet();
                    try {
                        tamanoBytes.addAndGet(Files.size(p));
                    } catch (IOException ignored) {
                    }
                });
            } catch (IOException ex) {
                log.warn("No se pudo recorrer directorio {}: {}", storagePath, ex.getMessage());
            }
            try {
                FileStore store = Files.getFileStore(storagePath);
                discoLibreMb = store.getUsableSpace() / (1024 * 1024);
                discoTotalMb = store.getTotalSpace() / (1024 * 1024);
            } catch (IOException ignored) {
            }
        }

        long docsBd = 0;
        try {
            docsBd = documentoRepository.findByActivoTrueOrderByFechaCreacionDesc().size();
        } catch (Exception ignored) {
        }

        String[] perfiles = environment.getActiveProfiles();
        String perfilActivo = perfiles.length > 0 ? String.join(", ", perfiles) : "default";
        String appVersion = Optional.ofNullable(getClass().getPackage().getImplementationVersion()).orElse("0.5.1");

        return AdminSistemaEstadoDTO.builder()
                .appNombre(environment.getProperty("spring.application.name", "sga-backend"))
                .appVersion(appVersion)
                .perfilActivo(perfilActivo)
                .javaVersion(System.getProperty("java.version"))
                .osNombre(System.getProperty("os.name") + " " + System.getProperty("os.arch"))
                .uptimeSegundos(ManagementFactory.getRuntimeMXBean().getUptime() / 1000L)
                .timestamp(LocalDateTime.now())
                .bdConectada(bdConectada)
                .bdMotor(bdMotor)
                .bdVersion(bdVersion)
                .bdLatenciaMs(bdLatenciaMs)
                .jvmHeapUsadoMb(usedMemMb)
                .jvmHeapMaximoMb(maxMemMb)
                .jvmHeapPorcentaje(memPct)
                .storageDirectorio(storagePath.toString())
                .storageDisponible(storageDisponible)
                .storageArchivosDisco(archivosDisco.get())
                .storageTamanoBytes(tamanoBytes.get())
                .storageDocumentosRegistradosBd(docsBd)
                .discoEspacioLibreMb(discoLibreMb)
                .discoEspacioTotalMb(discoTotalMb)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminDashboardResponseDTO obtenerDashboard(int dias, String moduloFiltro) {
        int safeDias = Math.min(Math.max(1, dias), 90);
        AdminSistemaEstadoDTO sistema = obtenerEstadoSistema();

        List<Usuario> usuarios = usuarioRepository.findAll();
        long usuariosActivos = usuarios.stream().filter(u -> Boolean.TRUE.equals(u.getEstadoActivo())).count();
        long usuariosInactivos = usuarios.size() - usuariosActivos;

        Map<String, Long> distRoles = new LinkedHashMap<>();
        for (Usuario u : usuarios) {
            String rol = u.getRol() != null ? u.getRol().name() : "SIN_ROL";
            distRoles.put(rol, distRoles.getOrDefault(rol, 0L) + 1L);
        }

        List<UsuarioCliente> portalUsers = usuarioClienteRepository.findAllWithCliente();
        long portalActivos = portalUsers.stream().filter(uc -> Boolean.TRUE.equals(uc.getEstadoActivo())).count();
        long empresasVinculadas = portalUsers.stream()
                .map(uc -> uc.getCliente() != null ? uc.getCliente().getIdCliente() : null)
                .filter(Objects::nonNull)
                .distinct()
                .count();

        Map<String, Object> logAll = adminLogsService.getLogs("all", 50);
        Map<String, Object> logErr = adminLogsService.getLogs("error", 50);

        LocalDateTime desde = LocalDate.now().minusDays(safeDias - 1L).atStartOfDay();
        var eventosPeriodo = auditoriaRepository.findByFechaGreaterThanEqualOrderByFechaDescIdEventoDesc(desde)
                .stream()
                .filter(e -> moduloFiltro == null || moduloFiltro.isBlank() || "TODOS".equalsIgnoreCase(moduloFiltro)
                        || moduloFiltro.equalsIgnoreCase(e.getModulo()))
                .toList();

        long exitos = eventosPeriodo.stream()
                .filter(e -> !"FALLO".equalsIgnoreCase(e.getResultado()) && !"ERROR".equalsIgnoreCase(e.getResultado()))
                .count();
        long fallos = eventosPeriodo.size() - exitos;
        long loginsFallidos = eventosPeriodo.stream()
                .filter(e -> "LOGIN_FALLIDO".equalsIgnoreCase(e.getAccion()))
                .count();

        return AdminDashboardResponseDTO.builder()
                .sistema(sistema)
                .usuariosTotal(usuarios.size())
                .usuariosActivos(usuariosActivos)
                .usuariosInactivos(usuariosInactivos)
                .distribucionRoles(distRoles)
                .portalUsuariosTotal(portalUsers.size())
                .portalUsuariosActivos(portalActivos)
                .portalEmpresasVinculadas(empresasVinculadas)
                .logArchivoNombre(String.valueOf(logAll.getOrDefault("fileName", "sga-backend.log")))
                .logSizeBytes(((Number) logAll.getOrDefault("sizeBytes", 0L)).longValue())
                .logTotalLineas(((Number) logAll.getOrDefault("totalLines", 0L)).longValue())
                .logErroresLineas(((Number) logErr.getOrDefault("totalLines", 0L)).longValue())
                .diasPeriodo(safeDias)
                .auditoriaTotalPeriodo(eventosPeriodo.size())
                .auditoriaExitosPeriodo(exitos)
                .auditoriaFallosPeriodo(fallos)
                .loginsFallidosPeriodo(loginsFallidos)
                .actividadDiaria(auditoriaService.obtenerSerieDiaria(safeDias, moduloFiltro))
                .eventosRecientes(auditoriaService.listarRecientes(8))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolMatrizDTO> obtenerMatrizRoles() {
        List<Usuario> usuarios = usuarioRepository.findAll();
        List<RolMatrizDTO> matriz = new ArrayList<>();

        for (RolUsuario rol : RolUsuario.values()) {
            long activos = usuarios.stream()
                    .filter(u -> u.getRol() == rol && Boolean.TRUE.equals(u.getEstadoActivo()))
                    .count();
            long inactivos = usuarios.stream()
                    .filter(u -> u.getRol() == rol && !Boolean.TRUE.equals(u.getEstadoActivo()))
                    .count();

            matriz.add(RolMatrizDTO.builder()
                    .codigo(rol.name())
                    .etiqueta(rol.getEtiqueta())
                    .descripcion(descripcionRol(rol))
                    .modulosPermitidos(modulosPorRol(rol))
                    .capacidades(capacidadesPorRol(rol))
                    .conmutaModulos(rol == RolUsuario.ADMINISTRADOR_TI || rol == RolUsuario.GERENTE_GENERAL)
                    .usuariosActivos(activos)
                    .usuariosInactivos(inactivos)
                    .build());
        }
        return matriz;
    }

    private String descripcionRol(RolUsuario rol) {
        return switch (rol) {
            case ADMINISTRADOR_TI -> "Gobernanza técnica, cuentas internas y B2B, auditoría, logs y diagnóstico.";
            case GERENTE_GENERAL -> "Supervisión ejecutiva transversal, sedes, catálogos maestros y KPIs.";
            case AGENTE_COMERCIAL -> "Prospección de clientes, cotizaciones, registro de pagos iniciales y reportes.";
            case SUPERVISOR_ADUANERO -> "Revisión técnica, aprobación aduanera, apertura de expedientes y supervisión.";
            case AGENTE_ADUANERO -> "Gestión operativa de expedientes, canales SUNAT, diligencias e inspecciones.";
            case SUPERVISOR_TRANSPORTE -> "Gestión de flota, conductores, despachos locales y cabina de ruta.";
            case CONTADOR -> "Validación financiera, conciliación de pagos y liquidaciones finales.";
            case ASESOR_LEGAL -> "Auditoría normativa, revisión de expedientes y respaldo documental.";
        };
    }

    private List<String> modulosPorRol(RolUsuario rol) {
        return switch (rol) {
            case ADMINISTRADOR_TI -> List.of("Administración TI", "Gerencia General", "Comercial", "Gestión Aduanera", "Agente Aduanero", "Transporte y Flota", "Finanzas", "Legal y Auditoría");
            case GERENTE_GENERAL -> List.of("Gerencia General", "Administración TI (Lectura)", "Comercial", "Gestión Aduanera", "Agente Aduanero", "Transporte y Flota", "Finanzas", "Legal y Auditoría");
            case AGENTE_COMERCIAL -> List.of("Recepción Comercial");
            case SUPERVISOR_ADUANERO -> List.of("Gestión Aduanera", "Agente Aduanero");
            case AGENTE_ADUANERO -> List.of("Agente Aduanero", "Gestión Aduanera");
            case SUPERVISOR_TRANSPORTE -> List.of("Transporte y Flota");
            case CONTADOR -> List.of("Finanzas");
            case ASESOR_LEGAL -> List.of("Legal y Auditoría");
        };
    }

    private List<String> capacidadesPorRol(RolUsuario rol) {
        return switch (rol) {
            case ADMINISTRADOR_TI -> List.of("CRUD Usuarios Internos/B2B", "Reset Contraseñas", "Logs y Auditoría TI", "Diagnóstico Sistema", "Acceso Transversal");
            case GERENTE_GENERAL -> List.of("Lectura Usuarios y Roles", "Gestión Sedes/Catálogos", "Lectura Operativa Global");
            case AGENTE_COMERCIAL -> List.of("CRUD Clientes", "CRUD Cotizaciones", "Registro Pagos Iniciales");
            case SUPERVISOR_ADUANERO -> List.of("Aprobación Cotizaciones", "Apertura Expedientes", "Control Almacén", "Liquidaciones");
            case AGENTE_ADUANERO -> List.of("Aprobación Cotizaciones", "Gestión Expedientes", "Control Almacén");
            case SUPERVISOR_TRANSPORTE -> List.of("CRUD Flota y Conductores", "CRUD Despachos", "Seguimiento Ruta");
            case CONTADOR -> List.of("Validación Pagos", "CRUD Liquidaciones Financieras", "Conciliación");
            case ASESOR_LEGAL -> List.of("Auditoría Expedientes", "Lectura Almacén y Liquidaciones", "Documentos");
        };
    }
}
