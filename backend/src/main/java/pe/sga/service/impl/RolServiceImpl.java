package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.dto.request.RolDTO;
import pe.sga.dto.response.RolResponseDTO;
import pe.sga.entity.Rol;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.RolRepository;
import pe.sga.service.IRolService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RolServiceImpl implements IRolService {

    private final RolRepository rolRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RolResponseDTO> listarTodos() {
        return rolRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RolResponseDTO> listarActivos() {
        return rolRepository.findByEstadoActivoTrue().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RolResponseDTO buscarPorId(Integer idRol) {
        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + idRol));
        return mapToResponse(rol);
    }

    @Override
    @Transactional
    public RolResponseDTO crear(RolDTO dto) {
        if (rolRepository.existsByNombreRol(dto.getNombreRol())) {
            throw new BadRequestException("El rol " + dto.getNombreRol() + " ya se encuentra registrado");
        }

        Rol rol = Rol.builder()
                .nombreRol(dto.getNombreRol())
                .descripcion(dto.getDescripcion())
                .estadoActivo(dto.getEstadoActivo() != null ? dto.getEstadoActivo() : true)
                .build();

        return mapToResponse(rolRepository.save(rol));
    }

    @Override
    @Transactional
    public RolResponseDTO actualizar(Integer idRol, RolDTO dto) {
        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + idRol));

        if (!rol.getNombreRol().equals(dto.getNombreRol()) && rolRepository.existsByNombreRol(dto.getNombreRol())) {
            throw new BadRequestException("El rol " + dto.getNombreRol() + " ya está en uso");
        }

        rol.setNombreRol(dto.getNombreRol());
        rol.setDescripcion(dto.getDescripcion());
        if (dto.getEstadoActivo() != null) {
            rol.setEstadoActivo(dto.getEstadoActivo());
        }

        return mapToResponse(rolRepository.save(rol));
    }

    @Override
    @Transactional
    public void eliminarPorEstado(Integer idRol) {
        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + idRol));
        rol.setEstadoActivo(false);
        rolRepository.save(rol);
    }

    @Override
    @Transactional
    public void reactivar(Integer idRol) {
        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() -> new ResourceNotFoundException("Rol no encontrado con ID: " + idRol));
        rol.setEstadoActivo(true);
        rolRepository.save(rol);
    }

    private RolResponseDTO mapToResponse(Rol rol) {
        return RolResponseDTO.builder()
                .idRol(rol.getIdRol())
                .nombreRol(rol.getNombreRol())
                .descripcion(rol.getDescripcion())
                .estadoActivo(rol.getEstadoActivo())
                .build();
    }
}
