package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.dto.request.ControlAlmacenRequestDTO;
import pe.sga.dto.request.ControlAlmacenSalidaDTO;
import pe.sga.dto.response.ControlAlmacenResponseDTO;
import pe.sga.entity.ControlAlmacen;
import pe.sga.entity.Expediente;
import pe.sga.entity.enums.CanalSunat;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.ControlAlmacenRepository;
import pe.sga.repository.ExpedienteRepository;
import pe.sga.service.IControlAlmacenService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ControlAlmacenServiceImpl implements IControlAlmacenService {

    private final ControlAlmacenRepository controlAlmacenRepository;
    private final ExpedienteRepository expedienteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ControlAlmacenResponseDTO> listarTodos(Boolean soloActivos) {
        if (soloActivos != null && soloActivos) {
            return controlAlmacenRepository.findByEstadoAlmacenNot("ANULADO").stream()
                    .map(this::mapToResponse)
                    .toList();
        }
        return controlAlmacenRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ControlAlmacenResponseDTO> listarPorDeposito(String depositoTemporal) {
        return controlAlmacenRepository.findByDepositoTemporalIgnoreCase(depositoTemporal).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ControlAlmacenResponseDTO buscarPorId(Integer idControlAlmacen) {
        ControlAlmacen ca = controlAlmacenRepository.findById(idControlAlmacen)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de almacén no encontrado con ID: " + idControlAlmacen));
        return mapToResponse(ca);
    }

    @Override
    @Transactional(readOnly = true)
    public ControlAlmacenResponseDTO buscarPorExpediente(Integer idExpediente) {
        ControlAlmacen ca = controlAlmacenRepository.findByExpediente_IdExpediente(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("No existe registro de almacén para el expediente ID: " + idExpediente));
        return mapToResponse(ca);
    }

    @Override
    @Transactional
    public ControlAlmacenResponseDTO registrarIngreso(ControlAlmacenRequestDTO dto) {
        Expediente expediente = expedienteRepository.findById(dto.getIdExpediente())
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + dto.getIdExpediente()));

        if (controlAlmacenRepository.existsByExpediente_IdExpediente(dto.getIdExpediente())) {
            throw new BadRequestException("Ya existe un control de almacén para el expediente ID: " + dto.getIdExpediente());
        }

        LocalDateTime fechaIngreso = LocalDateTime.now();
        int diasLibres = dto.getDiasLibresAlmacen() != null ? dto.getDiasLibresAlmacen() : 15;
        LocalDate fechaLimite = dto.getFechaLimiteRetiro() != null
                ? dto.getFechaLimiteRetiro()
                : fechaIngreso.toLocalDate().plusDays(diasLibres);

        String tipoIngreso = dto.getTipoIngreso();
        if (tipoIngreso == null || tipoIngreso.trim().isEmpty()) {
            tipoIngreso = (expediente.getCanalSunat() == CanalSunat.VERDE) ? "DIRECTO_VERDE" : "POST_INSPECCION";
        }

        ControlAlmacen ca = ControlAlmacen.builder()
                .expediente(expediente)
                .depositoTemporal(dto.getDepositoTemporal().trim())
                .ubicacionBahia(dto.getUbicacionBahia() != null ? dto.getUbicacionBahia().trim() : "Patio General")
                .tipoIngreso(tipoIngreso)
                .finalidadAlmacenaje(dto.getFinalidadAlmacenaje() != null ? dto.getFinalidadAlmacenaje().trim() : "INGRESO_IMPORTACION")
                .fechaIngreso(fechaIngreso)
                .diasLibresAlmacen(diasLibres)
                .fechaLimiteRetiro(fechaLimite)
                .cantidadBultos(dto.getCantidadBultos() != null ? dto.getCantidadBultos() : 0)
                .pesoBrutoKg(dto.getPesoBrutoKg() != null ? dto.getPesoBrutoKg() : BigDecimal.ZERO)
                .ticketPesajeVgm(dto.getTicketPesajeVgm() != null ? dto.getTicketPesajeVgm().trim() : null)
                .precintoConforme(dto.getPrecintoConforme() != null ? dto.getPrecintoConforme() : true)
                .autorizadoMovimiento(false)
                .estadoAlmacen("EN_CUSTODIA")
                .observaciones(dto.getObservaciones())
                .build();

        return mapToResponse(controlAlmacenRepository.save(ca));
    }

    @Override
    @Transactional
    public ControlAlmacenResponseDTO actualizar(Integer idControlAlmacen, ControlAlmacenRequestDTO dto) {
        ControlAlmacen ca = controlAlmacenRepository.findById(idControlAlmacen)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de almacén no encontrado con ID: " + idControlAlmacen));

        if (ca.getEstadoAlmacen().equalsIgnoreCase("ANULADO") || ca.getEstadoAlmacen().equalsIgnoreCase("DESPACHADO")) {
            throw new BadRequestException("No se puede editar un registro de almacén en estado: " + ca.getEstadoAlmacen());
        }

        if (dto.getDepositoTemporal() != null) ca.setDepositoTemporal(dto.getDepositoTemporal().trim());
        if (dto.getUbicacionBahia() != null) ca.setUbicacionBahia(dto.getUbicacionBahia().trim());
        if (dto.getTipoIngreso() != null) ca.setTipoIngreso(dto.getTipoIngreso().trim());
        if (dto.getFinalidadAlmacenaje() != null) ca.setFinalidadAlmacenaje(dto.getFinalidadAlmacenaje().trim());
        if (dto.getDiasLibresAlmacen() != null) {
            ca.setDiasLibresAlmacen(dto.getDiasLibresAlmacen());
            ca.setFechaLimiteRetiro(ca.getFechaIngreso().toLocalDate().plusDays(dto.getDiasLibresAlmacen()));
        }
        if (dto.getFechaLimiteRetiro() != null) ca.setFechaLimiteRetiro(dto.getFechaLimiteRetiro());
        if (dto.getCantidadBultos() != null) ca.setCantidadBultos(dto.getCantidadBultos());
        if (dto.getPesoBrutoKg() != null) ca.setPesoBrutoKg(dto.getPesoBrutoKg());
        if (dto.getTicketPesajeVgm() != null) ca.setTicketPesajeVgm(dto.getTicketPesajeVgm().trim());
        if (dto.getPrecintoConforme() != null) ca.setPrecintoConforme(dto.getPrecintoConforme());
        if (dto.getObservaciones() != null) ca.setObservaciones(dto.getObservaciones());

        return mapToResponse(controlAlmacenRepository.save(ca));
    }

    @Override
    @Transactional
    public ControlAlmacenResponseDTO autorizarSalida(Integer idControlAlmacen, ControlAlmacenSalidaDTO dto) {
        ControlAlmacen ca = controlAlmacenRepository.findById(idControlAlmacen)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de almacén no encontrado con ID: " + idControlAlmacen));

        ca.setAutorizadoMovimiento(true);
        ca.setFechaSalida(dto.getFechaSalida() != null ? dto.getFechaSalida() : LocalDateTime.now());
        ca.setEstadoAlmacen("DESPACHADO");
        if (dto.getObservaciones() != null) {
            ca.setObservaciones(dto.getObservaciones());
        }

        return mapToResponse(controlAlmacenRepository.save(ca));
    }

    @Override
    @Transactional
    public void anular(Integer idControlAlmacen) {
        ControlAlmacen ca = controlAlmacenRepository.findById(idControlAlmacen)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de almacén no encontrado con ID: " + idControlAlmacen));
        ca.setEstadoAlmacen("ANULADO");
        controlAlmacenRepository.save(ca);
    }

    private ControlAlmacenResponseDTO mapToResponse(ControlAlmacen ca) {
        Long diasRestantes = null;
        if (ca.getFechaLimiteRetiro() != null) {
            diasRestantes = ChronoUnit.DAYS.between(LocalDate.now(), ca.getFechaLimiteRetiro());
        }

        return ControlAlmacenResponseDTO.builder()
                .idControlAlmacen(ca.getIdControlAlmacen())
                .idExpediente(ca.getExpediente() != null ? ca.getExpediente().getIdExpediente() : null)
                .codigoExpediente(ca.getExpediente() != null ? ca.getExpediente().getCodigoExpediente() : null)
                .numeroDam(ca.getExpediente() != null ? ca.getExpediente().getNumeroDam() : null)
                .canalSunat(ca.getExpediente() != null && ca.getExpediente().getCanalSunat() != null ? ca.getExpediente().getCanalSunat().name() : null)
                .clienteRazonSocial(ca.getExpediente() != null && ca.getExpediente().getCliente() != null ? ca.getExpediente().getCliente().getRazonSocial() : null)
                .depositoTemporal(ca.getDepositoTemporal())
                .ubicacionBahia(ca.getUbicacionBahia())
                .tipoIngreso(ca.getTipoIngreso())
                .finalidadAlmacenaje(ca.getFinalidadAlmacenaje())
                .fechaIngreso(ca.getFechaIngreso())
                .diasLibresAlmacen(ca.getDiasLibresAlmacen())
                .fechaLimiteRetiro(ca.getFechaLimiteRetiro())
                .diasRestantes(diasRestantes)
                .cantidadBultos(ca.getCantidadBultos())
                .pesoBrutoKg(ca.getPesoBrutoKg())
                .ticketPesajeVgm(ca.getTicketPesajeVgm())
                .precintoConforme(ca.getPrecintoConforme())
                .autorizadoMovimiento(ca.getAutorizadoMovimiento())
                .fechaSalida(ca.getFechaSalida())
                .estadoAlmacen(ca.getEstadoAlmacen())
                .observaciones(ca.getObservaciones())
                .build();
    }
}
