package pe.sga.service.impl;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pe.sga.dto.response.AdminDashboardResponseDTO;
import pe.sga.dto.response.AuditoriaEventoDTO;
import pe.sga.entity.AuditoriaEvento;
import pe.sga.entity.Usuario;
import pe.sga.repository.AuditoriaEventoRepository;
import pe.sga.service.IAuditoriaService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuditoriaServiceImpl implements IAuditoriaService {

    private final AuditoriaEventoRepository auditoriaRepository;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrar(String modulo, String accion, String entidad, String idEntidad, String detalle, String resultado) {
        String actor = "SISTEMA";
        String rol = null;

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            Object principal = auth.getPrincipal();
            if (principal instanceof Usuario u) {
                actor = u.getUsername();
                rol = u.getRol() != null ? u.getRol().name() : null;
            } else if (auth.getName() != null && !"anonymousUser".equals(auth.getName())) {
                actor = auth.getName();
                rol = auth.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .findFirst()
                        .orElse(null);
            }
        }

        registrarConActor(actor, rol, modulo, accion, entidad, idEntidad, detalle, resultado);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void registrarConActor(String usuarioActor, String rolActor, String modulo, String accion,
                                  String entidad, String idEntidad, String detalle, String resultado) {
        try {
            String actor = (usuarioActor != null && !usuarioActor.isBlank()) ? truncar(usuarioActor, 80) : "SISTEMA";
            AuditoriaEvento evento = AuditoriaEvento.builder()
                    .fecha(LocalDateTime.now())
                    .usuarioActor(actor)
                    .rolActor(truncar(rolActor, 50))
                    .modulo(truncar(modulo != null ? modulo : "SISTEMA", 50))
                    .accion(truncar(accion != null ? accion : "EVENTO", 60))
                    .entidad(truncar(entidad, 60))
                    .idEntidad(truncar(idEntidad, 50))
                    .detalle(truncar(detalle, 500))
                    .ipOrigen(truncar(resolverIp(), 64))
                    .resultado(truncar(resultado != null ? resultado : "EXITO", 20))
                    .build();
            auditoriaRepository.save(evento);
        } catch (Exception ex) {
            log.warn("No se pudo registrar evento de auditoría ({}/{}): {}", modulo, accion, ex.getMessage());
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Object> listar(String buscar, String modulo, String resultado,
                                      LocalDateTime desde, LocalDateTime hasta, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        Page<AuditoriaEvento> pagina = auditoriaRepository.buscarConFiltros(
                buscar != null ? buscar.trim() : null,
                modulo != null ? modulo.trim() : null,
                resultado != null ? resultado.trim() : null,
                desde,
                hasta,
                PageRequest.of(safePage, safeSize)
        );

        Map<String, Object> res = new HashMap<>();
        res.put("items", pagina.getContent().stream().map(this::mapToDto).toList());
        res.put("totalElements", pagina.getTotalElements());
        res.put("totalPages", pagina.getTotalPages());
        res.put("page", pagina.getNumber());
        res.put("size", pagina.getSize());
        return res;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditoriaEventoDTO> listarRecientes(int limit) {
        int safeLimit = Math.min(Math.max(1, limit), 50);
        return auditoriaRepository.buscarConFiltros(
                        null, null, null, null, null, PageRequest.of(0, safeLimit))
                .getContent()
                .stream()
                .map(this::mapToDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdminDashboardResponseDTO.SerieDiariaDTO> obtenerSerieDiaria(int dias, String moduloFiltro) {
        int safeDias = Math.min(Math.max(1, dias), 90);
        LocalDate hoy = LocalDate.now();
        LocalDate inicio = hoy.minusDays(safeDias - 1L);
        List<AuditoriaEvento> eventos = auditoriaRepository
                .findByFechaGreaterThanEqualOrderByFechaDescIdEventoDesc(inicio.atStartOfDay());

        Map<LocalDate, long[]> mapa = new LinkedHashMap<>();
        for (int i = 0; i < safeDias; i++) {
            mapa.put(inicio.plusDays(i), new long[]{0L, 0L});
        }

        for (AuditoriaEvento e : eventos) {
            if (moduloFiltro != null && !moduloFiltro.isBlank() && !moduloFiltro.equalsIgnoreCase("TODOS")) {
                if (!moduloFiltro.equalsIgnoreCase(e.getModulo())) continue;
            }
            LocalDate d = e.getFecha().toLocalDate();
            long[] counts = mapa.get(d);
            if (counts != null) {
                if ("FALLO".equalsIgnoreCase(e.getResultado()) || "ERROR".equalsIgnoreCase(e.getResultado())) {
                    counts[1]++;
                } else {
                    counts[0]++;
                }
            }
        }

        List<AdminDashboardResponseDTO.SerieDiariaDTO> serie = new ArrayList<>();
        mapa.forEach((fecha, c) -> serie.add(AdminDashboardResponseDTO.SerieDiariaDTO.builder()
                .fecha(fecha.toString())
                .exitos(c[0])
                .fallos(c[1])
                .build()));
        return serie;
    }

    private AuditoriaEventoDTO mapToDto(AuditoriaEvento a) {
        return AuditoriaEventoDTO.builder()
                .idEvento(a.getIdEvento())
                .fecha(a.getFecha())
                .usuarioActor(a.getUsuarioActor())
                .rolActor(a.getRolActor())
                .modulo(a.getModulo())
                .accion(a.getAccion())
                .entidad(a.getEntidad())
                .idEntidad(a.getIdEntidad())
                .detalle(a.getDetalle())
                .ipOrigen(a.getIpOrigen())
                .resultado(a.getResultado())
                .build();
    }

    private String resolverIp() {
        try {
            if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
                HttpServletRequest req = attrs.getRequest();
                String forwarded = req.getHeader("X-Forwarded-For");
                if (forwarded != null && !forwarded.isBlank()) {
                    return forwarded.split(",")[0].trim();
                }
                return req.getRemoteAddr();
            }
        } catch (Exception ignored) {
        }
        return "local";
    }

    private String truncar(String val, int max) {
        if (val == null) return null;
        return val.length() <= max ? val : val.substring(0, max);
    }
}
