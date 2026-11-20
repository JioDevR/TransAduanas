package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.dto.request.CatalogoPuertoDTO;
import pe.sga.dto.response.CatalogoPuertoResponseDTO;
import pe.sga.entity.CatalogoPuerto;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.CatalogoPuertoRepository;
import pe.sga.service.IAuditoriaService;
import pe.sga.service.ICatalogoPuertoService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogoPuertoServiceImpl implements ICatalogoPuertoService {

    private final CatalogoPuertoRepository puertoRepository;

    private IAuditoriaService auditoriaService;

    @Autowired(required = false)
    public void setAuditoriaService(IAuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    private void auditar(String accion, Integer idPuerto, String detalle) {
        if (auditoriaService != null) {
            auditoriaService.registrar("MAESTROS", accion, "CATALOGO_PUERTO",
                    idPuerto != null ? String.valueOf(idPuerto) : null, detalle, "EXITO");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoPuertoResponseDTO> listarTodos() {
        return puertoRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<CatalogoPuertoResponseDTO> listarActivos() {
        return puertoRepository.findByEstado("ACTIVO").stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CatalogoPuertoResponseDTO buscarPorId(Integer idPuerto) {
        CatalogoPuerto puerto = puertoRepository.findById(idPuerto)
                .orElseThrow(() -> new ResourceNotFoundException("Puerto no encontrado con ID: " + idPuerto));
        return mapToResponse(puerto);
    }

    @Override
    @Transactional
    public CatalogoPuertoResponseDTO crear(CatalogoPuertoDTO dto) {
        if (puertoRepository.existsByCodigoPuerto(dto.getCodigoPuerto())) {
            throw new BadRequestException("Ya existe un puerto con el código: " + dto.getCodigoPuerto());
        }

        CatalogoPuerto puerto = CatalogoPuerto.builder()
                .codigoPuerto(dto.getCodigoPuerto())
                .nombre(dto.getNombre())
                .pais(dto.getPais())
                .tipo(dto.getTipo() != null ? dto.getTipo() : "MARITIMO")
                .estado(dto.getEstado() != null ? dto.getEstado() : "ACTIVO")
                .build();

        CatalogoPuerto guardado = puertoRepository.save(puerto);
        auditar("CREAR_PUERTO", guardado.getIdPuerto(),
                "Puerto creado: " + guardado.getCodigoPuerto() + " - " + guardado.getNombre());
        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public CatalogoPuertoResponseDTO actualizar(Integer idPuerto, CatalogoPuertoDTO dto) {
        CatalogoPuerto puerto = puertoRepository.findById(idPuerto)
                .orElseThrow(() -> new ResourceNotFoundException("Puerto no encontrado con ID: " + idPuerto));

        if (dto.getNombre() != null) puerto.setNombre(dto.getNombre());
        if (dto.getPais() != null) puerto.setPais(dto.getPais());
        if (dto.getTipo() != null) puerto.setTipo(dto.getTipo());
        if (dto.getEstado() != null) puerto.setEstado(dto.getEstado());

        CatalogoPuerto guardado = puertoRepository.save(puerto);
        auditar("ACTUALIZAR_PUERTO", guardado.getIdPuerto(),
                "Puerto actualizado: " + guardado.getCodigoPuerto() + " - " + guardado.getNombre());
        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public void eliminarPorEstado(Integer idPuerto) {
        CatalogoPuerto puerto = puertoRepository.findById(idPuerto)
                .orElseThrow(() -> new ResourceNotFoundException("Puerto no encontrado con ID: " + idPuerto));
        puerto.setEstado("INACTIVO");
        puertoRepository.save(puerto);
        auditar("DESACTIVAR_PUERTO", puerto.getIdPuerto(),
                "Puerto desactivado: " + puerto.getCodigoPuerto() + " - " + puerto.getNombre());
    }

    @Override
    @Transactional
    public void reactivar(Integer idPuerto) {
        CatalogoPuerto puerto = puertoRepository.findById(idPuerto)
                .orElseThrow(() -> new ResourceNotFoundException("Puerto no encontrado con ID: " + idPuerto));
        puerto.setEstado("ACTIVO");
        puertoRepository.save(puerto);
        auditar("REACTIVAR_PUERTO", puerto.getIdPuerto(),
                "Puerto reactivado: " + puerto.getCodigoPuerto() + " - " + puerto.getNombre());
    }

    private CatalogoPuertoResponseDTO mapToResponse(CatalogoPuerto p) {
        return CatalogoPuertoResponseDTO.builder()
                .idPuerto(p.getIdPuerto())
                .codigoPuerto(p.getCodigoPuerto())
                .nombre(p.getNombre())
                .pais(p.getPais())
                .tipo(p.getTipo())
                .estado(p.getEstado())
                .build();
    }
}
