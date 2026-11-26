package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
import pe.sga.entity.ExpedienteEvento;
import pe.sga.entity.NotificacionCliente;
import pe.sga.entity.UnidadTransporte;
import pe.sga.entity.enums.EstadoExpediente;
import pe.sga.entity.enums.EstadoOperativoConductor;
import pe.sga.entity.enums.EstadoOperativoFlota;
import pe.sga.entity.enums.EstadoTransporte;
import pe.sga.entity.enums.TipoTraslado;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.ConductorRepository;
import pe.sga.repository.DespachoTransporteRepository;
import pe.sga.repository.ExpedienteEventoRepository;
import pe.sga.repository.ExpedienteRepository;
import pe.sga.repository.IDocumentoAdjuntoRepository;
import pe.sga.repository.NotificacionClienteRepository;
import pe.sga.repository.UnidadTransporteRepository;
import pe.sga.service.IAuditoriaService;
import pe.sga.service.IDespachoTransporteService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DespachoTransporteServiceImpl implements IDespachoTransporteService {

    private static final List<EstadoTransporte> ESTADOS_RECURSO_OCUPADO =
            List.of(EstadoTransporte.PROGRAMADO, EstadoTransporte.EN_RUTA);

    private final DespachoTransporteRepository despachoRepository;
    private final ExpedienteRepository expedienteRepository;
    private final ConductorRepository conductorRepository;
    private final UnidadTransporteRepository unidadRepository;
    private final IDocumentoAdjuntoRepository documentoRepository;
    private final NotificacionClienteRepository notificacionClienteRepository;

    private IAuditoriaService auditoriaService;
    private ExpedienteEventoRepository expedienteEventoRepository;

    @Autowired(required = false)
    public void setAuditoriaService(IAuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @Autowired(required = false)
    public void setExpedienteEventoRepository(ExpedienteEventoRepository expedienteEventoRepository) {
        this.expedienteEventoRepository = expedienteEventoRepository;
    }

    private void registrarEventoExpediente(Expediente exp, String codigoEstado, String titulo, String descripcion) {
        if (expedienteEventoRepository == null || exp == null || exp.getIdExpediente() == null) return;
        String reg = exp.getCotizacion() != null ? exp.getCotizacion().getRegimenAduanero() : null;
        expedienteEventoRepository.save(ExpedienteEvento.builder()
                .expediente(exp)
                .codigoEstado(codigoEstado)
                .titulo(titulo)
                .descripcion(descripcion)
                .regimen(reg)
                .usuarioNombre("Área de Transporte")
                .fechaEvento(LocalDateTime.now())
                .build());
    }

    private void auditar(String accion, Integer idDespacho, String detalle) {
        if (auditoriaService != null) {
            auditoriaService.registrar("TRANSPORTE", accion, "DESPACHO_TRANSPORTE",
                    idDespacho != null ? String.valueOf(idDespacho) : null, detalle, "EXITO");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DespachoTransporteResponseDTO> listarConFiltros(
            boolean soloActivos, String estado, Integer idConductor, Integer idUnidad) {
        EstadoTransporte estadoEnum = null;
        if (estado != null && !estado.trim().isEmpty()) {
            try {
                estadoEnum = EstadoTransporte.valueOf(estado.trim().toUpperCase());
            } catch (IllegalArgumentException ex) {
                throw new BadRequestException("Estado de transporte no válido: " + estado);
            }
        }
        return despachoRepository.buscarConFiltros(soloActivos, estadoEnum, idConductor, idUnidad)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DespachoTransporteResponseDTO buscarPorId(Integer idDespachoTransporte) {
        DespachoTransporte dt = despachoRepository.findByIdWithDetails(idDespachoTransporte)
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
        DespachoTransporte dt = despachoRepository
                .findFirstByExpediente_IdExpedienteAndEstadoTransporteNotOrderByIdDespachoTransporteDesc(
                        idExpediente, EstadoTransporte.CANCELADO)
                .orElseThrow(() -> new ResourceNotFoundException("No existe despacho activo para el expediente ID: " + idExpediente));
        return mapToResponse(dt);
    }

    @Override
    @Transactional
    public DespachoTransporteResponseDTO programar(DespachoTransporteCrearDTO dto) {
        Expediente expediente = expedienteRepository.findById(dto.getIdExpediente())
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + dto.getIdExpediente()));

        if (expediente.getEstadoExpediente() == EstadoExpediente.ANULADO
                || expediente.getEstadoExpediente() == EstadoExpediente.CERRADO) {
            throw new BadRequestException("No se puede programar transporte para un expediente en estado: "
                    + expediente.getEstadoExpediente());
        }

        if (despachoRepository.existsByExpediente_IdExpedienteAndEstadoTransporteNot(
                dto.getIdExpediente(), EstadoTransporte.CANCELADO)) {
            throw new BadRequestException("Ya existe un despacho activo programado para el expediente ID: " + dto.getIdExpediente());
        }

        boolean hasConductor = dto.getIdConductor() != null;
        boolean hasUnidad = dto.getIdUnidad() != null;
        if (hasConductor != hasUnidad) {
            throw new BadRequestException(
                    "Para programar con asignación directa debe indicar tanto conductor como unidad, o dejar ambos vacíos.");
        }

        Conductor conductor = null;
        UnidadTransporte unidad = null;
        if (hasConductor) {
            conductor = conductorRepository.findByIdForUpdate(dto.getIdConductor())
                    .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + dto.getIdConductor()));
            unidad = unidadRepository.findByIdForUpdate(dto.getIdUnidad())
                    .orElseThrow(() -> new ResourceNotFoundException("Unidad de transporte no encontrada con ID: " + dto.getIdUnidad()));
            validarRecursosAptosParaAsignacion(conductor, unidad, null);
        }

        String tracking = generarCodigoTracking();
        EstadoTransporte estado = hasConductor
                ? EstadoTransporte.PROGRAMADO
                : EstadoTransporte.PENDIENTE_ASIGNACION;

        DespachoTransporte dt = DespachoTransporte.builder()
                .expediente(expediente)
                .codigoTracking(tracking)
                .tipoTraslado(dto.getTipoTraslado() != null ? dto.getTipoTraslado() : TipoTraslado.ENTREGA_IMPORTACION)
                .conductor(conductor)
                .unidad(unidad)
                .guiaRemisionTransporte(normalizeNullable(dto.getGuiaRemisionTransporte()))
                .direccionPartida(dto.getDireccionPartida().trim())
                .direccionLlegada(dto.getDireccionLlegada().trim())
                .distritoLlegada(dto.getDistritoLlegada().trim())
                .contactoRecepcion(normalizeNullable(dto.getContactoRecepcion()))
                .fechaProgramada(dto.getFechaProgramada() != null ? dto.getFechaProgramada() : LocalDateTime.now().plusDays(1))
                .estadoTransporte(estado)
                .observaciones(normalizeNullable(dto.getObservaciones()))
                .build();

        DespachoTransporte guardado = despachoRepository.save(dt);
        auditar("PROGRAMAR_DESPACHO", guardado.getIdDespachoTransporte(),
                "Despacho programado " + guardado.getCodigoTracking() + " (" + guardado.getTipoTraslado() + ")");
        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public DespachoTransporteResponseDTO aceptarSolicitud(Integer idDespachoTransporte) {
        DespachoTransporte dt = despachoRepository.findByIdWithDetails(idDespachoTransporte)
                .orElseThrow(() -> new ResourceNotFoundException("Despacho no encontrado con ID: " + idDespachoTransporte));

        if (dt.getEstadoTransporte() != EstadoTransporte.PENDIENTE_ASIGNACION) {
            throw new BadRequestException("Solo se pueden aceptar solicitudes en estado PENDIENTE_ASIGNACION (estado actual: "
                    + dt.getEstadoTransporte() + ")");
        }

        dt.setEstadoTransporte(EstadoTransporte.ACEPTADO);
        DespachoTransporte guardado = despachoRepository.save(dt);
        registrarEventoExpediente(dt.getExpediente(), "TRANSPORTE_ACEPTADO",
                "Solicitud de Transporte Aceptada",
                "El área de Transporte aceptó el traslado " + guardado.getCodigoTracking() + " para asignación de unidad y conductor.");
        auditar("ACEPTAR_SOLICITUD_DESPACHO", guardado.getIdDespachoTransporte(),
                "Solicitud de despacho aceptada " + guardado.getCodigoTracking());
        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public DespachoTransporteResponseDTO asignarRecursos(Integer idDespachoTransporte, DespachoAsignarRecursosDTO dto) {
        DespachoTransporte dt = despachoRepository.findByIdWithDetails(idDespachoTransporte)
                .orElseThrow(() -> new ResourceNotFoundException("Despacho no encontrado con ID: " + idDespachoTransporte));

        if (dt.getEstadoTransporte() != EstadoTransporte.PENDIENTE_ASIGNACION
                && dt.getEstadoTransporte() != EstadoTransporte.ACEPTADO
                && dt.getEstadoTransporte() != EstadoTransporte.PROGRAMADO) {
            throw new BadRequestException(
                    "Solo se pueden asignar o reasignar recursos en despachos PENDIENTE_ASIGNACION, ACEPTADO o PROGRAMADO (estado actual: "
                            + dt.getEstadoTransporte() + ")");
        }

        Conductor conductor = conductorRepository.findByIdForUpdate(dto.getIdConductor())
                .orElseThrow(() -> new ResourceNotFoundException("Conductor no encontrado con ID: " + dto.getIdConductor()));

        UnidadTransporte unidad = unidadRepository.findByIdForUpdate(dto.getIdUnidad())
                .orElseThrow(() -> new ResourceNotFoundException("Unidad de transporte no encontrada con ID: " + dto.getIdUnidad()));

        validarRecursosAptosParaAsignacion(conductor, unidad, idDespachoTransporte);

        dt.setConductor(conductor);
        dt.setUnidad(unidad);
        if (dto.getGuiaRemisionTransporte() != null && !dto.getGuiaRemisionTransporte().trim().isEmpty()) {
            dt.setGuiaRemisionTransporte(dto.getGuiaRemisionTransporte().trim());
        }
        if (dto.getFechaProgramada() != null) {
            dt.setFechaProgramada(dto.getFechaProgramada());
        }
        if (dto.getObservaciones() != null) {
            dt.setObservaciones(normalizeNullable(dto.getObservaciones()));
        }
        if (dt.getEstadoTransporte() == EstadoTransporte.PENDIENTE_ASIGNACION
                || dt.getEstadoTransporte() == EstadoTransporte.ACEPTADO) {
            dt.setEstadoTransporte(EstadoTransporte.PROGRAMADO);
        }

        DespachoTransporte guardado = despachoRepository.save(dt);
        auditar("ASIGNAR_RECURSOS_DESPACHO", guardado.getIdDespachoTransporte(),
                "Conductor " + conductor.getNombres() + " " + conductor.getApellidos()
                        + " y unidad " + unidad.getPlacaTracto()
                        + " asignados a despacho " + guardado.getCodigoTracking());
        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public DespachoTransporteResponseDTO iniciarRuta(Integer idDespachoTransporte, DespachoRutaDTO dto) {
        DespachoTransporte dt = despachoRepository.findByIdWithDetails(idDespachoTransporte)
                .orElseThrow(() -> new ResourceNotFoundException("Despacho no encontrado con ID: " + idDespachoTransporte));

        if (dt.getEstadoTransporte() != EstadoTransporte.PROGRAMADO) {
            throw new BadRequestException(
                    "Solo se puede iniciar ruta desde el estado PROGRAMADO (estado actual: " + dt.getEstadoTransporte() + ")");
        }

        if (dt.getConductor() == null || dt.getUnidad() == null) {
            throw new BadRequestException("No se puede iniciar ruta sin conductor y unidad asignados");
        }

        Conductor conductor = conductorRepository.findByIdForUpdate(dt.getConductor().getIdConductor())
                .orElseThrow(() -> new ResourceNotFoundException("Conductor asignado no encontrado"));
        UnidadTransporte unidad = unidadRepository.findByIdForUpdate(dt.getUnidad().getIdUnidad())
                .orElseThrow(() -> new ResourceNotFoundException("Unidad asignada no encontrada"));

        validarRecursosAptosParaAsignacion(conductor, unidad, idDespachoTransporte);

        dt.setFechaSalidaRuta(dto != null && dto.getFechaSalidaRuta() != null ? dto.getFechaSalidaRuta() : LocalDateTime.now());
        if (dto != null && dto.getGuiaRemisionTransporte() != null && !dto.getGuiaRemisionTransporte().trim().isEmpty()) {
            dt.setGuiaRemisionTransporte(dto.getGuiaRemisionTransporte().trim());
        }
        if (dto != null && dto.getObservaciones() != null) {
            dt.setObservaciones(normalizeNullable(dto.getObservaciones()));
        }
        dt.setEstadoTransporte(EstadoTransporte.EN_RUTA);

        unidad.setEstadoOperativo(EstadoOperativoFlota.EN_RUTA);
        unidadRepository.save(unidad);

        conductor.setEstadoOperativo(EstadoOperativoConductor.EN_RUTA);
        conductorRepository.save(conductor);

        if (dt.getExpediente() != null && dt.getExpediente().getCliente() != null) {
            String expCode = dt.getExpediente().getCodigoExpediente() != null ? dt.getExpediente().getCodigoExpediente() : "";
            NotificacionCliente notif = NotificacionCliente.builder()
                    .cliente(dt.getExpediente().getCliente())
                    .titulo("Despacho en Ruta - " + dt.getCodigoTracking())
                    .mensaje("Su despacho " + dt.getCodigoTracking() + " del expediente " + expCode + " ha iniciado ruta hacia el destino programado.")
                    .tipo("TRANSPORTE")
                    .leido(false)
                    .enlace(dt.getExpediente().getIdExpediente() != null ? "/expedientes/" + dt.getExpediente().getIdExpediente() : "/expedientes")
                    .fechaCreacion(LocalDateTime.now())
                    .build();
            notificacionClienteRepository.save(notif);
        }

        DespachoTransporte guardado = despachoRepository.save(dt);
        auditar("INICIAR_RUTA_DESPACHO", guardado.getIdDespachoTransporte(),
                "Despacho " + guardado.getCodigoTracking() + " inició ruta");
        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public DespachoTransporteResponseDTO confirmarEntrega(Integer idDespachoTransporte, DespachoEntregaDTO dto) {
        DespachoTransporte dt = despachoRepository.findByIdWithDetails(idDespachoTransporte)
                .orElseThrow(() -> new ResourceNotFoundException("Despacho no encontrado con ID: " + idDespachoTransporte));

        if (dt.getEstadoTransporte() != EstadoTransporte.EN_RUTA) {
            throw new BadRequestException(
                    "Solo se puede confirmar entrega de un despacho EN_RUTA (estado actual: " + dt.getEstadoTransporte() + ")");
        }

        dt.setFechaEntregaConforme(dto.getFechaEntregaConforme() != null ? dto.getFechaEntregaConforme() : LocalDateTime.now());
        if (dto.getReceptorNombre() != null) {
            dt.setReceptorNombre(normalizeNullable(dto.getReceptorNombre()));
        }
        if (dto.getReceptorDni() != null) {
            dt.setReceptorDni(normalizeNullable(dto.getReceptorDni()));
        }
        if (dto.getContactoRecepcion() != null) {
            dt.setContactoRecepcion(normalizeNullable(dto.getContactoRecepcion()));
        }
        if (dto.getIdDocumentoRemision() != null) {
            DocumentoAdjunto doc = documentoRepository.findById(dto.getIdDocumentoRemision())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Documento de remisión no encontrado con ID: " + dto.getIdDocumentoRemision()));
            dt.setDocumentoRemision(doc);
        }
        if (dto.getDetalleIncidencia() != null && !dto.getDetalleIncidencia().isBlank()) {
            dt.setDetalleIncidencia(dto.getDetalleIncidencia().trim());
        }
        if (dto.getObservaciones() != null && !dto.getObservaciones().isBlank()) {
            dt.setObservaciones(dto.getObservaciones().trim());
        }
        dt.setEstadoTransporte(EstadoTransporte.ENTREGADO);

        if (dt.getUnidad() != null) {
            UnidadTransporte unidad = dt.getUnidad();
            if (dto.getKilometrajeLlegada() != null) {
                if (unidad.getKilometrajeActual() != null && dto.getKilometrajeLlegada() < unidad.getKilometrajeActual()) {
                    throw new BadRequestException("El kilometraje de llegada (" + dto.getKilometrajeLlegada()
                            + " km) no puede ser menor al kilometraje actual de la unidad (" + unidad.getKilometrajeActual() + " km)");
                }
                unidad.setKilometrajeActual(dto.getKilometrajeLlegada());
            }
            unidad.setEstadoOperativo(EstadoOperativoFlota.DISPONIBLE);
            unidadRepository.save(unidad);
        }
        if (dt.getConductor() != null) {
            Conductor conductor = dt.getConductor();
            conductor.setEstadoOperativo(EstadoOperativoConductor.DISPONIBLE);
            conductorRepository.save(conductor);
        }

        if (dt.getExpediente() != null && dt.getExpediente().getCliente() != null) {
            String receptor = dt.getReceptorNombre() != null ? dt.getReceptorNombre() : "Recepción conforme";
            String expCode = dt.getExpediente().getCodigoExpediente() != null ? dt.getExpediente().getCodigoExpediente() : "";
            NotificacionCliente notif = NotificacionCliente.builder()
                    .cliente(dt.getExpediente().getCliente())
                    .titulo("Carga Entregada - " + dt.getCodigoTracking())
                    .mensaje("Su carga del expediente " + expCode + " ha sido entregada conforme en destino a receptor: " + receptor + ".")
                    .tipo("TRANSPORTE")
                    .leido(false)
                    .enlace(dt.getExpediente().getIdExpediente() != null ? "/expedientes/" + dt.getExpediente().getIdExpediente() : "/expedientes")
                    .fechaCreacion(LocalDateTime.now())
                    .build();
            notificacionClienteRepository.save(notif);
        }

        DespachoTransporte guardado = despachoRepository.save(dt);
        if (dt.getTipoTraslado() == TipoTraslado.RECOJO_EXPORTACION) {
            registrarEventoExpediente(dt.getExpediente(), "RECOGIDO_EN_PLANTA",
                    "Carga Recogida en Planta / Entregada a Terminal",
                    "Despacho " + guardado.getCodigoTracking() + " completado. Receptor: "
                            + (guardado.getReceptorNombre() != null ? guardado.getReceptorNombre() : "Almacén/Terminal"));
        } else {
            registrarEventoExpediente(dt.getExpediente(), "ENTREGA_TRANSPORTE_CONFORME",
                    "Entrega Terrestre Confirmada por Flota",
                    "Despacho " + guardado.getCodigoTracking() + " entregado a "
                            + (guardado.getReceptorNombre() != null ? guardado.getReceptorNombre() : "Cliente"));
        }
        auditar("CONFIRMAR_ENTREGA_DESPACHO", guardado.getIdDespachoTransporte(),
                "Entrega confirmada para despacho " + guardado.getCodigoTracking());
        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public DespachoTransporteResponseDTO reportarIncidencia(Integer idDespachoTransporte, String incidencia) {
        if (incidencia == null || incidencia.trim().isEmpty()) {
            throw new BadRequestException("El detalle de la incidencia es obligatorio");
        }

        DespachoTransporte dt = despachoRepository.findByIdWithDetails(idDespachoTransporte)
                .orElseThrow(() -> new ResourceNotFoundException("Despacho no encontrado con ID: " + idDespachoTransporte));

        if (dt.getEstadoTransporte() != EstadoTransporte.PROGRAMADO
                && dt.getEstadoTransporte() != EstadoTransporte.EN_RUTA) {
            throw new BadRequestException(
                    "Solo se pueden reportar incidencias en despachos PROGRAMADO o EN_RUTA (estado actual: "
                            + dt.getEstadoTransporte() + ")");
        }

        String detallePrevio = dt.getDetalleIncidencia() != null && !dt.getDetalleIncidencia().isBlank()
                ? dt.getDetalleIncidencia() + "\n"
                : "";
        dt.setDetalleIncidencia(detallePrevio + "[" + LocalDateTime.now() + "] " + incidencia.trim());

        DespachoTransporte guardado = despachoRepository.save(dt);
        auditar("REPORTAR_INCIDENCIA_DESPACHO", guardado.getIdDespachoTransporte(),
                "Incidencia reportada en despacho " + guardado.getCodigoTracking());
        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public void anular(Integer idDespachoTransporte) {
        DespachoTransporte dt = despachoRepository.findByIdWithDetails(idDespachoTransporte)
                .orElseThrow(() -> new ResourceNotFoundException("Despacho no encontrado con ID: " + idDespachoTransporte));

        if (dt.getEstadoTransporte() == EstadoTransporte.ENTREGADO) {
            throw new BadRequestException("No es posible anular un despacho que ya fue ENTREGADO.");
        }
        if (dt.getEstadoTransporte() == EstadoTransporte.CANCELADO) {
            throw new BadRequestException("El despacho ya se encuentra CANCELADO.");
        }

        dt.setEstadoTransporte(EstadoTransporte.CANCELADO);

        if (dt.getUnidad() != null) {
            UnidadTransporte unidad = dt.getUnidad();
            boolean enOtroDespacho = despachoRepository.existsByUnidad_IdUnidadAndEstadoTransporteInAndIdDespachoTransporteNot(
                    unidad.getIdUnidad(), ESTADOS_RECURSO_OCUPADO, idDespachoTransporte);
            if (!enOtroDespacho && unidad.getEstadoOperativo() == EstadoOperativoFlota.EN_RUTA) {
                unidad.setEstadoOperativo(EstadoOperativoFlota.DISPONIBLE);
                unidadRepository.save(unidad);
            }
        }
        if (dt.getConductor() != null) {
            Conductor conductor = dt.getConductor();
            boolean enOtroDespacho = despachoRepository.existsByConductor_IdConductorAndEstadoTransporteInAndIdDespachoTransporteNot(
                    conductor.getIdConductor(), ESTADOS_RECURSO_OCUPADO, idDespachoTransporte);
            if (!enOtroDespacho && conductor.getEstadoOperativo() == EstadoOperativoConductor.EN_RUTA) {
                conductor.setEstadoOperativo(EstadoOperativoConductor.DISPONIBLE);
                conductorRepository.save(conductor);
            }
        }

        despachoRepository.save(dt);
        auditar("ANULAR_DESPACHO", dt.getIdDespachoTransporte(),
                "Despacho " + dt.getCodigoTracking() + " cancelado/anulado");
    }

    private void validarRecursosAptosParaAsignacion(
            Conductor conductor, UnidadTransporte unidad, Integer idDespachoActual) {
        LocalDate hoy = LocalDate.now();
        Integer idExcluir = idDespachoActual != null ? idDespachoActual : -1;

        if (Boolean.FALSE.equals(unidad.getEstadoActivo())) {
            throw new BadRequestException("La unidad seleccionada se encuentra inactiva");
        }
        if (unidad.getEstadoOperativo() != null && unidad.getEstadoOperativo() != EstadoOperativoFlota.DISPONIBLE) {
            throw new BadRequestException("La unidad no se encuentra DISPONIBLE para asignación (estado actual: "
                    + unidad.getEstadoOperativo() + ")");
        }
        if (unidad.getSoatVencimiento() != null && unidad.getSoatVencimiento().isBefore(hoy)) {
            throw new BadRequestException("No se puede asignar una unidad con SOAT vencido (Vencimiento: "
                    + unidad.getSoatVencimiento() + ")");
        }
        if (unidad.getCitvVencimiento() != null && unidad.getCitvVencimiento().isBefore(hoy)) {
            throw new BadRequestException("No se puede asignar una unidad con Revisión Técnica CITV vencida (Vencimiento: "
                    + unidad.getCitvVencimiento() + ")");
        }
        if (despachoRepository.existsByUnidad_IdUnidadAndEstadoTransporteInAndIdDespachoTransporteNot(
                unidad.getIdUnidad(), ESTADOS_RECURSO_OCUPADO, idExcluir)) {
            throw new BadRequestException("La unidad seleccionada ya se encuentra asignada en otro despacho activo.");
        }

        if (Boolean.FALSE.equals(conductor.getEstadoActivo())) {
            throw new BadRequestException("El conductor seleccionado se encuentra inactivo");
        }
        if (conductor.getEstadoOperativo() != null
                && conductor.getEstadoOperativo() != EstadoOperativoConductor.DISPONIBLE) {
            throw new BadRequestException("El conductor no se encuentra DISPONIBLE para asignación (estado actual: "
                    + conductor.getEstadoOperativo() + ")");
        }
        if (conductor.getFechaVencimientoLicencia() != null && conductor.getFechaVencimientoLicencia().isBefore(hoy)) {
            throw new BadRequestException("No se puede asignar un conductor con licencia de conducir vencida (Vencimiento: "
                    + conductor.getFechaVencimientoLicencia() + ")");
        }
        if (despachoRepository.existsByConductor_IdConductorAndEstadoTransporteInAndIdDespachoTransporteNot(
                conductor.getIdConductor(), ESTADOS_RECURSO_OCUPADO, idExcluir)) {
            throw new BadRequestException("El conductor seleccionado ya se encuentra asignado en otro despacho activo.");
        }
    }

    private String generarCodigoTracking() {
        int year = LocalDate.now().getYear();
        Integer maxId = despachoRepository.findMaxId();
        long secuencia = (maxId != null ? maxId : 0) + 1L;
        String codigo;
        do {
            codigo = String.format("TRK-%d-%04d", year, secuencia);
            secuencia++;
        } while (despachoRepository.existsByCodigoTracking(codigo));
        return codigo;
    }

    private String normalizeNullable(String val) {
        if (val == null) {
            return null;
        }
        String trimmed = val.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private DespachoTransporteResponseDTO mapToResponse(DespachoTransporte dt) {
        Expediente exp = dt.getExpediente();
        String docRemisionNombre = dt.getDocumentoRemision() != null ? dt.getDocumentoRemision().getNombreOriginal() : null;
        String mercanciaNombre = null;
        if (exp != null) {
            if (exp.getMercancia() != null) {
                mercanciaNombre = exp.getMercancia().getNombre();
            } else if (exp.getCotizacion() != null && exp.getCotizacion().getMercancia() != null) {
                mercanciaNombre = exp.getCotizacion().getMercancia().getNombre();
            }
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
                .mercanciaNombre(mercanciaNombre)
                .idConductor(dt.getConductor() != null ? dt.getConductor().getIdConductor() : null)
                .conductorNombre(dt.getConductor() != null ? dt.getConductor().getNombres() + " " + dt.getConductor().getApellidos() : null)
                .conductorLicencia(dt.getConductor() != null ? dt.getConductor().getNumeroLicencia() : null)
                .conductorTelefono(dt.getConductor() != null ? dt.getConductor().getTelefono() : null)
                .idUnidad(dt.getUnidad() != null ? dt.getUnidad().getIdUnidad() : null)
                .unidadPlacaTracto(dt.getUnidad() != null ? dt.getUnidad().getPlacaTracto() : null)
                .unidadPlacaCarreta(dt.getUnidad() != null ? dt.getUnidad().getPlacaCarreta() : null)
                .unidadMarcaModelo(dt.getUnidad() != null ? dt.getUnidad().getMarcaModelo() : null)
                .unidadTipoVehiculo(dt.getUnidad() != null ? dt.getUnidad().getTipoVehiculo() : null)
                .unidadCapacidadKg(dt.getUnidad() != null ? dt.getUnidad().getCapacidadCargaKg() : null)
                .unidadKilometrajeActual(dt.getUnidad() != null ? dt.getUnidad().getKilometrajeActual() : null)
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
