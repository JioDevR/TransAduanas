package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.dto.request.CatalogoMercanciaRequestDTO;
import pe.sga.dto.response.CatalogoMercanciaResponseDTO;
import pe.sga.entity.CatalogoMercancia;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.CatalogoMercanciaRepository;
import pe.sga.service.IAuditoriaService;
import pe.sga.service.ICatalogoMercanciaService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoMercanciaServiceImpl implements ICatalogoMercanciaService {

    private final CatalogoMercanciaRepository mercanciaRepository;

    private IAuditoriaService auditoriaService;
    private CatalogoResolucionService catalogoResolucionService;

    @Autowired(required = false)
    public void setAuditoriaService(IAuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @Autowired(required = false)
    public void setCatalogoResolucionService(CatalogoResolucionService catalogoResolucionService) {
        this.catalogoResolucionService = catalogoResolucionService;
    }

    private void auditar(String accion, Integer idMercancia, String detalle) {
        if (auditoriaService != null) {
            auditoriaService.registrar("MAESTROS", accion, "CATALOGO_MERCANCIA",
                    idMercancia != null ? String.valueOf(idMercancia) : null, detalle, "EXITO");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoMercanciaResponseDTO> listarTodos() {
        return mercanciaRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoMercanciaResponseDTO> listarActivos() {
        return mercanciaRepository.findByEstado("ACTIVO").stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoMercanciaResponseDTO> listarPorCategoria(String categoria) {
        return mercanciaRepository.findByCategoriaIgnoreCase(categoria).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoMercanciaResponseDTO buscarPorId(Integer idMercancia) {
        CatalogoMercancia entity = mercanciaRepository.findById(idMercancia)
                .orElseThrow(() -> new ResourceNotFoundException("Mercancía no encontrada con ID: " + idMercancia));
        return mapToResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoMercanciaResponseDTO buscarPorCodigo(String codigo) {
        CatalogoMercancia entity = mercanciaRepository.findByCodigo(codigo)
                .orElseThrow(() -> new ResourceNotFoundException("Mercancía no encontrada con código: " + codigo));
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public CatalogoMercanciaResponseDTO crear(CatalogoMercanciaRequestDTO dto) {
        if (mercanciaRepository.existsByCodigo(dto.getCodigo().trim().toUpperCase())) {
            throw new BadRequestException("Ya existe una mercancía con el código: " + dto.getCodigo());
        }

        CatalogoMercancia entity = CatalogoMercancia.builder()
                .codigo(dto.getCodigo().trim().toUpperCase())
                .nombre(dto.getNombre().trim())
                .categoria(dto.getCategoria().trim().toUpperCase())
                .requierePermiso(dto.getRequierePermiso() != null ? dto.getRequierePermiso() : false)
                .entidadReguladora(dto.getEntidadReguladora() != null ? dto.getEntidadReguladora().trim().toUpperCase() : null)
                .arancelBasePct(dto.getArancelBasePct() != null ? dto.getArancelBasePct() : java.math.BigDecimal.ZERO)
                .estado(dto.getEstado() != null ? dto.getEstado().trim().toUpperCase() : "ACTIVO")
                .build();

        if (catalogoResolucionService != null) {
            catalogoResolucionService.enriquecerMercancia(entity);
        }
        CatalogoMercancia guardada = mercanciaRepository.save(entity);
        auditar("CREAR_MERCANCIA", guardada.getIdMercancia(),
                "Mercancía creada: " + guardada.getCodigo() + " - " + guardada.getNombre());
        return mapToResponse(guardada);
    }

    @Override
    @Transactional
    public CatalogoMercanciaResponseDTO actualizar(Integer idMercancia, CatalogoMercanciaRequestDTO dto) {
        CatalogoMercancia entity = mercanciaRepository.findById(idMercancia)
                .orElseThrow(() -> new ResourceNotFoundException("Mercancía no encontrada con ID: " + idMercancia));

        if (dto.getNombre() != null) entity.setNombre(dto.getNombre().trim());
        if (dto.getCategoria() != null) entity.setCategoria(dto.getCategoria().trim().toUpperCase());
        if (dto.getRequierePermiso() != null) entity.setRequierePermiso(dto.getRequierePermiso());
        if (Boolean.FALSE.equals(entity.getRequierePermiso())) {
            entity.setEntidadReguladora(null);
        } else if (dto.getEntidadReguladora() != null) {
            String entidad = dto.getEntidadReguladora().trim().toUpperCase();
            entity.setEntidadReguladora(entidad.isEmpty() ? null : entidad);
        }
        if (dto.getArancelBasePct() != null) entity.setArancelBasePct(dto.getArancelBasePct());
        if (dto.getEstado() != null) entity.setEstado(dto.getEstado().trim().toUpperCase());

        if (catalogoResolucionService != null) {
            catalogoResolucionService.enriquecerMercancia(entity);
        }
        CatalogoMercancia guardada = mercanciaRepository.save(entity);
        auditar("ACTUALIZAR_MERCANCIA", guardada.getIdMercancia(),
                "Mercancía actualizada: " + guardada.getCodigo() + " - " + guardada.getNombre());
        return mapToResponse(guardada);
    }

    @Override
    @Transactional
    public void eliminarPorEstado(Integer idMercancia) {
        CatalogoMercancia entity = mercanciaRepository.findById(idMercancia)
                .orElseThrow(() -> new ResourceNotFoundException("Mercancía no encontrada con ID: " + idMercancia));
        entity.setEstado("INACTIVO");
        mercanciaRepository.save(entity);
        auditar("DESACTIVAR_MERCANCIA", entity.getIdMercancia(),
                "Mercancía desactivada: " + entity.getCodigo() + " - " + entity.getNombre());
    }

    @Override
    @Transactional
    public void reactivar(Integer idMercancia) {
        CatalogoMercancia entity = mercanciaRepository.findById(idMercancia)
                .orElseThrow(() -> new ResourceNotFoundException("Mercancía no encontrada con ID: " + idMercancia));
        entity.setEstado("ACTIVO");
        mercanciaRepository.save(entity);
        auditar("REACTIVAR_MERCANCIA", entity.getIdMercancia(),
                "Mercancía reactivada: " + entity.getCodigo() + " - " + entity.getNombre());
    }

    private CatalogoMercanciaResponseDTO mapToResponse(CatalogoMercancia m) {
        return CatalogoMercanciaResponseDTO.builder()
                .idMercancia(m.getIdMercancia())
                .codigo(m.getCodigo())
                .nombre(m.getNombre())
                .categoria(m.getCategoria())
                .requierePermiso(m.getRequierePermiso())
                .entidadReguladora(m.getEntidadReguladora())
                .arancelBasePct(m.getArancelBasePct())
                .estado(m.getEstado())
                .build();
    }
}
