package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.dto.request.SedeCreateDTO;
import pe.sga.dto.request.SedeUpdateDTO;
import pe.sga.dto.response.SedeResponseDTO;
import pe.sga.entity.Sede;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.SedeRepository;
import pe.sga.service.IAuditoriaService;
import pe.sga.service.ISedeService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SedeServiceImpl implements ISedeService {

    private final SedeRepository sedeRepository;

    private IAuditoriaService auditoriaService;

    @Autowired(required = false)
    public void setAuditoriaService(IAuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    private void auditar(String accion, Integer idSede, String detalle) {
        if (auditoriaService != null) {
            auditoriaService.registrar("MAESTROS", accion, "SEDE",
                    idSede != null ? String.valueOf(idSede) : null, detalle, "EXITO");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SedeResponseDTO> listarTodas() {
        return sedeRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SedeResponseDTO> listarOperativas() {
        return sedeRepository.findByEstado("OPERATIVO").stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SedeResponseDTO buscarPorId(Integer idSede) {
        Sede sede = sedeRepository.findById(idSede)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada con ID: " + idSede));
        return mapToResponse(sede);
    }

    @Override
    @Transactional
    public SedeResponseDTO crear(SedeCreateDTO dto) {
        if (sedeRepository.existsByCodigoSede(dto.getCodigoSede())) {
            throw new BadRequestException("Ya existe una sede con el código: " + dto.getCodigoSede());
        }

        Sede sede = Sede.builder()
                .codigoSede(dto.getCodigoSede())
                .nombre(dto.getNombre())
                .tipo(dto.getTipo() != null ? dto.getTipo() : "SEDE_CENTRAL")
                .direccion(dto.getDireccion())
                .distrito(dto.getDistrito())
                .telefono(dto.getTelefono())
                .estado(dto.getEstado() != null ? dto.getEstado() : "OPERATIVO")
                .build();

        Sede guardada = sedeRepository.save(sede);
        auditar("CREAR_SEDE", guardada.getIdSede(),
                "Sede creada: " + guardada.getCodigoSede() + " - " + guardada.getNombre());
        return mapToResponse(guardada);
    }

    @Override
    @Transactional
    public SedeResponseDTO actualizar(Integer idSede, SedeUpdateDTO dto) {
        Sede sede = sedeRepository.findById(idSede)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada con ID: " + idSede));

        if (dto.getNombre() != null) sede.setNombre(dto.getNombre());
        if (dto.getTipo() != null) sede.setTipo(dto.getTipo());
        if (dto.getDireccion() != null) sede.setDireccion(dto.getDireccion());
        if (dto.getDistrito() != null) sede.setDistrito(dto.getDistrito());
        if (dto.getTelefono() != null) sede.setTelefono(dto.getTelefono());
        if (dto.getEstado() != null) sede.setEstado(dto.getEstado());

        Sede guardada = sedeRepository.save(sede);
        auditar("ACTUALIZAR_SEDE", guardada.getIdSede(),
                "Sede actualizada: " + guardada.getCodigoSede() + " - " + guardada.getNombre());
        return mapToResponse(guardada);
    }

    @Override
    @Transactional
    public void eliminarPorEstado(Integer idSede) {
        Sede sede = sedeRepository.findById(idSede)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada con ID: " + idSede));
        sede.setEstado("INACTIVO");
        sedeRepository.save(sede);
        auditar("DESACTIVAR_SEDE", sede.getIdSede(),
                "Sede desactivada: " + sede.getCodigoSede() + " - " + sede.getNombre());
    }

    @Override
    @Transactional
    public void reactivar(Integer idSede) {
        Sede sede = sedeRepository.findById(idSede)
                .orElseThrow(() -> new ResourceNotFoundException("Sede no encontrada con ID: " + idSede));
        sede.setEstado("OPERATIVO");
        sedeRepository.save(sede);
        auditar("REACTIVAR_SEDE", sede.getIdSede(),
                "Sede reactivada: " + sede.getCodigoSede() + " - " + sede.getNombre());
    }

    private SedeResponseDTO mapToResponse(Sede sede) {
        return SedeResponseDTO.builder()
                .idSede(sede.getIdSede())
                .codigoSede(sede.getCodigoSede())
                .nombre(sede.getNombre())
                .tipo(sede.getTipo())
                .direccion(sede.getDireccion())
                .distrito(sede.getDistrito())
                .telefono(sede.getTelefono())
                .estado(sede.getEstado())
                .build();
    }
}
