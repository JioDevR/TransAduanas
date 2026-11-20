package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.dto.response.AdminAlmacenamientoDTO;
import pe.sga.dto.response.AdminDashboardResponseDTO;
import pe.sga.dto.response.AdminSistemaEstadoDTO;
import pe.sga.dto.response.RolMatrizDTO;
import pe.sga.entity.DocumentoAdjunto;
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
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
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

        Path storagePath = resolverRutaStorage();
        boolean storageDisponible = Files.exists(storagePath) && Files.isDirectory(storagePath);
        Map<String, Path> archivosDiscoPorRuta = mapearArchivosEnDisco(storagePath, storageDisponible);
        long tamanoBytes = calcularTamanoTotalBytes(archivosDiscoPorRuta.values());
        EspacioDisco espacio = obtenerEspacioDisco(storagePath, storageDisponible);

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
                .storageArchivosDisco(archivosDiscoPorRuta.size())
                .storageTamanoBytes(tamanoBytes)
                .storageDocumentosRegistradosBd(docsBd)
                .discoEspacioLibreMb(espacio.libreMb())
                .discoEspacioTotalMb(espacio.totalMb())
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
        long portalInactivos = portalUsers.size() - portalActivos;
        long portalCambioPendiente = portalUsers.stream().filter(uc -> Boolean.TRUE.equals(uc.getDebeCambiarPassword())).count();
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
                .portalUsuariosInactivos(portalInactivos)
                .portalCambioPendiente(portalCambioPendiente)
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

    @Override
    @Transactional(readOnly = true)
    public AdminAlmacenamientoDTO obtenerDiagnosticoAlmacenamiento(
            String buscar, String entidad, String categoria, String integridad, int page, int size) {

        Path storagePath = resolverRutaStorage();
        boolean disponible = Files.exists(storagePath) && Files.isDirectory(storagePath);

        EspacioDisco espacio = obtenerEspacioDisco(storagePath, disponible);
        Map<String, Path> archivosDiscoPorRuta = mapearArchivosEnDisco(storagePath, disponible);
        long storageTamanoBytes = calcularTamanoTotalBytes(archivosDiscoPorRuta.values());

        List<DocumentoAdjunto> documentosBd = documentoRepository.findAll();
        List<AdminAlmacenamientoDTO.ArchivoStorageDTO> inventario = new ArrayList<>();
        Map<String, long[]> aggEntidad = new LinkedHashMap<>();
        Map<String, long[]> aggCategoria = new LinkedHashMap<>();
        Map<String, long[]> aggExtension = new LinkedHashMap<>();

        ConciliacionBd conc = conciliarDocumentosBd(
                documentosBd, archivosDiscoPorRuta, inventario, aggEntidad, aggCategoria, aggExtension);
        long[] huerfanos = recopilarArchivosHuerfanos(
                archivosDiscoPorRuta, conc.rutasEnBd(), inventario, aggExtension);

        inventario.sort(Comparator.comparing(
                AdminAlmacenamientoDTO.ArchivoStorageDTO::getFechaCreacion,
                Comparator.nullsLast(Comparator.reverseOrder())));

        List<AdminAlmacenamientoDTO.ArchivoStorageDTO> filtrados =
                filtrarInventario(inventario, buscar, entidad, categoria, integridad);

        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        int totalElements = filtrados.size();
        int totalPages = totalElements == 0 ? 0 : (int) Math.ceil((double) totalElements / safeSize);
        int fromIdx = Math.min(safePage * safeSize, totalElements);
        int toIdx = Math.min(fromIdx + safeSize, totalElements);

        return AdminAlmacenamientoDTO.builder()
                .directorioRaiz(storagePath.toString())
                .disponible(disponible)
                .discoTotalMb(espacio.totalMb())
                .discoLibreMb(espacio.libreMb())
                .discoUsadoMb(espacio.usadoMb())
                .discoOcupacionPct(espacio.ocupacionPct())
                .storageTamanoBytes(storageTamanoBytes)
                .archivosEnDisco(archivosDiscoPorRuta.size())
                .registrosBdTotales(documentosBd.size())
                .registrosBdActivos(conc.bdActivos())
                .registrosBdInactivos(conc.bdInactivos())
                .bytesRegistradosBdActivos(conc.bytesBdActivos())
                .archivosSincronizados(conc.sincronizados())
                .archivosFaltantesEnDisco(conc.faltantesDisco())
                .archivosHuerfanosEnDisco(huerfanos[0])
                .bytesHuerfanosEnDisco(huerfanos[1])
                .porEntidad(ordenarGrupos(aggEntidad))
                .porCategoria(ordenarGrupos(aggCategoria))
                .porExtension(ordenarGrupos(aggExtension))
                .items(filtrados.subList(fromIdx, toIdx))
                .totalElements(totalElements)
                .totalPages(totalPages)
                .page(safePage)
                .size(safeSize)
                .build();
    }

    private ConciliacionBd conciliarDocumentosBd(
            List<DocumentoAdjunto> documentosBd,
            Map<String, Path> archivosDiscoPorRuta,
            List<AdminAlmacenamientoDTO.ArchivoStorageDTO> inventario,
            Map<String, long[]> aggEntidad,
            Map<String, long[]> aggCategoria,
            Map<String, long[]> aggExtension) {
        Set<String> rutasEnBd = new HashSet<>();
        long bdActivos = 0;
        long bdInactivos = 0;
        long bytesBdActivos = 0;
        long sincronizados = 0;
        long faltantesDisco = 0;

        for (DocumentoAdjunto doc : documentosBd) {
            boolean activo = Boolean.TRUE.equals(doc.getActivo());
            long tamano = doc.getTamanoBytes() != null ? doc.getTamanoBytes() : 0L;
            if (activo) {
                bdActivos++;
                bytesBdActivos += tamano;
            } else {
                bdInactivos++;
            }

            String relPath = normalizarRutaRelativa(doc.getRutaAlmacenamiento());
            if (!relPath.isBlank()) {
                rutasEnBd.add(relPath);
            }

            Path archivoDisco = archivosDiscoPorRuta.get(relPath);
            boolean existeEnDisco = archivoDisco != null && Files.isRegularFile(archivoDisco);
            if (existeEnDisco) {
                tamano = obtenerTamanoArchivo(archivoDisco, tamano);
            }

            String estadoInt;
            if (!existeEnDisco) {
                estadoInt = "FALTANTE_DISCO";
                if (activo) faltantesDisco++;
            } else if (!activo) {
                estadoInt = "BAJA_LOGICA";
            } else {
                estadoInt = "SINCRONIZADO";
                sincronizados++;
            }

            String entKey = doc.getEntidadTipo() != null ? doc.getEntidadTipo().name() : "GENERAL";
            String catKey = doc.getCategoria() != null ? doc.getCategoria().name() : "GENERAL";
            String extKey = (doc.getExtension() != null && !doc.getExtension().isBlank())
                    ? doc.getExtension().trim().toUpperCase()
                    : extraerExtension(doc.getNombreOriginal());

            if (activo) {
                acumularGrupo(aggEntidad, entKey, tamano);
                acumularGrupo(aggCategoria, catKey, tamano);
                acumularGrupo(aggExtension, extKey, tamano);
            }

            inventario.add(AdminAlmacenamientoDTO.ArchivoStorageDTO.builder()
                    .idDocumento(doc.getIdDocumento())
                    .nombreOriginal(doc.getNombreOriginal())
                    .rutaRelativa(relPath)
                    .extension(extKey)
                    .formatoMime(doc.getFormatoMime())
                    .entidadTipo(entKey)
                    .categoria(catKey)
                    .tamanoBytes(tamano)
                    .activoBd(activo)
                    .estadoIntegridad(estadoInt)
                    .usuarioCreador(doc.getUsuarioCreador() != null ? doc.getUsuarioCreador().getUsername() : "SISTEMA")
                    .fechaCreacion(doc.getFechaCreacion())
                    .build());
        }

        return new ConciliacionBd(rutasEnBd, bdActivos, bdInactivos, bytesBdActivos, sincronizados, faltantesDisco);
    }

    private long[] recopilarArchivosHuerfanos(
            Map<String, Path> archivosDiscoPorRuta,
            Set<String> rutasEnBd,
            List<AdminAlmacenamientoDTO.ArchivoStorageDTO> inventario,
            Map<String, long[]> aggExtension) {
        long huerfanosCount = 0;
        long bytesHuerfanos = 0;

        for (Map.Entry<String, Path> entry : archivosDiscoPorRuta.entrySet()) {
            if (rutasEnBd.contains(entry.getKey())) {
                continue;
            }
            Path p = entry.getValue();
            long tamano = obtenerTamanoArchivo(p, 0L);
            LocalDateTime modTime = LocalDateTime.now();
            try {
                modTime = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(Files.getLastModifiedTime(p).toMillis()),
                        ZoneId.systemDefault());
            } catch (IOException ignored) {
            }
            huerfanosCount++;
            bytesHuerfanos += tamano;

            String nombre = p.getFileName() != null ? p.getFileName().toString() : entry.getKey();
            String ext = extraerExtension(nombre);
            acumularGrupo(aggExtension, ext, tamano);

            inventario.add(AdminAlmacenamientoDTO.ArchivoStorageDTO.builder()
                    .idDocumento(null)
                    .nombreOriginal(nombre)
                    .rutaRelativa(entry.getKey())
                    .extension(ext)
                    .formatoMime("application/octet-stream")
                    .entidadTipo("SIN_REGISTRO")
                    .categoria("HUERFANO")
                    .tamanoBytes(tamano)
                    .activoBd(false)
                    .estadoIntegridad("HUERFANO")
                    .usuarioCreador("DISCO_LOCAL")
                    .fechaCreacion(modTime)
                    .build());
        }
        return new long[]{huerfanosCount, bytesHuerfanos};
    }

    private List<AdminAlmacenamientoDTO.ArchivoStorageDTO> filtrarInventario(
            List<AdminAlmacenamientoDTO.ArchivoStorageDTO> inventario,
            String buscar, String entidad, String categoria, String integridad) {
        String buscaNorm = buscar != null ? buscar.trim().toLowerCase() : "";
        String entNorm = entidad != null ? entidad.trim() : "";
        String catNorm = categoria != null ? categoria.trim() : "";
        String intNorm = integridad != null ? integridad.trim() : "";

        return inventario.stream()
                .filter(a -> buscaNorm.isEmpty()
                        || (a.getNombreOriginal() != null && a.getNombreOriginal().toLowerCase().contains(buscaNorm))
                        || (a.getRutaRelativa() != null && a.getRutaRelativa().toLowerCase().contains(buscaNorm))
                        || (a.getUsuarioCreador() != null && a.getUsuarioCreador().toLowerCase().contains(buscaNorm)))
                .filter(a -> entNorm.isEmpty() || "ALL".equalsIgnoreCase(entNorm)
                        || entNorm.equalsIgnoreCase(a.getEntidadTipo()))
                .filter(a -> catNorm.isEmpty() || "ALL".equalsIgnoreCase(catNorm)
                        || catNorm.equalsIgnoreCase(a.getCategoria()))
                .filter(a -> intNorm.isEmpty() || "ALL".equalsIgnoreCase(intNorm)
                        || intNorm.equalsIgnoreCase(a.getEstadoIntegridad()))
                .toList();
    }

    private EspacioDisco obtenerEspacioDisco(Path storagePath, boolean disponible) {
        long libreMb = 0;
        long totalMb = 0;
        if (disponible) {
            try {
                FileStore store = Files.getFileStore(storagePath);
                libreMb = store.getUsableSpace() / (1024 * 1024);
                totalMb = store.getTotalSpace() / (1024 * 1024);
            } catch (IOException ignored) {
            }
        }
        long usadoMb = Math.max(0L, totalMb - libreMb);
        int ocupacionPct = totalMb > 0 ? (int) Math.min(100, Math.round((usadoMb * 100.0) / totalMb)) : 0;
        return new EspacioDisco(libreMb, totalMb, usadoMb, ocupacionPct);
    }

    private long calcularTamanoTotalBytes(Collection<Path> paths) {
        long total = 0;
        for (Path p : paths) {
            total += obtenerTamanoArchivo(p, 0L);
        }
        return total;
    }

    private long obtenerTamanoArchivo(Path p, long fallback) {
        try {
            return Files.size(p);
        } catch (IOException ignored) {
            return fallback;
        }
    }

    private record EspacioDisco(long libreMb, long totalMb, long usadoMb, int ocupacionPct) {}

    private record ConciliacionBd(
            Set<String> rutasEnBd,
            long bdActivos,
            long bdInactivos,
            long bytesBdActivos,
            long sincronizados,
            long faltantesDisco) {}

    @Override
    @Transactional
    public Map<String, Object> depurarArchivosHuerfanos() {
        Path storagePath = resolverRutaStorage();
        boolean disponible = Files.exists(storagePath) && Files.isDirectory(storagePath);

        long eliminados = 0;
        long bytesLiberados = 0;

        if (disponible) {
            Set<String> rutasEnBd = new HashSet<>();
            for (DocumentoAdjunto doc : documentoRepository.findAll()) {
                String rel = normalizarRutaRelativa(doc.getRutaAlmacenamiento());
                if (!rel.isBlank()) {
                    rutasEnBd.add(rel);
                }
            }

            Map<String, Path> archivosDiscoPorRuta = mapearArchivosEnDisco(storagePath, true);
            for (Map.Entry<String, Path> entry : archivosDiscoPorRuta.entrySet()) {
                if (rutasEnBd.contains(entry.getKey())) {
                    continue;
                }
                Path archivo = entry.getValue().normalize();
                if (!archivo.startsWith(storagePath)) {
                    continue;
                }
                try {
                    long tam = Files.size(archivo);
                    if (Files.deleteIfExists(archivo)) {
                        eliminados++;
                        bytesLiberados += tam;
                    }
                } catch (IOException ex) {
                    log.warn("No se pudo eliminar archivo huérfano {}: {}", archivo, ex.getMessage());
                }
            }

            limpiarDirectoriosVacios(storagePath);
        }

        auditoriaService.registrar("DOCUMENTOS", "DEPURAR_ARCHIVOS_HUERFANOS", "ALMACENAMIENTO",
                null, "Depuración de almacenamiento local: " + eliminados + " archivos huérfanos eliminados ("
                        + bytesLiberados + " bytes liberados)", "EXITO");

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("archivosEliminados", eliminados);
        res.put("bytesLiberados", bytesLiberados);
        res.put("timestamp", LocalDateTime.now());
        return res;
    }

    private Path resolverRutaStorage() {
        String raw = uploadDir != null && !uploadDir.isBlank() ? uploadDir : "./uploads/documentos";
        return Paths.get(raw).toAbsolutePath().normalize();
    }

    private Map<String, Path> mapearArchivosEnDisco(Path storagePath, boolean disponible) {
        Map<String, Path> map = new LinkedHashMap<>();
        if (!disponible) {
            return map;
        }
        try (Stream<Path> walk = Files.walk(storagePath)) {
            walk.filter(Files::isRegularFile).forEach(p -> {
                String rel = storagePath.relativize(p.toAbsolutePath().normalize())
                        .toString()
                        .replace('\\', '/');
                map.put(rel, p);
            });
        } catch (IOException ex) {
            log.warn("Error recorriendo almacenamiento en {}: {}", storagePath, ex.getMessage());
        }
        return map;
    }

    private void limpiarDirectoriosVacios(Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder())
                    .filter(Files::isDirectory)
                    .filter(dir -> !dir.equals(root))
                    .forEach(dir -> {
                        try (Stream<Path> children = Files.list(dir)) {
                            if (children.findAny().isEmpty()) {
                                Files.deleteIfExists(dir);
                            }
                        } catch (IOException ignored) {
                        }
                    });
        } catch (IOException ignored) {
        }
    }

    private String normalizarRutaRelativa(String raw) {
        if (raw == null) return "";
        String norm = raw.trim().replace('\\', '/');
        while (norm.startsWith("/")) {
            norm = norm.substring(1);
        }
        return norm;
    }

    private String extraerExtension(String filename) {
        if (filename == null) return "SIN_EXT";
        int idx = filename.lastIndexOf('.');
        if (idx < 0 || idx == filename.length() - 1) return "SIN_EXT";
        return filename.substring(idx + 1).trim().toUpperCase();
    }

    private void acumularGrupo(Map<String, long[]> map, String clave, long bytes) {
        long[] stats = map.computeIfAbsent(clave, k -> new long[2]);
        stats[0] += 1L;
        stats[1] += bytes;
    }

    private List<AdminAlmacenamientoDTO.GrupoStorageDTO> ordenarGrupos(Map<String, long[]> map) {
        return map.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[1], a.getValue()[1]))
                .map(e -> AdminAlmacenamientoDTO.GrupoStorageDTO.builder()
                        .clave(e.getKey())
                        .etiqueta(e.getKey().replace('_', ' '))
                        .cantidad(e.getValue()[0])
                        .tamanoBytes(e.getValue()[1])
                        .build())
                .toList();
    }
}
