package pe.sga.service.impl;

import org.springframework.stereotype.Service;
import pe.sga.dto.request.RolDTO;
import pe.sga.dto.response.RolResponseDTO;
import pe.sga.entity.enums.RolUsuario;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.service.IRolService;

import java.util.Arrays;
import java.util.List;

@Service
public class RolServiceImpl implements IRolService {

    @Override
    public List<RolResponseDTO> listarTodos() {
        return Arrays.stream(RolUsuario.values())
                .map(this::mapFromEnum)
                .toList();
    }

    @Override
    public List<RolResponseDTO> listarActivos() {
        return listarTodos();
    }

    @Override
    public RolResponseDTO buscarPorId(Integer idRol) {
        if (idRol == null || idRol < 1 || idRol > RolUsuario.values().length) {
            throw new ResourceNotFoundException("Rol no encontrado con ID: " + idRol);
        }
        return mapFromEnum(RolUsuario.values()[idRol - 1]);
    }

    @Override
    public RolResponseDTO crear(RolDTO dto) {
        throw new BadRequestException("Los roles son fijos del sistema y están definidos en RolUsuario");
    }

    @Override
    public RolResponseDTO actualizar(Integer idRol, RolDTO dto) {
        throw new BadRequestException("Los roles son fijos del sistema y están definidos en RolUsuario");
    }

    @Override
    public void eliminarPorEstado(Integer idRol) {
        throw new BadRequestException("Los roles del sistema no pueden ser eliminados dinámicamente");
    }

    @Override
    public void reactivar(Integer idRol) {
        throw new BadRequestException("Los roles del sistema están siempre activos");
    }

    private RolResponseDTO mapFromEnum(RolUsuario r) {
        return RolResponseDTO.builder()
                .idRol(r.ordinal() + 1)
                .nombreRol(r)
                .rol(r.name())
                .descripcion(r.getEtiqueta())
                .estadoActivo(true)
                .build();
    }
}
