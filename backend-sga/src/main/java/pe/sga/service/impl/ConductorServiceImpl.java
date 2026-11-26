package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.dto.request.ConductorRequestDTO;
import pe.sga.dto.response.ConductorResponseDTO;
import pe.sga.entity.Conductor;
import pe.sga.entity.enums.CategoriaLicenciaMtc;
import pe.sga.entity.enums.EstadoOperativoConductor;
import pe.sga.entity.enums.EstadoTransporte;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.ConductorRepository;
import pe.sga.repository.DespachoTransporteRepository;
import pe.sga.service.IAuditoriaService;
import pe.sga.service.IConductorService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConductorServiceImpl implements IConductorService {

    private static final List<EstadoTransporte> ESTADOS_DESPACHO_ACTIVOS =
            List.of(EstadoTransporte.PROGRAMADO, EstadoTransporte.EN_RUTA);

    private final ConductorRepository conductorRepository;
    private final DespachoTransporteRepository despachoRepository;

    private IAuditoriaService auditoriaService;

    @Autowired(required = false)
    public void setAuditoriaService(IAuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    private void auditar(String accion, Integer idConductor, String detalle) {
        if (auditoriaService != null) {
            auditoriaService.registrar("TRANSPORTE", accion, "CONDUCTOR",
                    idConductor != null ? String.valueOf(idConductor) : null, detalle, "EXITO");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConductorResponseDTO> listarTodos() {
        return conductorRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConductorResponseDTO> listarActivos() {
        return conductorRepository.findByEstadoActivo(true).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConductorResponseDTO> listarDisponibles() {
        return conductorRepository
                .findByEstadoOperativoAndEstadoActivo(EstadoOperativoConductor.DISPONIBLE, true)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ConductorResponseDTO buscarPorId(Integer idConductor) {
        Conductor conductor = conductorRepository.findById(idConductor)
                .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + idConductor));
        return mapToResponse(conductor);
    }

    @Override
    @Transactional(readOnly = true)
    public ConductorResponseDTO buscarPorLicencia(String numeroLicencia) {
        String normalizada = numeroLicencia != null ? numeroLicencia.trim().toUpperCase() : "";
        Conductor conductor = conductorRepository.findByNumeroLicencia(normalizada)
                .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con licencia: " + numeroLicencia));
        return mapToResponse(conductor);
    }

    @Override
    @Transactional
    public ConductorResponseDTO crear(ConductorRequestDTO dto) {
        String licencia = dto.getNumeroLicencia().trim().toUpperCase();
        if (conductorRepository.existsByNumeroLicencia(licencia)) {
            throw new BadRequestException("Ya existe un conductor registrado con la licencia: " + licencia);
        }

        String documento = normalizeNullable(dto.getNumeroDocumento());
        if (documento != null && conductorRepository.existsByNumeroDocumento(documento)) {
            throw new BadRequestException("Ya existe un conductor registrado con el documento: " + documento);
        }

        Conductor conductor = Conductor.builder()
                .nombres(dto.getNombres().trim())
                .apellidos(dto.getApellidos().trim())
                .numeroDocumento(documento)
                .tipoLicencia(dto.getTipoLicencia() != null ? dto.getTipoLicencia() : CategoriaLicenciaMtc.A_IIIC)
                .numeroLicencia(licencia)
                .fechaVencimientoLicencia(dto.getFechaVencimientoLicencia())
                .telefono(normalizeNullable(dto.getTelefono()))
                .estadoOperativo(dto.getEstadoOperativo() != null
                        ? dto.getEstadoOperativo()
                        : EstadoOperativoConductor.DISPONIBLE)
                .estadoActivo(dto.getEstadoActivo() != null ? dto.getEstadoActivo() : true)
                .observaciones(normalizeNullable(dto.getObservaciones()))
                .build();

        Conductor guardado = conductorRepository.save(conductor);
        auditar("CREAR_CONDUCTOR", guardado.getIdConductor(),
                "Conductor creado: " + guardado.getNombres() + " " + guardado.getApellidos()
                        + " (" + guardado.getNumeroLicencia() + ")");
        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public ConductorResponseDTO actualizar(Integer idConductor, ConductorRequestDTO dto) {
        Conductor conductor = conductorRepository.findById(idConductor)
                .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + idConductor));

        String licencia = dto.getNumeroLicencia().trim().toUpperCase();
        if (!conductor.getNumeroLicencia().equalsIgnoreCase(licencia)) {
            if (conductorRepository.existsByNumeroLicencia(licencia)) {
                throw new BadRequestException("Ya existe otro conductor registrado con la licencia: " + licencia);
            }
            conductor.setNumeroLicencia(licencia);
        }

        String documento = normalizeNullable(dto.getNumeroDocumento());
        if (documento != null && conductorRepository.existsByNumeroDocumentoAndIdConductorNot(documento, idConductor)) {
            throw new BadRequestException("Ya existe otro conductor registrado con el documento: " + documento);
        }

        if (dto.getEstadoOperativo() != null
                && dto.getEstadoOperativo() != conductor.getEstadoOperativo()
                && (dto.getEstadoOperativo() == EstadoOperativoConductor.DESCANSO
                        || dto.getEstadoOperativo() == EstadoOperativoConductor.SUSPENDIDO)
                && despachoRepository.existsByConductor_IdConductorAndEstadoTransporteIn(
                        idConductor, ESTADOS_DESPACHO_ACTIVOS)) {
            throw new BadRequestException(
                    "No se puede cambiar el estado operativo a " + dto.getEstadoOperativo()
                            + " porque el conductor tiene un despacho programado o en ruta.");
        }

        conductor.setNombres(dto.getNombres().trim());
        conductor.setApellidos(dto.getApellidos().trim());
        conductor.setNumeroDocumento(documento);
        if (dto.getTipoLicencia() != null) {
            conductor.setTipoLicencia(dto.getTipoLicencia());
        }
        conductor.setFechaVencimientoLicencia(dto.getFechaVencimientoLicencia());
        conductor.setTelefono(normalizeNullable(dto.getTelefono()));
        if (dto.getEstadoOperativo() != null) {
            conductor.setEstadoOperativo(dto.getEstadoOperativo());
        }
        if (dto.getEstadoActivo() != null) {
            if (!dto.getEstadoActivo() && despachoRepository.existsByConductor_IdConductorAndEstadoTransporteIn(
                    idConductor, ESTADOS_DESPACHO_ACTIVOS)) {
                throw new BadRequestException(
                        "No se puede desactivar un conductor asignado a un despacho programado o en ruta.");
            }
            conductor.setEstadoActivo(dto.getEstadoActivo());
        }
        conductor.setObservaciones(normalizeNullable(dto.getObservaciones()));

        Conductor guardado = conductorRepository.save(conductor);
        auditar("ACTUALIZAR_CONDUCTOR", guardado.getIdConductor(),
                "Conductor actualizado: " + guardado.getNombres() + " " + guardado.getApellidos()
                        + " (" + guardado.getNumeroLicencia() + ")");
        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public void eliminar(Integer idConductor) {
        Conductor conductor = conductorRepository.findById(idConductor)
                .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + idConductor));

        if (despachoRepository.existsByConductor_IdConductorAndEstadoTransporteIn(
                idConductor, ESTADOS_DESPACHO_ACTIVOS)) {
            throw new BadRequestException(
                    "No se puede dar de baja a un conductor asignado a un despacho programado o en ruta.");
        }

        conductor.setEstadoActivo(false);
        conductorRepository.save(conductor);
        auditar("DESACTIVAR_CONDUCTOR", conductor.getIdConductor(),
                "Conductor desactivado: " + conductor.getNombres() + " " + conductor.getApellidos());
    }

    @Override
    @Transactional
    public void reactivar(Integer idConductor) {
        Conductor conductor = conductorRepository.findById(idConductor)
                .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + idConductor));
        conductor.setEstadoActivo(true);
        conductorRepository.save(conductor);
        auditar("REACTIVAR_CONDUCTOR", conductor.getIdConductor(),
                "Conductor reactivado: " + conductor.getNombres() + " " + conductor.getApellidos());
    }

    private String normalizeNullable(String val) {
        if (val == null) {
            return null;
        }
        String trimmed = val.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private ConductorResponseDTO mapToResponse(Conductor c) {
        return ConductorResponseDTO.builder()
                .idConductor(c.getIdConductor())
                .nombres(c.getNombres())
                .apellidos(c.getApellidos())
                .nombreCompleto(c.getNombres() + " " + c.getApellidos())
                .numeroDocumento(c.getNumeroDocumento())
                .tipoLicencia(c.getTipoLicencia())
                .numeroLicencia(c.getNumeroLicencia())
                .fechaVencimientoLicencia(c.getFechaVencimientoLicencia())
                .telefono(c.getTelefono())
                .estadoOperativo(c.getEstadoOperativo())
                .estadoActivo(c.getEstadoActivo())
                .observaciones(c.getObservaciones())
                .build();
    }
}
