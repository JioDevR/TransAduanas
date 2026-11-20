package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.dto.request.CatalogoTipoCargaDTO;
import pe.sga.dto.response.CatalogoTipoCargaResponseDTO;
import pe.sga.entity.CatalogoTipoCarga;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.CatalogoTipoCargaRepository;
import pe.sga.service.IAuditoriaService;
import pe.sga.service.ICatalogoTipoCargaService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoTipoCargaServiceImpl implements ICatalogoTipoCargaService {

    private final CatalogoTipoCargaRepository tipoCargaRepository;

    private IAuditoriaService auditoriaService;

    @Autowired(required = false)
    public void setAuditoriaService(IAuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    private void auditar(String accion, Integer idTipoCarga, String detalle) {
        if (auditoriaService != null) {
            auditoriaService.registrar("MAESTROS", accion, "CATALOGO_TIPO_CARGA",
                    idTipoCarga != null ? String.valueOf(idTipoCarga) : null, detalle, "EXITO");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoTipoCargaResponseDTO> listarTodos() {
        return tipoCargaRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoTipoCargaResponseDTO> listarActivos() {
        return tipoCargaRepository.findByEstado("ACTIVO").stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoTipoCargaResponseDTO buscarPorId(Integer idTipoCarga) {
        CatalogoTipoCarga entity = tipoCargaRepository.findById(idTipoCarga)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de carga no encontrado con ID: " + idTipoCarga));
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public CatalogoTipoCargaResponseDTO crear(CatalogoTipoCargaDTO dto) {
        if (tipoCargaRepository.existsByCodigo(dto.getCodigo())) {
            throw new BadRequestException("Ya existe un tipo de carga con el código: " + dto.getCodigo());
        }

        CatalogoTipoCarga entity = CatalogoTipoCarga.builder()
                .codigo(dto.getCodigo().trim().toUpperCase())
                .nombre(dto.getNombre().trim())
                .descripcion(dto.getDescripcion() != null ? dto.getDescripcion().trim() : null)
                .estado(dto.getEstado() != null ? dto.getEstado().trim().toUpperCase() : "ACTIVO")
                .build();

        CatalogoTipoCarga guardado = tipoCargaRepository.save(entity);
        auditar("CREAR_TIPO_CARGA", guardado.getIdTipoCarga(),
                "Tipo de carga creado: " + guardado.getCodigo() + " - " + guardado.getNombre());
        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public CatalogoTipoCargaResponseDTO actualizar(Integer idTipoCarga, CatalogoTipoCargaDTO dto) {
        CatalogoTipoCarga entity = tipoCargaRepository.findById(idTipoCarga)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de carga no encontrado con ID: " + idTipoCarga));

        if (dto.getNombre() != null) entity.setNombre(dto.getNombre().trim());
        if (dto.getDescripcion() != null) entity.setDescripcion(dto.getDescripcion().trim());
        if (dto.getEstado() != null) entity.setEstado(dto.getEstado().trim().toUpperCase());

        CatalogoTipoCarga guardado = tipoCargaRepository.save(entity);
        auditar("ACTUALIZAR_TIPO_CARGA", guardado.getIdTipoCarga(),
                "Tipo de carga actualizado: " + guardado.getCodigo() + " - " + guardado.getNombre());
        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public void eliminarPorEstado(Integer idTipoCarga) {
        CatalogoTipoCarga entity = tipoCargaRepository.findById(idTipoCarga)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de carga no encontrado con ID: " + idTipoCarga));
        entity.setEstado("INACTIVO");
        tipoCargaRepository.save(entity);
        auditar("DESACTIVAR_TIPO_CARGA", entity.getIdTipoCarga(),
                "Tipo de carga desactivado: " + entity.getCodigo() + " - " + entity.getNombre());
    }

    @Override
    @Transactional
    public void reactivar(Integer idTipoCarga) {
        CatalogoTipoCarga entity = tipoCargaRepository.findById(idTipoCarga)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo de carga no encontrado con ID: " + idTipoCarga));
        entity.setEstado("ACTIVO");
        tipoCargaRepository.save(entity);
        auditar("REACTIVAR_TIPO_CARGA", entity.getIdTipoCarga(),
                "Tipo de carga reactivado: " + entity.getCodigo() + " - " + entity.getNombre());
    }

    private CatalogoTipoCargaResponseDTO mapToResponse(CatalogoTipoCarga tc) {
        return CatalogoTipoCargaResponseDTO.builder()
                .idTipoCarga(tc.getIdTipoCarga())
                .codigo(tc.getCodigo())
                .nombre(tc.getNombre())
                .descripcion(tc.getDescripcion())
                .estado(tc.getEstado())
                .build();
    }
}
