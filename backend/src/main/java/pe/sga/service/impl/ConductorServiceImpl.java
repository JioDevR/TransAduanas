package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.dto.request.ConductorRequestDTO;
import pe.sga.dto.response.ConductorResponseDTO;
import pe.sga.entity.Conductor;
import pe.sga.entity.Usuario;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.ConductorRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.IConductorService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ConductorServiceImpl implements IConductorService {

    private final ConductorRepository conductorRepository;
    private final UsuarioRepository usuarioRepository;

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
    public ConductorResponseDTO buscarPorId(Integer idConductor) {
        Conductor conductor = conductorRepository.findById(idConductor)
                .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + idConductor));
        return mapToResponse(conductor);
    }

    @Override
    @Transactional(readOnly = true)
    public ConductorResponseDTO buscarPorLicencia(String numeroLicencia) {
        Conductor conductor = conductorRepository.findByNumeroLicencia(numeroLicencia)
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

        Usuario usuario = null;
        if (dto.getIdUsuario() != null) {
            if (conductorRepository.existsByUsuario_IdUsuario(dto.getIdUsuario())) {
                throw new BadRequestException("El usuario seleccionado ya está vinculado a otro conductor");
            }
            usuario = usuarioRepository.findById(dto.getIdUsuario())
                    .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getIdUsuario()));
        }

        Conductor conductor = Conductor.builder()
                .usuario(usuario)
                .nombres(dto.getNombres().trim())
                .apellidos(dto.getApellidos().trim())
                .numeroDocumento(dto.getNumeroDocumento() != null ? dto.getNumeroDocumento().trim() : null)
                .tipoLicencia(dto.getTipoLicencia() != null ? dto.getTipoLicencia().trim().toUpperCase() : "A-IIIc")
                .numeroLicencia(licencia)
                .fechaVencimientoLicencia(dto.getFechaVencimientoLicencia())
                .telefono(dto.getTelefono() != null ? dto.getTelefono().trim() : null)
                .estadoOperativo(dto.getEstadoOperativo() != null ? dto.getEstadoOperativo().trim().toUpperCase() : "DISPONIBLE")
                .estadoActivo(dto.getEstadoActivo() != null ? dto.getEstadoActivo() : true)
                .build();

        return mapToResponse(conductorRepository.save(conductor));
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

        if (dto.getIdUsuario() != null) {
            if (conductor.getUsuario() == null || !conductor.getUsuario().getIdUsuario().equals(dto.getIdUsuario())) {
                if (conductorRepository.existsByUsuario_IdUsuario(dto.getIdUsuario())) {
                    throw new BadRequestException("El usuario seleccionado ya está vinculado a otro conductor");
                }
                Usuario usuario = usuarioRepository.findById(dto.getIdUsuario())
                        .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + dto.getIdUsuario()));
                conductor.setUsuario(usuario);
            }
        } else {
            conductor.setUsuario(null);
        }

        if (dto.getNombres() != null) conductor.setNombres(dto.getNombres().trim());
        if (dto.getApellidos() != null) conductor.setApellidos(dto.getApellidos().trim());
        if (dto.getNumeroDocumento() != null) conductor.setNumeroDocumento(dto.getNumeroDocumento().trim());
        if (dto.getTipoLicencia() != null) conductor.setTipoLicencia(dto.getTipoLicencia().trim().toUpperCase());
        if (dto.getFechaVencimientoLicencia() != null) conductor.setFechaVencimientoLicencia(dto.getFechaVencimientoLicencia());
        if (dto.getTelefono() != null) conductor.setTelefono(dto.getTelefono().trim());
        if (dto.getEstadoOperativo() != null) conductor.setEstadoOperativo(dto.getEstadoOperativo().trim().toUpperCase());
        if (dto.getEstadoActivo() != null) conductor.setEstadoActivo(dto.getEstadoActivo());

        return mapToResponse(conductorRepository.save(conductor));
    }

    @Override
    @Transactional
    public void eliminar(Integer idConductor) {
        Conductor conductor = conductorRepository.findById(idConductor)
                .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + idConductor));
        // Borrado lógico estándar
        conductor.setEstadoActivo(false);
        conductorRepository.save(conductor);
    }

    @Override
    @Transactional
    public void reactivar(Integer idConductor) {
        Conductor conductor = conductorRepository.findById(idConductor)
                .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + idConductor));
        conductor.setEstadoActivo(true);
        conductorRepository.save(conductor);
    }

    private ConductorResponseDTO mapToResponse(Conductor c) {
        Integer idUser = c.getUsuario() != null ? c.getUsuario().getIdUsuario() : null;
        String username = c.getUsuario() != null ? c.getUsuario().getUsername() : null;

        return ConductorResponseDTO.builder()
                .idConductor(c.getIdConductor())
                .idUsuario(idUser)
                .username(username)
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
                .build();
    }
}
