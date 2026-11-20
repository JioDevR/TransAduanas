package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.dto.request.DespachoAsignarRecursosDTO;
import pe.sga.dto.request.DespachoEntregaDTO;
import pe.sga.dto.request.DespachoRutaDTO;
import pe.sga.dto.request.DespachoTransporteCrearDTO;
import pe.sga.dto.response.DespachoTransporteResponseDTO;
import pe.sga.entity.Conductor;
import pe.sga.entity.DespachoTransporte;
import pe.sga.entity.DocumentoAdjunto;
import pe.sga.entity.Expediente;
import pe.sga.entity.NotificacionCliente;
import pe.sga.entity.UnidadTransporte;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.ConductorRepository;
import pe.sga.repository.DespachoTransporteRepository;
import pe.sga.repository.ExpedienteRepository;
import pe.sga.repository.IDocumentoAdjuntoRepository;
import pe.sga.repository.NotificacionClienteRepository;
import pe.sga.repository.UnidadTransporteRepository;
import pe.sga.service.IDespachoTransporteService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DespachoTransporteServiceImpl implements IDespachoTransporteService {

    private final DespachoTransporteRepository despachoRepository;
    private final ExpedienteRepository expedienteRepository;
    private final ConductorRepository conductorRepository;
    private final UnidadTransporteRepository unidadRepository;
    private final IDocumentoAdjuntoRepository documentoRepository;
    private final NotificacionClienteRepository notificacionClienteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<DespachoTransporteResponseDTO> listarTodos(Boolean soloActivos) {
        if (soloActivos != null && soloActivos) {
            return despachoRepository.findByEstadoTransporteNot("CANCELADO").stream()
                    .map(this::mapToResponse)
                    .toList();
        }
        return despachoRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DespachoTransporteResponseDTO> listarPorEstado(String estadoTransporte) {
        return despachoRepository.findByEstadoTransporte(estadoTransporte).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DespachoTransporteResponseDTO> listarPorConductor(Integer idConductor) {
        return despachoRepository.findByConductor_IdConductor(idConductor).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<DespachoTransporteResponseDTO> listarPorUnidad(Integer idUnidad) {
        return despachoRepository.findByUnidad_IdUnidad(idUnidad).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DespachoTransporteResponseDTO buscarPorId(Integer idDespachoTransporte) {
        DespachoTransporte dt = despachoRepository.findById(idDespachoTransporte)
                .orElseThrow(() -> new ResourceNotFoundException("Despacho no encontrado con ID: " + idDespachoTransporte));
        return mapToResponse(dt);
    }

    @Override
    @Transactional(readOnly = true)
    public DespachoTransporteResponseDTO buscarPorTracking(String codigoTracking) {
        DespachoTransporte dt = despachoRepository.findByCodigoTracking(codigoTracking)
                .orElseThrow(() -> new ResourceNotFoundException("Despacho no encontrado con tracking: " + codigoTracking));
        return mapToResponse(dt);
    }

    @Override
    @Transactional(readOnly = true)
    public DespachoTransporteResponseDTO buscarPorExpediente(Integer idExpediente) {
        DespachoTransporte dt = despachoRepository.findByExpediente_IdExpediente(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("No existe despacho para el expediente ID: " + idExpediente));
        return mapToResponse(dt);
    }

    @Override
    @Transactional(readOnly = true)
    public DespachoTransporteResponseDTO obtenerDespachoActivoPorUsuario(Integer idUsuario) {
        if (idUsuario == null) {
            return null;
        }
        // Buscar despacho activo asignado al usuario chofer
        List<String> estadosActivos = List.of("EN_RUTA", "PROGRAMADO", "PENDIENTE_ASIGNACION");
        var despachoOpt = despachoRepository.findFirstByConductor_Usuario_IdUsuarioAndEstadoTransporteInOrderByFechaProgramadaAsc(
                idUsuario, estadosActivos);

        if (despachoOpt.isPresent()) {
            return mapToResponse(despachoOpt.get());
        }

        // Si el usuario no tiene conductor asociado o no tiene despacho asignado, buscar si tiene conductor
        var conductorOpt = conductorRepository.findByUsuario_IdUsuario(idUsuario);
        if (conductorOpt.isPresent()) {
            var despachoPorConductor = despachoRepository.findFirstByConductor_IdConductorAndEstadoTransporteInOrderByFechaProgramadaAsc(
                    conductorOpt.get().getIdConductor(), estadosActivos);
            return despachoPorConductor.map(this::mapToResponse).orElse(null);
        }

        return null;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DespachoTransporteResponseDTO> listarMisDespachos(Integer idUsuario) {
        if (idUsuario == null) return List.of();
        return despachoRepository.findByConductor_Usuario_IdUsuarioOrderByFechaProgramadaDesc(idUsuario).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional
    public DespachoTransporteResponseDTO programar(DespachoTransporteCrearDTO dto) {
        Expediente expediente = expedienteRepository.findById(dto.getIdExpediente())
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + dto.getIdExpediente()));

        if (despachoRepository.existsByExpediente_IdExpediente(dto.getIdExpediente())) {
            throw new BadRequestException("Ya existe un despacho programado para el expediente ID: " + dto.getIdExpediente());
        }

        Conductor conductor = null;
        if (dto.getIdConductor() != null) {
            conductor = conductorRepository.findById(dto.getIdConductor())
                    .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + dto.getIdConductor()));
        }

        UnidadTransporte unidad = null;
        if (dto.getIdUnidad() != null) {
            unidad = unidadRepository.findById(dto.getIdUnidad())
                    .orElseThrow(() -> new ResourceNotFoundException("Unidad de transporte no encontrada con ID: " + dto.getIdUnidad()));
        }

        String tracking = generarCodigoTracking();
        String estado = (conductor != null && unidad != null) ? "PROGRAMADO" : "PENDIENTE_ASIGNACION";

        DespachoTransporte dt = DespachoTransporte.builder()
                .expediente(expediente)
                .codigoTracking(tracking)
                .tipoTraslado(dto.getTipoTraslado() != null ? dto.getTipoTraslado().trim().toUpperCase() : "ENTREGA_IMPORTACION")
                .conductor(conductor)
                .unidad(unidad)
                .guiaRemisionTransporte(dto.getGuiaRemisionTransporte() != null ? dto.getGuiaRemisionTransporte().trim() : null)
                .direccionPartida(dto.getDireccionPartida().trim())
                .direccionLlegada(dto.getDireccionLlegada().trim())
                .distritoLlegada(dto.getDistritoLlegada().trim())
                .contactoRecepcion(dto.getContactoRecepcion() != null ? dto.getContactoRecepcion().trim() : null)
                .fechaProgramada(dto.getFechaProgramada() != null ? dto.getFechaProgramada() : LocalDateTime.now().plusDays(1))
                .estadoTransporte(estado)
                .observaciones(dto.getObservaciones())
                .build();

        return mapToResponse(despachoRepository.save(dt));
    }

    @Override
    @Transactional
    public DespachoTransporteResponseDTO asignarRecursos(Integer idDespachoTransporte, DespachoAsignarRecursosDTO dto) {
        DespachoTransporte dt = despachoRepository.findById(idDespachoTransporte)
                .orElseThrow(() -> new ResourceNotFoundException("Despacho no encontrado con ID: " + idDespachoTransporte));

        Conductor conductor = conductorRepository.findById(dto.getIdConductor())
                .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + dto.getIdConductor()));

        UnidadTransporte unidad = unidadRepository.findById(dto.getIdUnidad())
                .orElseThrow(() -> new ResourceNotFoundException("Unidad de transporte no encontrada con ID: " + dto.getIdUnidad()));

        // Validaciones técnicas de seguridad vehicular
        if (unidad.getEstadoActivo() != null && !unidad.getEstadoActivo()) {
            throw new BadRequestException("La unidad seleccionada se encuentra inactiva");
        }
        if (unidad.getEstadoOperativo() != null && !"DISPONIBLE".equalsIgnoreCase(unidad.getEstadoOperativo())) {
            throw new BadRequestException("La unidad no se encuentra DISPONIBLE para asignación (estado actual: " + unidad.getEstadoOperativo() + ")");
        }
        if (unidad.getSoatVencimiento() != null && unidad.getSoatVencimiento().isBefore(LocalDate.now())) {
            throw new BadRequestException("No se puede asignar una unidad con SOAT vencido (Vencimiento: " + unidad.getSoatVencimiento() + ")");
        }
        if (unidad.getCitvVencimiento() != null && unidad.getCitvVencimiento().isBefore(LocalDate.now())) {
            throw new BadRequestException("No se puede asignar una unidad con Revisión Técnica CITV vencida (Vencimiento: " + unidad.getCitvVencimiento() + ")");
        }
        if (despachoRepository.existsByUnidad_IdUnidadAndEstadoTransporteInAndIdDespachoTransporteNot(
                unidad.getIdUnidad(), List.of("PROGRAMADO", "EN_RUTA"), idDespachoTransporte)) {
            throw new BadRequestException("La unidad seleccionada ya se encuentra asignada en otro despacho activo.");
        }

        // Validaciones técnicas de conductor
        if (conductor.getEstadoActivo() != null && !conductor.getEstadoActivo()) {
            throw new BadRequestException("El conductor seleccionado se encuentra inactivo");
        }
        if (conductor.getEstadoOperativo() != null && !"DISPONIBLE".equalsIgnoreCase(conductor.getEstadoOperativo())) {
            throw new BadRequestException("El conductor no se encuentra DISPONIBLE para asignación (estado actual: " + conductor.getEstadoOperativo() + ")");
        }
        if (conductor.getFechaVencimientoLicencia() != null && conductor.getFechaVencimientoLicencia().isBefore(LocalDate.now())) {
            throw new BadRequestException("No se puede asignar un conductor con licencia de conducir vencida (Vencimiento: " + conductor.getFechaVencimientoLicencia() + ")");
        }
        if (despachoRepository.existsByConductor_IdConductorAndEstadoTransporteInAndIdDespachoTransporteNot(
                conductor.getIdConductor(), List.of("PROGRAMADO", "EN_RUTA"), idDespachoTransporte)) {
            throw new BadRequestException("El conductor seleccionado ya se encuentra asignado en otro despacho activo.");
        }

        dt.setConductor(conductor);
        dt.setUnidad(unidad);
        if (dto.getGuiaRemisionTransporte() != null && !dto.getGuiaRemisionTransporte().trim().isEmpty()) {
            dt.setGuiaRemisionTransporte(dto.getGuiaRemisionTransporte().trim());
        }
        if (dto.getFechaProgramada() != null) {
            dt.setFechaProgramada(dto.getFechaProgramada());
        }
        if ("PENDIENTE_ASIGNACION".equalsIgnoreCase(dt.getEstadoTransporte())) {
            dt.setEstadoTransporte("PROGRAMADO");
        }

        return mapToResponse(despachoRepository.save(dt));
    }

    @Override
    @Transactional
    public DespachoTransporteResponseDTO iniciarRuta(Integer idDespachoTransporte, DespachoRutaDTO dto) {
        DespachoTransporte dt = despachoRepository.findById(idDespachoTransporte)
                .orElseThrow(() -> new ResourceNotFoundException("Despacho no encontrado con ID: " + idDespachoTransporte));

        if (dt.getConductor() == null || dt.getUnidad() == null) {
            throw new BadRequestException("No se puede iniciar ruta sin conductor y unidad asignados");
        }

        dt.setFechaSalidaRuta(dto.getFechaSalidaRuta() != null ? dto.getFechaSalidaRuta() : LocalDateTime.now());
        if (dto.getGuiaRemisionTransporte() != null && !dto.getGuiaRemisionTransporte().trim().isEmpty()) {
            dt.setGuiaRemisionTransporte(dto.getGuiaRemisionTransporte().trim());
        }
        if (dto.getObservaciones() != null) {
            dt.setObservaciones(dto.getObservaciones());
        }
        dt.setEstadoTransporte("EN_RUTA");

        // Actualizar vehículo y chofer a EN_RUTA
        UnidadTransporte unidad = dt.getUnidad();
        unidad.setEstadoOperativo("EN_RUTA");
        unidadRepository.save(unidad);

        Conductor conductor = dt.getConductor();
        conductor.setEstadoOperativo("EN_RUTA");
        conductorRepository.save(conductor);

        // Notificación B2B al cliente
        if (dt.getExpediente() != null && dt.getExpediente().getCliente() != null) {
            String expCode = dt.getExpediente().getCodigoExpediente() != null ? dt.getExpediente().getCodigoExpediente() : "";
            NotificacionCliente notif = NotificacionCliente.builder()
                    .cliente(dt.getExpediente().getCliente())
                    .titulo("Despacho en Ruta - " + dt.getCodigoTracking())
                    .mensaje("Su despacho " + dt.getCodigoTracking() + " del expediente " + expCode + " ha iniciado ruta hacia el destino programado.")
                    .tipo("TRANSPORTE")
                    .leido(false)
                    .enlace("/portal-b2b?tab=seguimiento&tracking=" + dt.getCodigoTracking())
                    .build();
            notificacionClienteRepository.save(notif);
        }

        return mapToResponse(despachoRepository.save(dt));
    }

    @Override
    @Transactional
    public DespachoTransporteResponseDTO confirmarEntrega(Integer idDespachoTransporte, DespachoEntregaDTO dto) {
        DespachoTransporte dt = despachoRepository.findById(idDespachoTransporte)
                .orElseThrow(() -> new ResourceNotFoundException("Despacho no encontrado con ID: " + idDespachoTransporte));

        dt.setFechaEntregaConforme(dto.getFechaEntregaConforme() != null ? dto.getFechaEntregaConforme() : LocalDateTime.now());
        if (dto.getReceptorNombre() != null) {
            dt.setReceptorNombre(dto.getReceptorNombre().trim());
        }
        if (dto.getReceptorDni() != null) {
            dt.setReceptorDni(dto.getReceptorDni().trim());
        }
        if (dto.getContactoRecepcion() != null) {
            dt.setContactoRecepcion(dto.getContactoRecepcion().trim());
        }
        if (dto.getIdDocumentoRemision() != null) {
            DocumentoAdjunto doc = documentoRepository.findById(dto.getIdDocumentoRemision()).orElse(null);
            dt.setDocumentoRemision(doc);
        }
        if (dto.getDetalleIncidencia() != null) {
            dt.setDetalleIncidencia(dto.getDetalleIncidencia());
        }
        if (dto.getObservaciones() != null) {
            dt.setObservaciones(dto.getObservaciones());
        }
        dt.setEstadoTransporte("ENTREGADO");

        // Actualizar odómetro y liberar vehículo a DISPONIBLE
        if (dt.getUnidad() != null) {
            UnidadTransporte unidad = dt.getUnidad();
            if (dto.getKilometrajeLlegada() != null && (unidad.getKilometrajeActual() == null || dto.getKilometrajeLlegada() > unidad.getKilometrajeActual())) {
                unidad.setKilometrajeActual(dto.getKilometrajeLlegada());
            }
            unidad.setEstadoOperativo("DISPONIBLE");
            unidadRepository.save(unidad);
        }
        if (dt.getConductor() != null) {
            Conductor conductor = dt.getConductor();
            conductor.setEstadoOperativo("DISPONIBLE");
            conductorRepository.save(conductor);
        }

        // Notificación B2B al cliente
        if (dt.getExpediente() != null && dt.getExpediente().getCliente() != null) {
            String receptor = dt.getReceptorNombre() != null ? dt.getReceptorNombre() : "Recepción conforme";
            String expCode = dt.getExpediente().getCodigoExpediente() != null ? dt.getExpediente().getCodigoExpediente() : "";
            NotificacionCliente notif = NotificacionCliente.builder()
                    .cliente(dt.getExpediente().getCliente())
                    .titulo("Carga Entregada - " + dt.getCodigoTracking())
                    .mensaje("Su carga del expediente " + expCode + " ha sido entregada conforme en destino a receptor: " + receptor + ".")
                    .tipo("TRANSPORTE")
                    .leido(false)
                    .enlace("/portal-b2b?tab=seguimiento&tracking=" + dt.getCodigoTracking())
                    .build();
            notificacionClienteRepository.save(notif);
        }

        return mapToResponse(despachoRepository.save(dt));
    }

    @Override
    @Transactional
    public DespachoTransporteResponseDTO reportarIncidencia(Integer idDespachoTransporte, String incidencia) {
        DespachoTransporte dt = despachoRepository.findById(idDespachoTransporte)
                .orElseThrow(() -> new ResourceNotFoundException("Despacho no encontrado con ID: " + idDespachoTransporte));

        String detallePrevio = dt.getDetalleIncidencia() != null ? dt.getDetalleIncidencia() + "\n" : "";
        dt.setDetalleIncidencia(detallePrevio + "[" + LocalDateTime.now() + "] " + incidencia);

        return mapToResponse(despachoRepository.save(dt));
    }

    @Override
    @Transactional
    public void anular(Integer idDespachoTransporte) {
        DespachoTransporte dt = despachoRepository.findById(idDespachoTransporte)
                .orElseThrow(() -> new ResourceNotFoundException("Despacho no encontrado con ID: " + idDespachoTransporte));

        dt.setEstadoTransporte("CANCELADO");

        if (dt.getUnidad() != null) {
            UnidadTransporte unidad = dt.getUnidad();
            unidad.setEstadoOperativo("DISPONIBLE");
            unidadRepository.save(unidad);
        }
        if (dt.getConductor() != null) {
            Conductor conductor = dt.getConductor();
            conductor.setEstadoOperativo("DISPONIBLE");
            conductorRepository.save(conductor);
        }

        despachoRepository.save(dt);
    }

    private String generarCodigoTracking() {
        int year = LocalDate.now().getYear();
        long count = despachoRepository.count() + 1;
        String codigo;
        do {
            codigo = String.format("TRK-%d-%04d", year, count);
            count++;
        } while (despachoRepository.existsByCodigoTracking(codigo));
        return codigo;
    }

    private DespachoTransporteResponseDTO mapToResponse(DespachoTransporte dt) {
        Expediente exp = dt.getExpediente();
        String docRemisionNombre = dt.getDocumentoRemision() != null ? dt.getDocumentoRemision().getNombreOriginal() : null;

        Integer idUserConductor = null;
        if (dt.getConductor() != null && dt.getConductor().getUsuario() != null) {
            idUserConductor = dt.getConductor().getUsuario().getIdUsuario();
        }

        return DespachoTransporteResponseDTO.builder()
                .idDespachoTransporte(dt.getIdDespachoTransporte())
                .codigoTracking(dt.getCodigoTracking())
                .tipoTraslado(dt.getTipoTraslado())
                .idExpediente(exp != null ? exp.getIdExpediente() : null)
                .codigoExpediente(exp != null ? exp.getCodigoExpediente() : null)
                .numeroDam(exp != null ? exp.getNumeroDam() : null)
                .canalSunat(exp != null && exp.getCanalSunat() != null ? exp.getCanalSunat().name() : null)
                .clienteRazonSocial(exp != null && exp.getCliente() != null ? exp.getCliente().getRazonSocial() : null)
                .clienteNumeroDocumento(exp != null && exp.getCliente() != null ? exp.getCliente().getNumeroDocumento() : null)
                .clienteTelefono(exp != null && exp.getCliente() != null ? exp.getCliente().getTelefonoContacto() : null)
                .numeroContenedor(exp != null ? exp.getNumeroContenedor() : null)
                .precintoAduanero(exp != null ? exp.getPrecintoAduanero() : null)
                .mercanciaNombre(exp != null && exp.getCotizacion() != null && exp.getCotizacion().getMercancia() != null ? exp.getCotizacion().getMercancia().getNombre() : null)
                .idConductor(dt.getConductor() != null ? dt.getConductor().getIdConductor() : null)
                .idUsuarioConductor(idUserConductor)
                .conductorNombre(dt.getConductor() != null ? dt.getConductor().getNombres() + " " + dt.getConductor().getApellidos() : null)
                .conductorLicencia(dt.getConductor() != null ? dt.getConductor().getNumeroLicencia() : null)
                .conductorTelefono(dt.getConductor() != null ? dt.getConductor().getTelefono() : null)
                .idUnidad(dt.getUnidad() != null ? dt.getUnidad().getIdUnidad() : null)
                .unidadPlacaTracto(dt.getUnidad() != null ? dt.getUnidad().getPlacaTracto() : null)
                .unidadPlacaCarreta(dt.getUnidad() != null ? dt.getUnidad().getPlacaCarreta() : null)
                .unidadMarcaModelo(dt.getUnidad() != null ? dt.getUnidad().getMarcaModelo() : null)
                .unidadTipoVehiculo(dt.getUnidad() != null ? dt.getUnidad().getTipoVehiculo() : null)
                .unidadCapacidadKg(dt.getUnidad() != null ? dt.getUnidad().getCapacidadCargaKg() : null)
                .guiaRemisionTransporte(dt.getGuiaRemisionTransporte())
                .idDocumentoRemision(dt.getDocumentoRemision() != null ? dt.getDocumentoRemision().getIdDocumento() : null)
                .nombreDocumentoRemision(docRemisionNombre)
                .direccionPartida(dt.getDireccionPartida())
                .direccionLlegada(dt.getDireccionLlegada())
                .distritoLlegada(dt.getDistritoLlegada())
                .contactoRecepcion(dt.getContactoRecepcion())
                .receptorNombre(dt.getReceptorNombre())
                .receptorDni(dt.getReceptorDni())
                .fechaProgramada(dt.getFechaProgramada())
                .fechaSalidaRuta(dt.getFechaSalidaRuta())
                .fechaEntregaConforme(dt.getFechaEntregaConforme())
                .estadoTransporte(dt.getEstadoTransporte())
                .detalleIncidencia(dt.getDetalleIncidencia())
                .observaciones(dt.getObservaciones())
                .build();
    }
}
