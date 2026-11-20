package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.dto.request.UnidadTransporteRequestDTO;
import pe.sga.dto.response.UnidadTransporteResponseDTO;
import pe.sga.entity.UnidadTransporte;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.UnidadTransporteRepository;
import pe.sga.service.IAuditoriaService;
import pe.sga.service.IUnidadTransporteService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UnidadTransporteServiceImpl implements IUnidadTransporteService {

    private final UnidadTransporteRepository unidadRepository;

    private IAuditoriaService auditoriaService;

    @Autowired(required = false)
    public void setAuditoriaService(IAuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    private void auditar(String accion, Integer idUnidad, String detalle) {
        if (auditoriaService != null) {
            auditoriaService.registrar("TRANSPORTE", accion, "UNIDAD_TRANSPORTE",
                    idUnidad != null ? String.valueOf(idUnidad) : null, detalle, "EXITO");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnidadTransporteResponseDTO> listarTodas() {
        return unidadRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnidadTransporteResponseDTO> listarActivas() {
        return unidadRepository.findByEstadoActivo(true).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UnidadTransporteResponseDTO> listarDisponibles() {
        return unidadRepository.findByEstadoOperativoAndEstadoActivo("DISPONIBLE", true).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UnidadTransporteResponseDTO buscarPorId(Integer idUnidad) {
        UnidadTransporte unidad = unidadRepository.findById(idUnidad)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad de transporte no encontrada con ID: " + idUnidad));
        return mapToResponse(unidad);
    }

    @Override
    @Transactional(readOnly = true)
    public UnidadTransporteResponseDTO buscarPorPlaca(String placaTracto) {
        UnidadTransporte unidad = unidadRepository.findByPlacaTracto(placaTracto)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad no encontrada con placa: " + placaTracto));
        return mapToResponse(unidad);
    }

    @Override
    @Transactional
    public UnidadTransporteResponseDTO crear(UnidadTransporteRequestDTO dto) {
        String placa = dto.getPlacaTracto().trim().toUpperCase();
        if (unidadRepository.existsByPlacaTracto(placa)) {
            throw new BadRequestException("Ya existe una unidad registrada con la placa: " + placa);
        }

        UnidadTransporte unidad = UnidadTransporte.builder()
                .placaTracto(placa)
                .placaCarreta(dto.getPlacaCarreta() != null ? dto.getPlacaCarreta().trim().toUpperCase() : null)
                .marcaModelo(dto.getMarcaModelo().trim())
                .anoFabricacion(dto.getAnoFabricacion())
                .tipoVehiculo(dto.getTipoVehiculo() != null ? dto.getTipoVehiculo().trim().toUpperCase() : "TRACTO_PLATAFORMA")
                .capacidadCargaKg(dto.getCapacidadCargaKg())
                .soatVencimiento(dto.getSoatVencimiento())
                .citvVencimiento(dto.getCitvVencimiento())
                .kilometrajeActual(dto.getKilometrajeActual() != null ? dto.getKilometrajeActual() : 0)
                .estadoOperativo(dto.getEstadoOperativo() != null ? dto.getEstadoOperativo().trim().toUpperCase() : "DISPONIBLE")
                .estadoActivo(dto.getEstadoActivo() != null ? dto.getEstadoActivo() : true)
                .observaciones(dto.getObservaciones() != null ? dto.getObservaciones().trim() : null)
                .build();

        UnidadTransporte guardada = unidadRepository.save(unidad);
        auditar("CREAR_UNIDAD", guardada.getIdUnidad(),
                "Unidad de transporte registrada: " + guardada.getPlacaTracto());
        return mapToResponse(guardada);
    }

    @Override
    @Transactional
    public UnidadTransporteResponseDTO actualizar(Integer idUnidad, UnidadTransporteRequestDTO dto) {
        UnidadTransporte unidad = unidadRepository.findById(idUnidad)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad no encontrada con ID: " + idUnidad));

        String placa = dto.getPlacaTracto().trim().toUpperCase();
        if (!unidad.getPlacaTracto().equalsIgnoreCase(placa)) {
            if (unidadRepository.existsByPlacaTracto(placa)) {
                throw new BadRequestException("Ya existe otra unidad registrada con la placa: " + placa);
            }
            unidad.setPlacaTracto(placa);
        }

        if (dto.getPlacaCarreta() != null) unidad.setPlacaCarreta(dto.getPlacaCarreta().trim().toUpperCase());
        if (dto.getMarcaModelo() != null) unidad.setMarcaModelo(dto.getMarcaModelo().trim());
        if (dto.getAnoFabricacion() != null) unidad.setAnoFabricacion(dto.getAnoFabricacion());
        if (dto.getTipoVehiculo() != null) unidad.setTipoVehiculo(dto.getTipoVehiculo().trim().toUpperCase());
        if (dto.getCapacidadCargaKg() != null) unidad.setCapacidadCargaKg(dto.getCapacidadCargaKg());
        if (dto.getSoatVencimiento() != null) unidad.setSoatVencimiento(dto.getSoatVencimiento());
        if (dto.getCitvVencimiento() != null) unidad.setCitvVencimiento(dto.getCitvVencimiento());
        if (dto.getKilometrajeActual() != null) unidad.setKilometrajeActual(dto.getKilometrajeActual());
        if (dto.getEstadoOperativo() != null) unidad.setEstadoOperativo(dto.getEstadoOperativo().trim().toUpperCase());
        if (dto.getEstadoActivo() != null) unidad.setEstadoActivo(dto.getEstadoActivo());
        if (dto.getObservaciones() != null) unidad.setObservaciones(dto.getObservaciones().trim());

        UnidadTransporte guardada = unidadRepository.save(unidad);
        auditar("ACTUALIZAR_UNIDAD", guardada.getIdUnidad(),
                "Unidad de transporte actualizada: " + guardada.getPlacaTracto());
        return mapToResponse(guardada);
    }

    @Override
    @Transactional
    public void eliminar(Integer idUnidad) {
        UnidadTransporte unidad = unidadRepository.findById(idUnidad)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad no encontrada con ID: " + idUnidad));
        // Borrado lógico estándar
        unidad.setEstadoActivo(false);
        unidadRepository.save(unidad);
        auditar("DESACTIVAR_UNIDAD", unidad.getIdUnidad(),
                "Unidad de transporte desactivada: " + unidad.getPlacaTracto());
    }

    @Override
    @Transactional
    public void reactivar(Integer idUnidad) {
        UnidadTransporte unidad = unidadRepository.findById(idUnidad)
                .orElseThrow(() -> new ResourceNotFoundException("Unidad no encontrada con ID: " + idUnidad));
        unidad.setEstadoActivo(true);
        unidadRepository.save(unidad);
        auditar("REACTIVAR_UNIDAD", unidad.getIdUnidad(),
                "Unidad de transporte reactivada: " + unidad.getPlacaTracto());
    }

    private UnidadTransporteResponseDTO mapToResponse(UnidadTransporte u) {
        return UnidadTransporteResponseDTO.builder()
                .idUnidad(u.getIdUnidad())
                .placaTracto(u.getPlacaTracto())
                .placaCarreta(u.getPlacaCarreta())
                .marcaModelo(u.getMarcaModelo())
                .anoFabricacion(u.getAnoFabricacion())
                .tipoVehiculo(u.getTipoVehiculo())
                .capacidadCargaKg(u.getCapacidadCargaKg())
                .soatVencimiento(u.getSoatVencimiento())
                .citvVencimiento(u.getCitvVencimiento())
                .kilometrajeActual(u.getKilometrajeActual())
                .estadoOperativo(u.getEstadoOperativo())
                .estadoActivo(u.getEstadoActivo())
                .observaciones(u.getObservaciones())
                .build();
    }
}
