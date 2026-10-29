package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.dto.request.ExpedienteActualizarDTO;
import pe.sga.dto.request.ExpedienteCanalDTO;
import pe.sga.dto.request.ExpedienteCrearDTO;
import pe.sga.dto.request.ExpedienteLevanteDTO;
import pe.sga.dto.response.ExpedienteResponseDTO;
import pe.sga.entity.Cotizacion;
import pe.sga.entity.Expediente;
import pe.sga.entity.Usuario;
import pe.sga.entity.enums.CanalSunat;
import pe.sga.entity.enums.EstadoCotizacion;
import pe.sga.entity.enums.EstadoExpediente;
import pe.sga.entity.enums.EstadoPagoCotizacion;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.dto.request.DespachoTransporteCrearDTO;
import pe.sga.dto.request.ExpedienteCerrarDTO;
import pe.sga.dto.request.ExpedienteDiligenciaDTO;
import pe.sga.dto.request.ExpedienteEmbarqueExportacionDTO;
import pe.sga.dto.request.ExpedienteIngresoOrigenDTO;
import pe.sga.dto.request.ExpedienteRecepcionExteriorDTO;
import pe.sga.dto.request.ExpedienteRecojoExportacionDTO;
import pe.sga.dto.request.ExpedienteRetiroDirectoDTO;
import pe.sga.dto.request.ExpedienteSolicitudTransporteDTO;
import pe.sga.entity.DespachoTransporte;
import pe.sga.entity.NotificacionCliente;
import pe.sga.repository.CotizacionPagoRepository;
import pe.sga.repository.CotizacionRepository;
import pe.sga.repository.DespachoTransporteRepository;
import pe.sga.repository.ExpedienteRepository;
import pe.sga.repository.NotificacionClienteRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.IDespachoTransporteService;
import pe.sga.service.IExpedienteService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExpedienteServiceImpl implements IExpedienteService {

    private final ExpedienteRepository expedienteRepository;
    private final CotizacionRepository cotizacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CotizacionPagoRepository cotizacionPagoRepository;
    private final NotificacionClienteRepository notificacionClienteRepository;
    private final DespachoTransporteRepository despachoRepository;
    private final IDespachoTransporteService despachoTransporteService;


    @Override
    @Transactional(readOnly = true)
    public List<ExpedienteResponseDTO> listarTodos(Boolean soloActivos) {
        if (soloActivos != null && soloActivos) {
            return mapListToResponse(expedienteRepository.findByEstadoExpedienteNot(EstadoExpediente.ANULADO));
        }
        return mapListToResponse(expedienteRepository.findAll());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpedienteResponseDTO> listarPorCanal(CanalSunat canal) {
        return mapListToResponse(expedienteRepository.findByCanalSunat(canal));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpedienteResponseDTO> listarPorEstado(EstadoExpediente estado) {
        return mapListToResponse(expedienteRepository.findByEstadoExpediente(estado));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpedienteResponseDTO> listarPorAgente(Integer idAgenteAduanero) {
        return mapListToResponse(expedienteRepository.findByAgenteAduanero_IdUsuario(idAgenteAduanero));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpedienteResponseDTO> listarPorCliente(Integer idCliente) {
        return mapListToResponse(expedienteRepository.findByCliente_IdCliente(idCliente));
    }

    @Override
    @Transactional(readOnly = true)
    public ExpedienteResponseDTO buscarPorId(Integer idExpediente) {
        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));
        return mapToResponse(expediente);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpedienteResponseDTO buscarPorCodigo(String codigoExpediente) {
        Expediente expediente = expedienteRepository.findByCodigoExpediente(codigoExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con código: " + codigoExpediente));
        return mapToResponse(expediente);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpedienteResponseDTO buscarPorCotizacion(Integer idCotizacion) {
        Expediente expediente = expedienteRepository.findByCotizacion_IdCotizacion(idCotizacion)
                .orElseThrow(() -> new ResourceNotFoundException("No existe expediente para la cotización ID: " + idCotizacion));
        return mapToResponse(expediente);
    }

    @Override
    @Transactional
    public ExpedienteResponseDTO crear(ExpedienteCrearDTO dto) {
        Cotizacion cotizacion = cotizacionRepository.findById(dto.getIdCotizacion())
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con ID: " + dto.getIdCotizacion()));

        if (cotizacion.getEstado() != EstadoCotizacion.APROBADA) {
            throw new BadRequestException("Solo se pueden aperturar expedientes para cotizaciones con estado APROBADA. Estado actual: " + cotizacion.getEstado());
        }

        // COMPUERTA FINANCIERA: Debe existir un pago o adelanto validado y aprobado por Finanzas
        boolean tienePagoAprobado = "ADELANTO_APROBADO".equals(cotizacion.getEstadoPago())
                || "PAGADO_TOTAL".equals(cotizacion.getEstadoPago())
                || cotizacionPagoRepository.existsByCotizacion_IdCotizacionAndEstadoPago(cotizacion.getIdCotizacion(), EstadoPagoCotizacion.APROBADO);

        if (!tienePagoAprobado) {
            throw new BadRequestException("No es posible aperturar el expediente aduanero. Se requiere un pago o adelanto validado y aprobado por el área de Finanzas.");
        }

        if (expedienteRepository.existsByCotizacion_IdCotizacion(dto.getIdCotizacion())) {
            throw new BadRequestException("Ya existe un expediente aduanero aperturado para la cotización ID: " + dto.getIdCotizacion());
        }

        Usuario agente = usuarioRepository.findById(dto.getIdAgenteAduanero())
                .orElseThrow(() -> new ResourceNotFoundException("Agente aduanero no encontrado con ID: " + dto.getIdAgenteAduanero()));

        String codigoExpediente = generarCodigoExpediente();

        Expediente expediente = Expediente.builder()
                .codigoExpediente(codigoExpediente)
                .cotizacion(cotizacion)
                .cliente(cotizacion.getCliente())
                .mercancia(cotizacion.getMercancia())
                .tipoCarga(cotizacion.getTipoCarga())
                .agenteAduanero(agente)
                .fechaAsignacion(LocalDateTime.now())
                .contactoExteriorNombre(dto.getContactoExteriorNombre())
                .contactoExteriorTelefono(dto.getContactoExteriorTelefono())
                .contactoExteriorCorreo(dto.getContactoExteriorCorreo())
                .fechaZarpeEtd(dto.getFechaZarpeEtd())
                .fechaLlegadaEta(dto.getFechaLlegadaEta())
                .numeroContenedor(dto.getNumeroContenedor())
                .precintoAduanero(dto.getPrecintoAduanero())
                .naveViaje(dto.getNaveViaje())
                .canalSunat(CanalSunat.PENDIENTE)
                .notificadoAgente(false)
                .estadoExpediente(EstadoExpediente.ASIGNADO)
                .fechaApertura(LocalDateTime.now())
                .observaciones(dto.getObservaciones())
                .build();

        Expediente guardado = expedienteRepository.save(expediente);

        // Notificaciones al cliente B2B (Diagrama BPMN: "Not. expediente creado" & "Not. agente asignado")
        if (guardado.getCliente() != null) {
            // 1. Notificación: Expediente creado
            NotificacionCliente notifExp = NotificacionCliente.builder()
                    .cliente(guardado.getCliente())
                    .titulo("Expediente Aduanero Creado: " + guardado.getCodigoExpediente())
                    .mensaje("Se ha aperturado satisfactoriamente el expediente aduanero " + guardado.getCodigoExpediente()
                            + " derivado de su cotización aprobada " + cotizacion.getCodigoCotizacion()
                            + ". Puede realizar seguimiento continuo de los hitos aduaneros en la sección de Expedientes.")
                    .tipo("DAM")
                    .leido(false)
                    .enlace("/expedientes")
                    .fechaCreacion(LocalDateTime.now())
                    .build();
            notificacionClienteRepository.save(notifExp);

            // 2. Notificación: Agente asignado
            String detalleAgente = agente.getCorreoInstitucional() != null && !agente.getCorreoInstitucional().isBlank()
                    ? " (" + agente.getCorreoInstitucional() + ")"
                    : "";
            NotificacionCliente notifAgente = NotificacionCliente.builder()
                    .cliente(guardado.getCliente())
                    .titulo("Agente Aduanero Asignado: " + agente.getNombreCompleto())
                    .mensaje("El Supervisor Aduanero ha asignado al Agente Aduanero " + agente.getNombreCompleto() + detalleAgente
                            + " a su expediente " + guardado.getCodigoExpediente()
                            + " para liderar la numeración de DAM, aforo e inspección ante SUNAT.")
                    .tipo("DAM")
                    .leido(false)
                    .enlace("/expedientes")
                    .fechaCreacion(LocalDateTime.now())
                    .build();
            notificacionClienteRepository.save(notifAgente);
        }

        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public ExpedienteResponseDTO actualizar(Integer idExpediente, ExpedienteActualizarDTO dto) {
        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));

        if (expediente.getEstadoExpediente() == EstadoExpediente.ANULADO || expediente.getEstadoExpediente() == EstadoExpediente.CERRADO) {
            throw new BadRequestException("No se puede editar un expediente en estado: " + expediente.getEstadoExpediente());
        }

        if (dto.getContactoExteriorNombre() != null) expediente.setContactoExteriorNombre(dto.getContactoExteriorNombre());
        if (dto.getContactoExteriorTelefono() != null) expediente.setContactoExteriorTelefono(dto.getContactoExteriorTelefono());
        if (dto.getContactoExteriorCorreo() != null) expediente.setContactoExteriorCorreo(dto.getContactoExteriorCorreo());
        if (dto.getFechaZarpeEtd() != null) expediente.setFechaZarpeEtd(dto.getFechaZarpeEtd());
        if (dto.getFechaLlegadaEta() != null) expediente.setFechaLlegadaEta(dto.getFechaLlegadaEta());
        if (dto.getFechaArriboAduana() != null) expediente.setFechaArriboAduana(dto.getFechaArriboAduana());
        if (dto.getNumeroContenedor() != null) expediente.setNumeroContenedor(dto.getNumeroContenedor());
        if (dto.getPrecintoAduanero() != null) expediente.setPrecintoAduanero(dto.getPrecintoAduanero());
        if (dto.getNaveViaje() != null) expediente.setNaveViaje(dto.getNaveViaje());
        if (dto.getObservaciones() != null) expediente.setObservaciones(dto.getObservaciones());

        return mapToResponse(expedienteRepository.save(expediente));
    }

    @Override
    @Transactional
    public ExpedienteResponseDTO asignarCanal(Integer idExpediente, ExpedienteCanalDTO dto) {
        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));

        String dam = dto.getNumeroDam().trim();
        if (expediente.getNumeroDam() == null || !expediente.getNumeroDam().equalsIgnoreCase(dam)) {
            if (expedienteRepository.existsByNumeroDam(dam)) {
                throw new BadRequestException("Ya existe un expediente con el número de DAM: " + dam);
            }
            expediente.setNumeroDam(dam);
        }

        expediente.setCanalSunat(dto.getCanalSunat());
        expediente.setFechaAsignacionCanal(dto.getFechaAsignacionCanal() != null ? dto.getFechaAsignacionCanal() : LocalDateTime.now());

        if (dto.getDetalleInspeccion() != null) {
            expediente.setDetalleInspeccion(dto.getDetalleInspeccion());
        }

        // Regla de Negocio: Canales Naranja y Rojo activan notificación automática al agente
        if (dto.getCanalSunat() == CanalSunat.NARANJA || dto.getCanalSunat() == CanalSunat.ROJO) {
            expediente.setNotificadoAgente(true);
            expediente.setFechaNotificacion(LocalDateTime.now());
            expediente.setEstadoExpediente(EstadoExpediente.INSPECCION);
        } else if (dto.getCanalSunat() == CanalSunat.VERDE) {
            expediente.setNotificadoAgente(false);
            expediente.setEstadoExpediente(EstadoExpediente.EN_TRAMITE);
        }

        return mapToResponse(expedienteRepository.save(expediente));
    }

    @Override
    @Transactional
    public ExpedienteResponseDTO autorizarLevante(Integer idExpediente, ExpedienteLevanteDTO dto) {
        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));

        if (expediente.getNumeroDam() == null || expediente.getCanalSunat() == CanalSunat.PENDIENTE) {
            throw new BadRequestException("No se puede autorizar levante sin antes haber asignado DAM y canal aduanero");
        }

        expediente.setFechaLevanteAutorizacion(dto.getFechaLevanteAutorizacion() != null ? dto.getFechaLevanteAutorizacion() : LocalDateTime.now());
        if (dto.getDetalleInspeccion() != null) {
            expediente.setDetalleInspeccion(dto.getDetalleInspeccion());
        }
        if (dto.getObservaciones() != null) {
            expediente.setObservaciones(dto.getObservaciones());
        }
        expediente.setEstadoExpediente(EstadoExpediente.LEVANTE_AUTORIZADO);

        return mapToResponse(expedienteRepository.save(expediente));
    }

    @Override
    @Transactional
    public ExpedienteResponseDTO solicitarRecojoExportacion(Integer idExpediente, ExpedienteRecojoExportacionDTO dto) {
        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));

        if (expediente.getEstadoExpediente() == EstadoExpediente.ANULADO || expediente.getEstadoExpediente() == EstadoExpediente.CERRADO) {
            throw new BadRequestException("No se puede solicitar recojo en un expediente en estado: " + expediente.getEstadoExpediente());
        }

        if (despachoRepository.existsByExpediente_IdExpediente(idExpediente)) {
            throw new BadRequestException("Ya existe una orden de traslado terrestre vinculada a este expediente.");
        }

        DespachoTransporteCrearDTO despachoDTO = DespachoTransporteCrearDTO.builder()
                .idExpediente(idExpediente)
                .tipoTraslado("RECOJO_EXPORTACION")
                .direccionPartida(dto.getDireccionRecojoPlanta())
                .direccionLlegada(dto.getAlmacenDestinoTerminal())
                .distritoLlegada(dto.getDistritoRecojo())
                .contactoRecepcion(dto.getContactoPlanta())
                .fechaProgramada(dto.getFechaProgramadaRecojo() != null ? dto.getFechaProgramadaRecojo() : LocalDateTime.now().plusDays(1))
                .observaciones(dto.getObservaciones())
                .build();

        despachoTransporteService.programar(despachoDTO);

        if (expediente.getCliente() != null) {
            NotificacionCliente notif = NotificacionCliente.builder()
                    .cliente(expediente.getCliente())
                    .titulo("Recojo en Planta Programado: " + expediente.getCodigoExpediente())
                    .mensaje("Se ha programado el recojo de mercadería en planta para la exportación "
                            + expediente.getCodigoExpediente() + ". El área de Flota asignará la unidad para traslado al almacén temporal aduanero.")
                    .tipo("TRANSPORTE")
                    .leido(false)
                    .enlace("/expedientes")
                    .fechaCreacion(LocalDateTime.now())
                    .build();
            notificacionClienteRepository.save(notif);
        }

        return mapToResponse(expediente);
    }

    @Override
    @Transactional
    public ExpedienteResponseDTO confirmarIngresoOrigen(Integer idExpediente, ExpedienteIngresoOrigenDTO dto) {
        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));

        if (expediente.getEstadoExpediente() == EstadoExpediente.ANULADO || expediente.getEstadoExpediente() == EstadoExpediente.CERRADO) {
            throw new BadRequestException("No se puede confirmar ingreso en un expediente en estado: " + expediente.getEstadoExpediente());
        }

        LocalDateTime fechaIngreso = dto.getFechaIngreso() != null ? dto.getFechaIngreso() : LocalDateTime.now();
        expediente.setFechaArriboAduana(fechaIngreso);

        if (dto.getNumeroContenedor() != null && !dto.getNumeroContenedor().isBlank()) {
            expediente.setNumeroContenedor(dto.getNumeroContenedor().trim());
        }
        if (dto.getPrecintoAduanero() != null && !dto.getPrecintoAduanero().isBlank()) {
            expediente.setPrecintoAduanero(dto.getPrecintoAduanero().trim());
        }

        expediente.setEstadoExpediente(EstadoExpediente.EN_TRAMITE);

        String nota = String.format("[INGRESO A ZONA PRIMARIA / ALMACÉN ORIGEN] Terminal: %s | Ticket/VGM: %s | Fecha: %s. %s",
                dto.getTerminalAlmacenOrigen(),
                dto.getNumeroTicketIngreso() != null ? dto.getNumeroTicketIngreso() : "S/N",
                fechaIngreso,
                dto.getObservaciones() != null ? dto.getObservaciones() : "");

        String obsActual = expediente.getObservaciones() != null ? expediente.getObservaciones() + "\n" : "";
        expediente.setObservaciones(obsActual + nota);

        Expediente guardado = expedienteRepository.save(expediente);

        if (guardado.getCliente() != null) {
            NotificacionCliente notif = NotificacionCliente.builder()
                    .cliente(guardado.getCliente())
                    .titulo("Mercadería Ingresada a Zona Primaria: " + guardado.getCodigoExpediente())
                    .mensaje("Su carga ha ingresado satisfactoriamente al depósito temporal de salida (" + dto.getTerminalAlmacenOrigen()
                            + ") para inicio de trámite y asignación de canal de control SUNAT.")
                    .tipo("DAM")
                    .leido(false)
                    .enlace("/expedientes")
                    .fechaCreacion(LocalDateTime.now())
                    .build();
            notificacionClienteRepository.save(notif);
        }

        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public ExpedienteResponseDTO registrarDiligencia(Integer idExpediente, ExpedienteDiligenciaDTO dto) {

        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));

        if (expediente.getEstadoExpediente() == EstadoExpediente.ANULADO || expediente.getEstadoExpediente() == EstadoExpediente.CERRADO) {
            throw new BadRequestException("No se puede registrar diligencia en un expediente en estado: " + expediente.getEstadoExpediente());
        }

        expediente.setDetalleInspeccion(dto.getDetalleInspeccion());
        if (dto.getObservaciones() != null && !dto.getObservaciones().isBlank()) {
            String obsActual = expediente.getObservaciones() != null ? expediente.getObservaciones() + "\n" : "";
            expediente.setObservaciones(obsActual + "[DILIGENCIA AGENTE]: " + dto.getObservaciones());
        }
        return mapToResponse(expedienteRepository.save(expediente));
    }

    @Override
    @Transactional
    public ExpedienteResponseDTO confirmarEmbarqueOrigen(Integer idExpediente, ExpedienteEmbarqueExportacionDTO dto) {
        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));

        if (expediente.getEstadoExpediente() != EstadoExpediente.LEVANTE_AUTORIZADO) {
            throw new BadRequestException("Para confirmar el embarque internacional, el expediente debe contar con LEVANTE AUTORIZADO de exportación.");
        }

        if (dto.getNaveViaje() != null && !dto.getNaveViaje().isBlank()) {
            expediente.setNaveViaje(dto.getNaveViaje().trim());
        }
        if (dto.getFechaZarpeEtd() != null) {
            expediente.setFechaZarpeEtd(dto.getFechaZarpeEtd());
        }
        if (dto.getFechaEstimadaLlegadaExterior() != null) {
            expediente.setFechaLlegadaEta(dto.getFechaEstimadaLlegadaExterior());
        }

        String notaEmbarque = String.format("[EMBARQUE CONFIRMADO] Nave/Viaje: %s | Zarpe ETD: %s | ETA Destino: %s | Puerto Destino: %s. %s",
                dto.getNaveViaje(),
                dto.getFechaZarpeEtd() != null ? dto.getFechaZarpeEtd() : "Confirmado",
                dto.getFechaEstimadaLlegadaExterior() != null ? dto.getFechaEstimadaLlegadaExterior() : "Por confirmar",
                dto.getPuertoDestinoExterior() != null ? dto.getPuertoDestinoExterior() : "Exterior",
                dto.getObservaciones() != null ? dto.getObservaciones() : "");

        String obsActual = expediente.getObservaciones() != null ? expediente.getObservaciones() + "\n" : "";
        expediente.setObservaciones(obsActual + notaEmbarque);

        Expediente guardado = expedienteRepository.save(expediente);

        if (guardado.getCliente() != null) {
            NotificacionCliente notif = NotificacionCliente.builder()
                    .cliente(guardado.getCliente())
                    .titulo("Carga Embarcada Rumbo a Destino: " + guardado.getCodigoExpediente())
                    .mensaje("Su carga de exportación ha sido embarcada a bordo de " + dto.getNaveViaje()
                            + (dto.getPuertoDestinoExterior() != null ? " con destino a " + dto.getPuertoDestinoExterior() : "")
                            + ". Se encuentra en tránsito marítimo/aéreo internacional.")
                    .tipo("DAM")
                    .leido(false)
                    .enlace("/expedientes")
                    .fechaCreacion(LocalDateTime.now())
                    .build();
            notificacionClienteRepository.save(notif);
        }

        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public ExpedienteResponseDTO confirmarRecepcionExterior(Integer idExpediente, ExpedienteRecepcionExteriorDTO dto) {
        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));

        if (expediente.getEstadoExpediente() != EstadoExpediente.LEVANTE_AUTORIZADO) {
            throw new BadRequestException("Solo se puede registrar recepción en destino si el expediente cuenta con Levante y Embarque previo.");
        }

        LocalDateTime fechaRecepcion = dto.getFechaRecepcionDestino() != null ? dto.getFechaRecepcionDestino() : LocalDateTime.now();

        String notaRecepcion = String.format("[RECEPCIÓN DESTINO EXTERIOR] Consignatario: %s | Doc/BL: %s | Fecha: %s. %s",
                dto.getConsignatarioExterior(),
                dto.getDocumentoRecepcion() != null ? dto.getDocumentoRecepcion() : "S/N",
                fechaRecepcion,
                dto.getObservaciones() != null ? dto.getObservaciones() : "");

        String obsActual = expediente.getObservaciones() != null ? expediente.getObservaciones() + "\n" : "";
        expediente.setObservaciones(obsActual + notaRecepcion);

        Expediente guardado = expedienteRepository.save(expediente);

        if (guardado.getCliente() != null) {
            NotificacionCliente notif = NotificacionCliente.builder()
                    .cliente(guardado.getCliente())
                    .titulo("Mercadería Entregada en Destino: " + guardado.getCodigoExpediente())
                    .mensaje("El consignatario internacional " + dto.getConsignatarioExterior()
                            + " ha confirmado la recepción conforme de la carga de exportación " + guardado.getCodigoExpediente()
                            + ". El expediente se encuentra listo para liquidación y cierre.")
                    .tipo("DAM")
                    .leido(false)
                    .enlace("/expedientes")
                    .fechaCreacion(LocalDateTime.now())
                    .build();
            notificacionClienteRepository.save(notif);
        }

        return mapToResponse(guardado);
    }

    @Override
    @Transactional
    public ExpedienteResponseDTO solicitarTransporte(Integer idExpediente, ExpedienteSolicitudTransporteDTO dto) {
        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));

        if (expediente.getEstadoExpediente() != EstadoExpediente.LEVANTE_AUTORIZADO) {
            throw new BadRequestException("El expediente debe contar con LEVANTE AUTORIZADO para solicitar traslado terrestre.");
        }

        if (despachoRepository.existsByExpediente_IdExpediente(idExpediente)) {
            throw new BadRequestException("Ya existe una orden de despacho terrestre vinculada a este expediente.");
        }

        DespachoTransporteCrearDTO despachoDTO = DespachoTransporteCrearDTO.builder()
                .idExpediente(idExpediente)
                .tipoTraslado("ENTREGA_IMPORTACION")
                .direccionPartida(dto.getDireccionPartida())
                .direccionLlegada(dto.getDireccionLlegada())
                .distritoLlegada(dto.getDistritoLlegada())
                .contactoRecepcion(dto.getContactoRecepcion())
                .fechaProgramada(dto.getFechaProgramada() != null ? dto.getFechaProgramada() : LocalDateTime.now().plusDays(1))
                .observaciones(dto.getObservaciones())
                .build();

        despachoTransporteService.programar(despachoDTO);

        if (expediente.getCliente() != null) {
            NotificacionCliente notif = NotificacionCliente.builder()
                    .cliente(expediente.getCliente())
                    .titulo("Envío a Destino Solicitado: " + expediente.getCodigoExpediente())
                    .mensaje("El Agente Aduanero ha generado la solicitud de despacho terrestre a destino para el expediente "
                            + expediente.getCodigoExpediente() + ". El área de Flota y Transporte programará la unidad y conductor asignado.")
                    .tipo("TRANSPORTE")
                    .leido(false)
                    .enlace("/expedientes")
                    .fechaCreacion(LocalDateTime.now())
                    .build();
            notificacionClienteRepository.save(notif);
        }

        return mapToResponse(expediente);
    }

    @Override
    @Transactional
    public ExpedienteResponseDTO registrarRetiroDirecto(Integer idExpediente, ExpedienteRetiroDirectoDTO dto) {
        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));

        if (expediente.getEstadoExpediente() != EstadoExpediente.LEVANTE_AUTORIZADO) {
            throw new BadRequestException("Solo se puede registrar acta de retiro directo si el expediente cuenta con LEVANTE AUTORIZADO.");
        }

        String notaRetiro = String.format("[RETIRO DIRECTO CLIENTE] Terminal: %s | Acta: %s | Receptor: %s (DNI: %s) | Fecha: %s. %s",
                dto.getTerminalAlmacen() != null ? dto.getTerminalAlmacen() : "Terminal Extraportuario",
                dto.getNumeroActaRetiro() != null ? dto.getNumeroActaRetiro() : "S/N",
                dto.getReceptorNombre(),
                dto.getReceptorDni(),
                dto.getFechaRetiro() != null ? dto.getFechaRetiro() : LocalDateTime.now(),
                dto.getObservaciones() != null ? dto.getObservaciones() : "");

        String obsActual = expediente.getObservaciones() != null ? expediente.getObservaciones() + "\n" : "";
        expediente.setObservaciones(obsActual + notaRetiro);

        return mapToResponse(expedienteRepository.save(expediente));
    }

    @Override
    @Transactional
    public ExpedienteResponseDTO cerrar(Integer idExpediente, ExpedienteCerrarDTO dto) {
        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));

        if (expediente.getEstadoExpediente() == EstadoExpediente.CERRADO) {
            throw new BadRequestException("El expediente ya se encuentra en estado CERRADO.");
        }

        if (expediente.getEstadoExpediente() != EstadoExpediente.LEVANTE_AUTORIZADO) {
            throw new BadRequestException("El expediente no puede cerrarse sin haber obtenido LEVANTE AUTORIZADO.");
        }

        // Si la cotización incluía servicio de transporte y se creó despacho, validar que esté entregado
        boolean requiereTransporte = expediente.getCotizacion() != null && Boolean.TRUE.equals(expediente.getCotizacion().getServicioTransporte());
        var despachoOpt = despachoRepository.findByExpediente_IdExpediente(idExpediente);
        if (requiereTransporte && despachoOpt.isPresent()) {
            DespachoTransporte dt = despachoOpt.get();
            if (!"ENTREGADO".equalsIgnoreCase(dt.getEstadoTransporte())) {
                throw new BadRequestException("No es posible cerrar el expediente: El despacho de transporte aún se encuentra en estado '" + dt.getEstadoTransporte() + "'. Debe estar ENTREGADO.");
            }
        }

        expediente.setEstadoExpediente(EstadoExpediente.CERRADO);
        expediente.setFechaCierre(LocalDateTime.now());
        if (dto != null && dto.getObservacionesCierre() != null && !dto.getObservacionesCierre().isBlank()) {
            String obsActual = expediente.getObservaciones() != null ? expediente.getObservaciones() + "\n" : "";
            expediente.setObservaciones(obsActual + "[CIERRE CONFORME SUPERVISOR]: " + dto.getObservacionesCierre());
        }

        Expediente cerrado = expedienteRepository.save(expediente);

        if (cerrado.getCliente() != null) {
            NotificacionCliente notif = NotificacionCliente.builder()
                    .cliente(cerrado.getCliente())
                    .titulo("Expediente Cerrado con Conformidad: " + cerrado.getCodigoExpediente())
                    .mensaje("El Supervisor Aduanero ha validado la liquidación y conformidad de entrega, cerrando formalmente el expediente "
                            + cerrado.getCodigoExpediente() + ". ¡Gracias por confiar en TransAduanas!")
                    .tipo("DAM")
                    .leido(false)
                    .enlace("/expedientes")
                    .fechaCreacion(LocalDateTime.now())
                    .build();
            notificacionClienteRepository.save(notif);
        }

        return mapToResponse(cerrado);
    }

    @Override
    @Transactional
    public void anular(Integer idExpediente) {
        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));
        // Borrado lógico estándar
        expediente.setEstadoExpediente(EstadoExpediente.ANULADO);
        expediente.setFechaCierre(LocalDateTime.now());
        expedienteRepository.save(expediente);
    }

    private String generarCodigoExpediente() {
        int year = LocalDate.now().getYear();
        long count = expedienteRepository.count() + 1;
        String codigo;
        do {
            codigo = String.format("EXP-%d-%04d", year, count);
            count++;
        } while (expedienteRepository.existsByCodigoExpediente(codigo));
        return codigo;
    }

    private List<ExpedienteResponseDTO> mapListToResponse(List<Expediente> expedientes) {
        if (expedientes == null || expedientes.isEmpty()) {
            return List.of();
        }
        List<Integer> idsExp = expedientes.stream()
                .map(Expediente::getIdExpediente)
                .filter(Objects::nonNull)
                .toList();
        Map<Integer, DespachoTransporte> despachoMap = idsExp.isEmpty()
                ? Map.of()
                : despachoRepository.findByExpediente_IdExpedienteIn(idsExp).stream()
                .filter(d -> d.getExpediente() != null)
                .collect(Collectors.toMap(d -> d.getExpediente().getIdExpediente(), Function.identity(), (a, b) -> a));

        return expedientes.stream()
                .map(e -> mapToResponse(e, Optional.ofNullable(despachoMap.get(e.getIdExpediente()))))
                .toList();
    }

    private ExpedienteResponseDTO mapToResponse(Expediente e) {
        var despachoOpt = e.getIdExpediente() != null
                ? despachoRepository.findByExpediente_IdExpediente(e.getIdExpediente())
                : Optional.<DespachoTransporte>empty();
        return mapToResponse(e, despachoOpt);
    }

    private ExpedienteResponseDTO mapToResponse(Expediente e, Optional<DespachoTransporte> despachoOpt) {
        var cot = e.getCotizacion();
        var merc = e.getMercancia() != null ? e.getMercancia() : (cot != null ? cot.getMercancia() : null);
        var tc = e.getTipoCarga() != null ? e.getTipoCarga() : (cot != null ? cot.getTipoCarga() : null);
        var pe = cot != null ? cot.getPuertoEnvio() : null;
        var pd = cot != null ? cot.getPuertoDestino() : null;
        var uc = cot != null ? cot.getUsuarioComercial() : null;

        String mod = pe != null && pe.getTipo() != null ? pe.getTipo()
                : (pd != null && pd.getTipo() != null ? pd.getTipo() : "MARITIMO");

        return ExpedienteResponseDTO.builder()
                .idExpediente(e.getIdExpediente())
                .codigoExpediente(e.getCodigoExpediente())
                .idCotizacion(cot != null ? cot.getIdCotizacion() : null)
                .codigoCotizacion(cot != null ? cot.getCodigoCotizacion() : null)
                .regimenAduanero(cot != null ? cot.getRegimenAduanero() : null)
                .idCliente(e.getCliente() != null ? e.getCliente().getIdCliente() : null)
                .clienteRazonSocial(e.getCliente() != null ? e.getCliente().getRazonSocial() : null)
                .clienteNumeroDocumento(e.getCliente() != null ? e.getCliente().getNumeroDocumento() : null)
                .idAgenteAduanero(e.getAgenteAduanero() != null ? e.getAgenteAduanero().getIdUsuario() : null)
                .agenteAduaneroNombre(e.getAgenteAduanero() != null ? e.getAgenteAduanero().getNombreCompleto() : null)
                .fechaAsignacion(e.getFechaAsignacion())
                .contactoExteriorNombre(e.getContactoExteriorNombre())
                .contactoExteriorTelefono(e.getContactoExteriorTelefono())
                .contactoExteriorCorreo(e.getContactoExteriorCorreo())
                .fechaZarpeEtd(e.getFechaZarpeEtd())
                .fechaLlegadaEta(e.getFechaLlegadaEta())
                .fechaArriboAduana(e.getFechaArriboAduana())
                .numeroDam(e.getNumeroDam())
                .canalSunat(e.getCanalSunat())
                .fechaAsignacionCanal(e.getFechaAsignacionCanal())
                .notificadoAgente(e.getNotificadoAgente())
                .fechaNotificacion(e.getFechaNotificacion())
                .detalleInspeccion(e.getDetalleInspeccion())
                .fechaLevanteAutorizacion(e.getFechaLevanteAutorizacion())
                .numeroContenedor(e.getNumeroContenedor())
                .precintoAduanero(e.getPrecintoAduanero())
                .naveViaje(e.getNaveViaje())
                .estadoExpediente(e.getEstadoExpediente())
                .fechaApertura(e.getFechaApertura())
                .fechaCierre(e.getFechaCierre())
                .observaciones(e.getObservaciones())

                // Mercancía y Carga
                .idMercancia(merc != null ? merc.getIdMercancia() : null)
                .mercanciaCodigo(merc != null ? merc.getCodigo() : null)
                .mercanciaNombre(merc != null ? merc.getNombre() : null)
                .idTipoCarga(tc != null ? tc.getIdTipoCarga() : null)
                .tipoCargaNombre(tc != null ? tc.getNombre() : null)
                .tipoCargaDescripcion(tc != null ? tc.getDescripcion() : null)

                // Modalidad y Puertos
                .modalidad(mod)
                .idPuertoEnvio(pe != null ? pe.getIdPuerto() : null)
                .puertoEnvioNombre(pe != null ? pe.getNombre() : null)
                .puertoEnvioCodigo(pe != null ? pe.getCodigoPuerto() : null)
                .puertoEnvioPais(pe != null ? pe.getPais() : null)
                .puertoEnvioTipo(pe != null ? pe.getTipo() : null)
                .idPuertoDestino(pd != null ? pd.getIdPuerto() : null)
                .puertoDestinoNombre(pd != null ? pd.getNombre() : null)
                .puertoDestinoCodigo(pd != null ? pd.getCodigoPuerto() : null)
                .puertoDestinoPais(pd != null ? pd.getPais() : null)
                .puertoDestinoTipo(pd != null ? pd.getTipo() : null)

                // Logística y Ruta Terrestre
                .origenDepartamento(cot != null ? cot.getOrigenDepartamento() : null)
                .origenProvincia(cot != null ? cot.getOrigenProvincia() : null)
                .destinoDepartamento(cot != null ? cot.getDestinoDepartamento() : null)
                .destinoProvincia(cot != null ? cot.getDestinoProvincia() : null)
                .costoTransporteInterno(cot != null ? cot.getCostoTransporteInterno() : null)

                // Valoración Económica y Tributos
                .valorFob(cot != null ? cot.getValorFob() : null)
                .montoFleteEstimado(cot != null ? cot.getMontoFleteEstimado() : null)
                .montoSeguroEstimado(cot != null ? cot.getMontoSeguroEstimado() : null)
                .montoHonorariosEstimado(cot != null ? cot.getMontoHonorariosEstimado() : null)
                .tasaAdValorem(cot != null ? cot.getTasaAdValorem() : null)
                .tasaPercepcion(cot != null ? cot.getTasaPercepcion() : null)
                .montoTributosEstimado(cot != null ? cot.getMontoTributosEstimado() : null)
                .totalCotizado(cot != null ? cot.getTotalCotizado() : null)

                // Control Financiero Pre-Expediente
                .estadoPago(cot != null ? cot.getEstadoPago() : null)
                .totalAbonado(cot != null ? cot.getTotalAbonado() : null)
                .saldoPendiente(cot != null ? cot.getSaldoPendiente() : null)
                .usuarioComercialNombre(uc != null ? uc.getNombreCompleto() : null)

                // Transporte y Despacho
                .servicioTransporte(cot != null && Boolean.TRUE.equals(cot.getServicioTransporte()))
                .idDespachoTransporte(despachoOpt.map(DespachoTransporte::getIdDespachoTransporte).orElse(null))
                .codigoTrackingTransporte(despachoOpt.map(DespachoTransporte::getCodigoTracking).orElse(null))
                .estadoTransporte(despachoOpt.map(DespachoTransporte::getEstadoTransporte).orElse(null))
                .build();
    }
}

