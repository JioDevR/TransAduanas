package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.config.JwtTokenProvider;
import pe.sga.dto.portal.*;
import pe.sga.dto.request.LoginRequestDTO;
import pe.sga.dto.response.ExpedienteEventoDTO;
import pe.sga.dto.response.ExpedienteResponseDTO;
import pe.sga.entity.*;
import pe.sga.entity.enums.EstadoCotizacion;
import pe.sga.entity.enums.EstadoExpediente;
import pe.sga.entity.enums.EstadoPagoCotizacion;
import pe.sga.entity.enums.EstadoTransporte;
import pe.sga.entity.enums.RolUsuario;
import pe.sga.entity.enums.TipoAbonoCotizacion;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.*;
import pe.sga.service.IAuditoriaService;
import pe.sga.service.IPortalService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortalServiceImpl implements IPortalService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioClienteRepository usuarioClienteRepository;
    private final ClienteRepository clienteRepository;
    private final CotizacionRepository cotizacionRepository;
    private final ExpedienteRepository expedienteRepository;
    private final CotizacionPagoRepository cotizacionPagoRepository;
    private final LiquidacionFinancieraRepository liquidacionFinancieraRepository;
    private final ControlAlmacenRepository controlAlmacenRepository;
    private final DespachoTransporteRepository despachoTransporteRepository;
    private final CatalogoMercanciaRepository mercanciaRepository;
    private final CatalogoTipoCargaRepository tipoCargaRepository;
    private final CatalogoPuertoRepository puertoRepository;
    private final IDocumentoAdjuntoRepository documentoRepository;
    private final NotificacionClienteRepository notificacionClienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;

    private IAuditoriaService auditoriaService;
    private ComprobanteFacturaRepository comprobanteFacturaRepository;
    private ExpedienteEventoRepository expedienteEventoRepository;

    @Autowired(required = false)
    public void setExpedienteEventoRepository(ExpedienteEventoRepository expedienteEventoRepository) {
        this.expedienteEventoRepository = expedienteEventoRepository;
    }

    @Autowired(required = false)
    public void setAuditoriaService(IAuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @Autowired(required = false)
    public void setComprobanteFacturaRepository(ComprobanteFacturaRepository comprobanteFacturaRepository) {
        this.comprobanteFacturaRepository = comprobanteFacturaRepository;
    }

    private void auditar(String accion, String entidad, Integer idEntidad, String detalle) {
        if (auditoriaService != null) {
            auditoriaService.registrar("PORTAL_B2B", accion, entidad,
                    idEntidad != null ? String.valueOf(idEntidad) : null, detalle, "EXITO");
        }
    }

    private void auditarActor(String actor, String accion, Integer idUsuarioCliente, String detalle, String resultado) {
        if (auditoriaService != null) {
            auditoriaService.registrarConActor(actor, "CLIENTE_PORTAL", "PORTAL_B2B", accion,
                    "USUARIO_CLIENTE", idUsuarioCliente != null ? String.valueOf(idUsuarioCliente) : null,
                    detalle, resultado);
        }
    }

    @Override
    @Transactional
    public PortalAuthResponseDTO autenticarCliente(LoginRequestDTO loginRequest) {
        String identifier = loginRequest.getUsername() != null ? loginRequest.getUsername().trim() : "";
        UsuarioCliente uc = usuarioClienteRepository.findByUsername(identifier)
                .or(() -> usuarioClienteRepository.findByCorreoElectronico(identifier))
                .or(() -> usuarioClienteRepository.findByCliente_NumeroDocumento(identifier).stream().findFirst())
                .orElseThrow(() -> {
                    auditarActor(identifier.isBlank() ? "ANONIMO" : identifier, "LOGIN_PORTAL_FALLIDO",
                            null, "Usuario o identificador de portal B2B no encontrado", "FALLO");
                    return new BadCredentialsException("Credenciales de cliente incorrectas");
                });

        if (!Boolean.TRUE.equals(uc.getEstadoActivo())) {
            auditarActor(uc.getUsername(), "LOGIN_PORTAL_FALLIDO", uc.getIdUsuarioCliente(),
                    "Cuenta B2B inactiva intentó iniciar sesión", "FALLO");
            throw new BadCredentialsException("La cuenta de cliente se encuentra inactiva");
        }

        if (uc.getCliente() == null || !Boolean.TRUE.equals(uc.getCliente().getEstadoActivo())) {
            auditarActor(uc.getUsername(), "LOGIN_PORTAL_FALLIDO", uc.getIdUsuarioCliente(),
                    "Cuenta B2B con empresa cliente inactiva intentó iniciar sesión", "FALLO");
            throw new BadCredentialsException("La empresa cliente se encuentra inactiva en el sistema");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), uc.getPasswordHash())) {
            auditarActor(uc.getUsername(), "LOGIN_PORTAL_FALLIDO", uc.getIdUsuarioCliente(),
                    "Contraseña incorrecta en Portal B2B", "FALLO");
            throw new BadCredentialsException("Credenciales de cliente incorrectas");
        }

        uc.setUltimoAcceso(LocalDateTime.now());
        usuarioClienteRepository.save(uc);

        auditarActor(uc.getUsername(), "LOGIN_PORTAL_EXITOSO", uc.getIdUsuarioCliente(),
                "Inicio de sesión exitoso en Portal B2B", "EXITO");
        String token = tokenProvider.generateTokenForPortal(uc);

        return PortalAuthResponseDTO.builder()
                .token(token)
                .tokenType("Bearer")
                .idUsuarioCliente(uc.getIdUsuarioCliente())
                .username(uc.getUsername())
                .contactoRepresentante(uc.getContactoRepresentante())
                .nombres(uc.getNombres())
                .apellidos(uc.getApellidos())
                .correoElectronico(uc.getCorreoElectronico())
                .idCliente(uc.getCliente().getIdCliente())
                .ruc(uc.getCliente().getNumeroDocumento())
                .razonSocial(uc.getCliente().getRazonSocial())
                .debeCambiarPassword(Boolean.TRUE.equals(uc.getDebeCambiarPassword()))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PortalPerfilDTO obtenerPerfil(UsuarioCliente uc) {
        Cliente c = uc.getCliente();
        return PortalPerfilDTO.builder()
                .idUsuario(uc.getIdUsuarioCliente())
                .username(uc.getUsername())
                .nombres(uc.getNombres())
                .apellidos(uc.getApellidos())
                .correoInstitucional(uc.getCorreoElectronico())
                .cargoTitulo(null)
                .telefono(uc.getTelefonoContacto())
                .debeCambiarPassword(Boolean.TRUE.equals(uc.getDebeCambiarPassword()))
                .idCliente(c.getIdCliente())
                .ruc(c.getNumeroDocumento())
                .razonSocial(c.getRazonSocial())
                .contactoRepresentante(c.getContactoRepresentante())
                .direccionFiscal(c.getDireccionFiscal())
                .correoContacto(c.getCorreoContacto())
                .telefonoContacto(c.getTelefonoContacto())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioCliente obtenerUsuarioClientePorUsername(String username) {
        return usuarioClienteRepository.findByUsername(username)
                .or(() -> usuarioClienteRepository.findByCorreoElectronico(username))
                .or(() -> usuarioClienteRepository.findByCliente_NumeroDocumento(username).stream().findFirst())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario cliente no encontrado: " + username));
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalCotizacionDTO> listarCotizaciones(Integer idCliente) {
        validarCliente(idCliente);
        return cotizacionRepository.findByCliente_IdCliente(idCliente).stream()
                .map(this::mapToPortalCotizacion)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PortalCotizacionDTO buscarCotizacionPorId(Integer idCliente, Integer idCotizacion) {
        Cotizacion cotizacion = cotizacionRepository.findById(idCotizacion)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con ID: " + idCotizacion));

        if (cotizacion.getCliente() == null || !cotizacion.getCliente().getIdCliente().equals(idCliente)) {
            throw new ResourceNotFoundException("Cotización no encontrada o no pertenece a su empresa");
        }
        return mapToPortalCotizacion(cotizacion);
    }

    @Override
    @Transactional
    public PortalCotizacionDTO solicitarCotizacion(Integer idCliente, PortalCotizacionSolicitudDTO dto, Integer idUsuarioCliente) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + idCliente));

        // Las solicitudes creadas desde el Portal B2B ingresan a la bandeja de "Solicitudes Sin Asignar"
        // para que cualquier asesor comercial pueda tomarlas.
        Usuario usuarioComercial = null;

        // Generar código correlativo COT-YYYY-NNNN
        String codigoCotizacion = generarCodigoCotizacion();

        CatalogoMercancia mercancia = dto.getIdMercancia() != null
                ? mercanciaRepository.findById(dto.getIdMercancia()).orElse(null)
                : null;

        CatalogoTipoCarga tipoCarga = dto.getIdTipoCarga() != null
                ? tipoCargaRepository.findById(dto.getIdTipoCarga()).orElse(null)
                : null;

        CatalogoPuerto puertoEnvio = dto.getIdPuertoEnvio() != null
                ? puertoRepository.findById(dto.getIdPuertoEnvio()).orElse(null)
                : null;

        CatalogoPuerto puertoDestino = dto.getIdPuertoDestino() != null
                ? puertoRepository.findById(dto.getIdPuertoDestino()).orElse(null)
                : null;

        BigDecimal fob = dto.getValorFob() != null ? dto.getValorFob().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal flete = dto.getMontoFleteEstimado() != null ? dto.getMontoFleteEstimado().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        int validez = dto.getValidezDias() != null ? dto.getValidezDias() : 15;
        boolean conTransporte = Boolean.TRUE.equals(dto.getServicioTransporte());

        String origenDepto = conTransporte && dto.getOrigenDepartamento() != null ? dto.getOrigenDepartamento().trim().toUpperCase() : null;
        String origenProv = conTransporte && dto.getOrigenProvincia() != null ? dto.getOrigenProvincia().trim().toUpperCase() : null;
        String destinoDepto = conTransporte && dto.getDestinoDepartamento() != null ? dto.getDestinoDepartamento().trim().toUpperCase() : null;
        String destinoProv = conTransporte && dto.getDestinoProvincia() != null ? dto.getDestinoProvincia().trim().toUpperCase() : null;

        String obs = (dto.getObservaciones() != null && !dto.getObservaciones().isBlank())
                ? "Solicitud generada desde el Portal del Cliente: " + dto.getObservaciones().trim()
                : "Solicitud registrada por el cliente desde Portal B2B";

        Cotizacion cotizacion = Cotizacion.builder()
                .codigoCotizacion(codigoCotizacion)
                .cliente(cliente)
                .usuarioComercial(usuarioComercial)
                .mercancia(mercancia)
                .tipoCarga(tipoCarga)
                .regimenAduanero(dto.getRegimenAduanero() != null ? dto.getRegimenAduanero().trim().toUpperCase() : "IMPORTACION_DEFINITIVA")
                .puertoEnvio(puertoEnvio)
                .puertoDestino(puertoDestino)
                .servicioTransporte(conTransporte)
                .origenDepartamento(origenDepto)
                .origenProvincia(origenProv)
                .destinoDepartamento(destinoDepto)
                .destinoProvincia(destinoProv)
                .costoTransporteInterno(BigDecimal.ZERO)
                .valorFob(fob)
                .montoFleteEstimado(flete)
                .montoHonorariosEstimado(BigDecimal.ZERO)
                .montoSeguroEstimado(BigDecimal.ZERO)
                .tasaAdValorem(BigDecimal.ZERO)
                .tasaPercepcion(BigDecimal.ZERO)
                .montoTributosEstimado(BigDecimal.ZERO)
                .totalCotizado(BigDecimal.ZERO)
                .validezDias(validez)
                .fechaEmision(LocalDate.now())
                .estado(EstadoCotizacion.PENDIENTE)
                .estadoPago("SIN_PAGO")
                .totalAbonado(BigDecimal.ZERO)
                .saldoPendiente(BigDecimal.ZERO)
                .observaciones(obs)
                .build();

        Cotizacion guardada = cotizacionRepository.save(cotizacion);
        log.info("Nueva cotización {} solicitada por cliente ID {}", codigoCotizacion, idCliente);
        auditar("SOLICITAR_COTIZACION_PORTAL", "COTIZACION", guardada.getIdCotizacion(),
                "Cotización " + guardada.getCodigoCotizacion() + " solicitada desde Portal B2B");
        return mapToPortalCotizacion(guardada);
    }

    @Override
    @Transactional
    public PortalCotizacionDTO aceptarCotizacion(Integer idCliente, Integer idCotizacion) {
        Cotizacion cotizacion = cotizacionRepository.findById(idCotizacion)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con ID: " + idCotizacion));

        if (cotizacion.getCliente() == null || !cotizacion.getCliente().getIdCliente().equals(idCliente)) {
            throw new ResourceNotFoundException("Cotización no encontrada o no pertenece a su empresa");
        }

        if (cotizacion.getEstado() != EstadoCotizacion.APROBADA) {
            throw new BadRequestException("Solo se pueden aceptar cotizaciones aprobadas técnicamente por la agencia aduanera");
        }

        if ((cotizacion.getObservaciones() != null && cotizacion.getObservaciones().contains("Aceptada formalmente por el cliente"))
                || (cotizacion.getEstadoPago() != null && !"SIN_PAGO".equalsIgnoreCase(cotizacion.getEstadoPago()) && !"PENDIENTE".equalsIgnoreCase(cotizacion.getEstadoPago()))) {
            throw new BadRequestException("Esta cotización ya fue aceptada previamente.");
        }

        // Mantener el estado de pago en SIN_PAGO hasta que el cliente registre un voucher (PENDIENTE_REVISION)
        if (cotizacion.getEstadoPago() == null || cotizacion.getEstadoPago().isBlank()) {
            cotizacion.setEstadoPago("SIN_PAGO");
        }

        // Garantizar que el saldo pendiente corresponda al total cotizado menos lo abonado
        BigDecimal total = cotizacion.getTotalCotizado() != null ? cotizacion.getTotalCotizado() : BigDecimal.ZERO;
        BigDecimal abonado = cotizacion.getTotalAbonado() != null ? cotizacion.getTotalAbonado() : BigDecimal.ZERO;
        BigDecimal saldo = total.subtract(abonado);
        cotizacion.setSaldoPendiente(saldo.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : saldo);

        cotizacion.setObservaciones((cotizacion.getObservaciones() != null ? cotizacion.getObservaciones() + " | " : "") + "Aceptada formalmente por el cliente vía Portal B2B el " + LocalDateTime.now());
        Cotizacion guardada = cotizacionRepository.save(cotizacion);
        auditar("ACEPTAR_COTIZACION_PORTAL", "COTIZACION", guardada.getIdCotizacion(),
                "Cotización " + guardada.getCodigoCotizacion() + " aceptada por cliente en Portal B2B");
        return mapToPortalCotizacion(guardada);
    }

    @Override
    @Transactional
    public PortalCotizacionDTO rechazarCotizacion(Integer idCliente, Integer idCotizacion, PortalCotizacionRechazoDTO dto) {
        Cotizacion cotizacion = cotizacionRepository.findById(idCotizacion)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con ID: " + idCotizacion));

        if (cotizacion.getCliente() == null || !cotizacion.getCliente().getIdCliente().equals(idCliente)) {
            throw new ResourceNotFoundException("Cotización no encontrada o no pertenece a su empresa");
        }

        if (cotizacion.getEstado() != EstadoCotizacion.APROBADA) {
            throw new BadRequestException("Solo se pueden responder cotizaciones con aprobación técnica vigente.");
        }

        boolean tieneAbonos = cotizacion.getTotalAbonado() != null && cotizacion.getTotalAbonado().compareTo(BigDecimal.ZERO) > 0;
        boolean yaAceptada = cotizacion.getObservaciones() != null && cotizacion.getObservaciones().contains("Aceptada formalmente por el cliente");
        if (tieneAbonos || yaAceptada) {
            throw new BadRequestException("No se puede rechazar una cotización que ya fue aceptada o tiene abonos registrados.");
        }

        String accion = dto != null && dto.getAccion() != null ? dto.getAccion().trim().toUpperCase() : "RECHAZAR";
        String motivo = dto != null && dto.getMotivo() != null && !dto.getMotivo().isBlank()
                ? dto.getMotivo().trim()
                : "Sin observaciones adicionales del cliente.";

        if ("OBSERVAR".equals(accion)) {
            cotizacion.setEstado(EstadoCotizacion.OBSERVADA);
            cotizacion.setMotivoObservacion("[Solicitud de ajuste B2B] " + motivo);
        } else {
            cotizacion.setEstado(EstadoCotizacion.RECHAZADA);
            cotizacion.setMotivoObservacion("[Rechazada por cliente B2B] " + motivo);
        }

        cotizacion.setObservaciones((cotizacion.getObservaciones() != null ? cotizacion.getObservaciones() + " | " : "")
                + "Respuesta cliente B2B (" + cotizacion.getEstado().name() + ") el " + LocalDateTime.now() + ": " + motivo);
        Cotizacion guardada = cotizacionRepository.save(cotizacion);
        auditar("RECHAZAR_COTIZACION_PORTAL", "COTIZACION", guardada.getIdCotizacion(),
                "Cotización " + guardada.getCodigoCotizacion() + " marcada como " + guardada.getEstado().name() + " por el cliente");
        return mapToPortalCotizacion(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalExpedienteDTO> listarExpedientes(Integer idCliente) {
        validarCliente(idCliente);
        List<Expediente> expedientes = expedienteRepository.findByCliente_IdCliente(idCliente);
        if (expedientes.isEmpty()) {
            return List.of();
        }
        List<Integer> idsExp = expedientes.stream().map(Expediente::getIdExpediente).toList();
        Map<Integer, ControlAlmacen> almacenMap = controlAlmacenRepository.findByExpediente_IdExpedienteIn(idsExp).stream()
                .filter(a -> a.getExpediente() != null)
                .collect(Collectors.toMap(a -> a.getExpediente().getIdExpediente(), Function.identity(), (a, b) -> a));
        Map<Integer, DespachoTransporte> transporteMap = despachoTransporteRepository
                .findByExpediente_IdExpedienteInAndEstadoTransporteNot(idsExp, EstadoTransporte.CANCELADO)
                .stream()
                .filter(t -> t.getExpediente() != null)
                .collect(Collectors.toMap(t -> t.getExpediente().getIdExpediente(), Function.identity(), (a, b) -> a));

        return expedientes.stream()
                .map(e -> mapToPortalExpediente(e, almacenMap.get(e.getIdExpediente()), transporteMap.get(e.getIdExpediente())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PortalExpedienteDTO buscarExpedientePorId(Integer idCliente, Integer idExpediente) {
        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));

        if (expediente.getCliente() == null || !expediente.getCliente().getIdCliente().equals(idCliente)) {
            throw new ResourceNotFoundException("Expediente no encontrado o no pertenece a su empresa");
        }

        return mapToPortalExpediente(expediente);
    }

    @Override
    @Transactional
    public PortalExpedienteDTO crearExpedienteSimple(Integer idCliente, PortalExpedienteSimpleDTO dto, Integer idUsuarioCliente) {
        validarCliente(idCliente);
        Cotizacion cotizacion = cotizacionRepository.findById(dto.getIdCotizacion())
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con ID: " + dto.getIdCotizacion()));

        if (cotizacion.getCliente() == null || !cotizacion.getCliente().getIdCliente().equals(idCliente)) {
            throw new ResourceNotFoundException("Cotización no encontrada o no pertenece a su empresa");
        }
        if (cotizacion.getEstado() != EstadoCotizacion.APROBADA) {
            throw new BadRequestException("Solo se puede generar expediente para cotizaciones aprobadas.");
        }
        boolean tienePagoAprobado = "ADELANTO_APROBADO".equals(cotizacion.getEstadoPago())
                || "PAGADO_TOTAL".equals(cotizacion.getEstadoPago())
                || cotizacionPagoRepository.existsByCotizacion_IdCotizacionAndEstadoPago(cotizacion.getIdCotizacion(), EstadoPagoCotizacion.APROBADO);
        if (!tienePagoAprobado) {
            throw new BadRequestException("Su anticipo debe ser validado por Finanzas antes de generar el expediente.");
        }
        if (expedienteRepository.existsByCotizacion_IdCotizacion(cotizacion.getIdCotizacion())) {
            throw new BadRequestException("Ya existe un expediente aperturado para la cotización " + cotizacion.getCodigoCotizacion());
        }

        int year = LocalDate.now().getYear();
        long count = expedienteRepository.count() + 1;
        String codigoExpediente;
        do {
            codigoExpediente = String.format("EXP-%d-%04d", year, count);
            count++;
        } while (expedienteRepository.existsByCodigoExpediente(codigoExpediente));

        Expediente expediente = Expediente.builder()
                .codigoExpediente(codigoExpediente)
                .cotizacion(cotizacion)
                .cliente(cotizacion.getCliente())
                .mercancia(cotizacion.getMercancia())
                .tipoCarga(cotizacion.getTipoCarga())
                .agenteAduanero(null)
                .fechaAsignacion(null)
                .contactoExteriorNombre(dto.getContactoExteriorNombre().trim())
                .contactoExteriorTelefono(dto.getContactoExteriorTelefono())
                .contactoExteriorCorreo(dto.getContactoExteriorCorreo())
                .fechaZarpeEtd(dto.getFechaZarpeEtd())
                .fechaLlegadaEta(dto.getFechaLlegadaEta())
                .numeroContenedor(dto.getNumeroContenedor())
                .naveViaje(dto.getNaveViaje())
                .canalSunat(pe.sga.entity.enums.CanalSunat.PENDIENTE)
                .notificadoAgente(false)
                .estadoExpediente(EstadoExpediente.ASIGNADO)
                .fechaApertura(LocalDateTime.now())
                .observaciones(dto.getObservaciones())
                .build();

        Expediente guardado = expedienteRepository.save(expediente);

        if (expedienteEventoRepository != null) {
            expedienteEventoRepository.save(ExpedienteEvento.builder()
                    .expediente(guardado)
                    .codigoEstado("EXPEDIENTE_CREADO")
                    .titulo("Expediente Simple Generado (Portal B2B)")
                    .descripcion("El cliente completó contacto exterior (" + dto.getContactoExteriorNombre().trim()
                            + "). Pendiente de asignación de agente aduanero en Gestión Aduanera.")
                    .regimen(cotizacion.getRegimenAduanero())
                    .usuarioNombre("Portal B2B Cliente")
                    .fechaEvento(LocalDateTime.now())
                    .build());
        }

        crearNotificacion(idCliente,
                "Expediente Simple Creado: " + guardado.getCodigoExpediente(),
                "Su expediente " + guardado.getCodigoExpediente()
                        + " fue generado correctamente. Gestión Aduanera asignará un Agente Aduanero a la brevedad.",
                "DAM",
                "/expedientes/" + guardado.getIdExpediente());

        auditar("CREAR_EXPEDIENTE_SIMPLE_PORTAL", "EXPEDIENTE", guardado.getIdExpediente(),
                "Expediente simple " + guardado.getCodigoExpediente() + " creado desde Portal B2B");
        return mapToPortalExpediente(guardado);
    }

    @Override
    @Transactional
    public PortalExpedienteDTO actualizarDatosClienteExpediente(Integer idCliente, Integer idExpediente, PortalExpedienteDatosClienteDTO dto) {
        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));
        if (expediente.getCliente() == null || !expediente.getCliente().getIdCliente().equals(idCliente)) {
            throw new ResourceNotFoundException("Expediente no encontrado o no pertenece a su empresa");
        }
        if (expediente.getEstadoExpediente() == EstadoExpediente.CERRADO || expediente.getEstadoExpediente() == EstadoExpediente.ANULADO) {
            throw new BadRequestException("No se pueden modificar datos de un expediente cerrado o anulado.");
        }

        expediente.setContactoExteriorNombre(dto.getContactoExteriorNombre().trim());
        expediente.setContactoExteriorTelefono(dto.getContactoExteriorTelefono());
        expediente.setContactoExteriorCorreo(dto.getContactoExteriorCorreo());
        if (dto.getNumeroContenedor() != null) expediente.setNumeroContenedor(dto.getNumeroContenedor());
        if (dto.getNaveViaje() != null) expediente.setNaveViaje(dto.getNaveViaje());
        if (dto.getObservaciones() != null) expediente.setObservaciones(dto.getObservaciones());

        Expediente guardado = expedienteRepository.save(expediente);
        auditar("ACTUALIZAR_DATOS_EXPEDIENTE_PORTAL", "EXPEDIENTE", guardado.getIdExpediente(),
                "Contacto exterior actualizado en Portal B2B para " + guardado.getCodigoExpediente());
        return mapToPortalExpediente(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpedienteResponseDTO obtenerExpedienteParaInformePdf(Integer idCliente, Integer idExpediente) {
        Expediente e = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));
        if (e.getCliente() == null || !e.getCliente().getIdCliente().equals(idCliente)) {
            throw new ResourceNotFoundException("Expediente no encontrado o no pertenece a su empresa");
        }
        return ExpedienteResponseDTO.builder()
                .idExpediente(e.getIdExpediente())
                .codigoExpediente(e.getCodigoExpediente())
                .numeroDam(e.getNumeroDam())
                .canalSunat(e.getCanalSunat())
                .clienteRazonSocial(e.getCliente() != null ? e.getCliente().getRazonSocial() : null)
                .clienteNumeroDocumento(e.getCliente() != null ? e.getCliente().getNumeroDocumento() : null)
                .mercanciaNombre(e.getMercancia() != null ? e.getMercancia().getNombre()
                        : (e.getCotizacion() != null && e.getCotizacion().getMercancia() != null ? e.getCotizacion().getMercancia().getNombre() : null))
                .agenteAduaneroNombre(e.getAgenteAduanero() != null ? e.getAgenteAduanero().getNombreCompleto() : null)
                .receptorConformidadNombre(e.getReceptorConformidadNombre())
                .receptorConformidadDni(e.getReceptorConformidadDni())
                .informeConformidadTexto(e.getInformeConformidadTexto())
                .fechaInformeConformidad(e.getFechaInformeConformidad())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public PortalResumenFinancieroDTO obtenerResumenFinanciero(Integer idCliente) {
        validarCliente(idCliente);

        List<Cotizacion> cotizaciones = cotizacionRepository.findByCliente_IdCliente(idCliente);
        BigDecimal totalCotizadoUsd = cotizaciones.stream()
                .map(c -> c.getTotalCotizado() != null ? c.getTotalCotizado() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalAbonadoUsd = cotizaciones.stream()
                .map(c -> c.getTotalAbonado() != null ? c.getTotalAbonado() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal saldoPendienteUsd = cotizaciones.stream()
                .map(c -> c.getSaldoPendiente() != null ? c.getSaldoPendiente() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<LiquidacionFinanciera> liquidaciones = liquidacionFinancieraRepository.findByExpediente_Cliente_IdCliente(idCliente);
        BigDecimal totalLiquidacionesPen = liquidaciones.stream()
                .map(l -> l.getTotalGeneralPen() != null ? l.getTotalGeneralPen() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalLiquidacionesUsd = liquidaciones.stream()
                .map(l -> l.getTotalGeneralUsd() != null ? l.getTotalGeneralUsd() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDetraccionesPen = liquidaciones.stream()
                .map(l -> l.getMontoDetraccion() != null ? l.getMontoDetraccion() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long cotizacionesActivas = cotizaciones.stream()
                .filter(c -> c.getEstado() != EstadoCotizacion.ANULADA && c.getEstado() != EstadoCotizacion.RECHAZADA)
                .count();

        long expedientesEnTransito = expedienteRepository.findByCliente_IdCliente(idCliente).stream()
                .filter(e -> e.getEstadoExpediente() != EstadoExpediente.CERRADO
                        && e.getEstadoExpediente() != EstadoExpediente.ANULADO)
                .count();

        long pagosPendientes = cotizacionPagoRepository.findByCotizacion_Cliente_IdClienteOrderByFechaRegistroDesc(idCliente).stream()
                .filter(p -> p.getEstadoPago() == EstadoPagoCotizacion.PENDIENTE_REVISION)
                .count();

        return PortalResumenFinancieroDTO.builder()
                .totalCotizadoUsd(totalCotizadoUsd.setScale(2, RoundingMode.HALF_UP))
                .totalAbonadoUsd(totalAbonadoUsd.setScale(2, RoundingMode.HALF_UP))
                .saldoPendienteUsd(saldoPendienteUsd.setScale(2, RoundingMode.HALF_UP))
                .totalLiquidacionesPen(totalLiquidacionesPen.setScale(2, RoundingMode.HALF_UP))
                .totalLiquidacionesUsd(totalLiquidacionesUsd.setScale(2, RoundingMode.HALF_UP))
                .totalDetraccionesPen(totalDetraccionesPen.setScale(2, RoundingMode.HALF_UP))
                .cotizacionesActivas(cotizacionesActivas)
                .expedientesEnTransito(expedientesEnTransito)
                .pagosPendientesRevision(pagosPendientes)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalObligacionPagoDTO> listarObligacionesPendientes(Integer idCliente) {
        validarCliente(idCliente);
        List<Cotizacion> cotizaciones = cotizacionRepository.findByCliente_IdCliente(idCliente);
        if (cotizaciones.isEmpty()) {
            return List.of();
        }
        Map<Integer, Expediente> expByCotId = expedienteRepository.findByCliente_IdCliente(idCliente).stream()
                .filter(e -> e.getCotizacion() != null)
                .collect(Collectors.toMap(e -> e.getCotizacion().getIdCotizacion(), Function.identity(), (a, b) -> a));
        Map<Integer, LiquidacionFinanciera> liqByExpId = liquidacionFinancieraRepository.findByExpediente_Cliente_IdCliente(idCliente).stream()
                .filter(l -> l.getExpediente() != null)
                .collect(Collectors.toMap(l -> l.getExpediente().getIdExpediente(), Function.identity(), (a, b) -> a));

        List<PortalObligacionPagoDTO> lista = new ArrayList<>();

        for (Cotizacion c : cotizaciones) {
            // Solo cotizaciones con aprobación técnica aduanera
            if (c.getEstado() != EstadoCotizacion.APROBADA) {
                continue;
            }

            Optional<Expediente> expOpt = Optional.ofNullable(expByCotId.get(c.getIdCotizacion()));
            BigDecimal totalCot = c.getTotalCotizado() != null ? c.getTotalCotizado() : BigDecimal.ZERO;
            BigDecimal abonadoCot = c.getTotalAbonado() != null ? c.getTotalAbonado() : BigDecimal.ZERO;
            BigDecimal saldoCalculado = totalCot.subtract(abonadoCot).max(BigDecimal.ZERO);
            BigDecimal saldoCot = (c.getSaldoPendiente() != null && c.getSaldoPendiente().compareTo(BigDecimal.ZERO) > 0)
                    ? c.getSaldoPendiente()
                    : saldoCalculado;

            if (expOpt.isPresent()) {
                Expediente exp = expOpt.get();
                Optional<LiquidacionFinanciera> liqOpt = Optional.ofNullable(liqByExpId.get(exp.getIdExpediente()));
                BigDecimal liqUsd = liqOpt.map(l -> l.getTotalGeneralUsd() != null ? l.getTotalGeneralUsd() : BigDecimal.ZERO).orElse(BigDecimal.ZERO);

                BigDecimal totalConsolidado = totalCot.add(liqUsd);
                BigDecimal saldoConsolidado = saldoCot.add(liqUsd);

                if (saldoConsolidado.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal sugerido = abonadoCot.compareTo(BigDecimal.ZERO) == 0
                            ? saldoConsolidado.multiply(new BigDecimal("0.35")).setScale(2, RoundingMode.HALF_UP)
                            : saldoConsolidado;
                    lista.add(PortalObligacionPagoDTO.builder()
                            .tipoObligacion("EXPEDIENTE_HEREDADO")
                            .idReferencia(exp.getIdExpediente())
                            .idCotizacion(c.getIdCotizacion())
                            .idExpediente(exp.getIdExpediente())
                            .codigo(exp.getCodigoExpediente())
                            .codigoCotizacion(c.getCodigoCotizacion())
                            .codigoExpediente(exp.getCodigoExpediente())
                            .descripcion(c.getMercancia() != null ? c.getMercancia().getNombre() : "Despacho Aduanero")
                            .numeroDam(exp.getNumeroDam())
                            .canalSunat(exp.getCanalSunat() != null ? exp.getCanalSunat().name() : null)
                            .totalContratadoUsd(totalConsolidado.setScale(2, RoundingMode.HALF_UP))
                            .totalAmortizadoUsd(abonadoCot.setScale(2, RoundingMode.HALF_UP))
                            .saldoPendienteUsd(saldoConsolidado.setScale(2, RoundingMode.HALF_UP))
                            .montoSugeridoAnticipoUsd(sugerido)
                            .estadoPago(c.getEstadoPago())
                            .tieneExpediente(true)
                            .fechaReferencia(exp.getFechaApertura())
                            .build());
                }
            } else {
                boolean aceptadaPorCliente = (c.getObservaciones() != null && c.getObservaciones().contains("Aceptada formalmente por el cliente"))
                        || (c.getEstadoPago() != null && !"SIN_PAGO".equalsIgnoreCase(c.getEstadoPago()) && !"PENDIENTE".equalsIgnoreCase(c.getEstadoPago()));
                if (!aceptadaPorCliente && abonadoCot.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                if (saldoCot.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal sugerido = abonadoCot.compareTo(BigDecimal.ZERO) == 0
                            ? totalCot.multiply(new BigDecimal("0.35")).setScale(2, RoundingMode.HALF_UP).min(saldoCot)
                            : saldoCot;
                    lista.add(PortalObligacionPagoDTO.builder()
                            .tipoObligacion("ANTICIPO_COTIZACION")
                            .idReferencia(c.getIdCotizacion())
                            .idCotizacion(c.getIdCotizacion())
                            .idExpediente(null)
                            .codigo(c.getCodigoCotizacion())
                            .codigoCotizacion(c.getCodigoCotizacion())
                            .codigoExpediente(null)
                            .descripcion(c.getMercancia() != null ? c.getMercancia().getNombre() : "Cotización de Servicio")
                            .numeroDam(null)
                            .canalSunat(null)
                            .totalContratadoUsd(totalCot.setScale(2, RoundingMode.HALF_UP))
                            .totalAmortizadoUsd(abonadoCot.setScale(2, RoundingMode.HALF_UP))
                            .saldoPendienteUsd(saldoCot.setScale(2, RoundingMode.HALF_UP))
                            .montoSugeridoAnticipoUsd(sugerido)
                            .estadoPago(c.getEstadoPago())
                            .tieneExpediente(false)
                            .fechaReferencia(c.getFechaEmision() != null ? c.getFechaEmision().atStartOfDay() : null)
                            .build());
                }
            }
        }

        return lista;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalPagoAnticipoDTO> listarAnticipos(Integer idCliente) {
        validarCliente(idCliente);
        return cotizacionPagoRepository.findByCotizacion_Cliente_IdClienteOrderByFechaRegistroDesc(idCliente).stream()
                .map(this::mapToPortalPagoAnticipo)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalLiquidacionDTO> listarLiquidaciones(Integer idCliente) {
        validarCliente(idCliente);
        return liquidacionFinancieraRepository.findByExpediente_Cliente_IdCliente(idCliente).stream()
                .map(this::mapToPortalLiquidacion)
                .toList();
    }

    @Override
    @Transactional
    public PortalPagoAnticipoDTO registrarVoucherPago(Integer idCliente, PortalRegistroVoucherDTO dto, Integer idUsuarioCliente) {

        if ("LIQUIDACION_EXPEDIENTE".equalsIgnoreCase(dto.getTipoObligacion())) {
            LiquidacionFinanciera liquidacion = liquidacionFinancieraRepository.findById(dto.getIdReferencia())
                    .orElseThrow(() -> new ResourceNotFoundException("Liquidación no encontrada con ID: " + dto.getIdReferencia()));

            if (liquidacion.getExpediente() == null || liquidacion.getExpediente().getCliente() == null
                    || !liquidacion.getExpediente().getCliente().getIdCliente().equals(idCliente)) {
                throw new BadRequestException("La liquidación indicada no corresponde a su empresa");
            }

            DocumentoAdjunto documentoVoucher = null;
            if (dto.getIdDocumentoComprobante() != null) {
                documentoVoucher = documentoRepository.findById(dto.getIdDocumentoComprobante()).orElse(null);
            }

            liquidacion.setMetodoPago(dto.getMetodoPago());
            liquidacion.setBancoDestino(dto.getBancoDestino());
            liquidacion.setNumeroOperacion(dto.getNumeroOperacion());
            liquidacion.setFechaPago(dto.getFechaOperacion().atStartOfDay());
            liquidacion.setEstadoPago("EN_CONCILIACION");
            if (documentoVoucher != null) {
                liquidacion.setDocumentoVoucher(documentoVoucher);
            }
            liquidacion.setObservaciones((liquidacion.getObservaciones() != null ? liquidacion.getObservaciones() + " | " : "")
                    + "Voucher registrado por cliente el " + LocalDateTime.now() + " (Banco origen: " + dto.getBancoOrigen() + ")");
            liquidacionFinancieraRepository.save(liquidacion);

            auditar("REGISTRAR_VOUCHER_PORTAL", "LIQUIDACION_FINANCIERA", liquidacion.getIdLiquidacion(),
                    "Voucher de liquidación " + liquidacion.getCodigoLiquidacion()
                            + " registrado en Portal B2B (op: " + dto.getNumeroOperacion() + ")");

            BigDecimal tcLiq = liquidacion.getTipoCambio() != null && liquidacion.getTipoCambio().compareTo(BigDecimal.ZERO) > 0
                    ? liquidacion.getTipoCambio()
                    : new BigDecimal("3.7500");

            return PortalPagoAnticipoDTO.builder()
                    .codigoPago("LIQ-PAG-" + liquidacion.getIdLiquidacion())
                    .codigoCotizacion(liquidacion.getCodigoLiquidacion())
                    .tipoAbono("LIQUIDACION_FINAL")
                    .montoAbonado(dto.getMontoAbonado())
                    .moneda("USD")
                    .tipoCambio(tcLiq)
                    .montoEquivalentePen(dto.getMontoAbonado().multiply(tcLiq).setScale(2, RoundingMode.HALF_UP))
                    .metodoPago(dto.getMetodoPago())
                    .bancoOrigen(dto.getBancoOrigen())
                    .bancoDestino(dto.getBancoDestino())
                    .numeroOperacion(dto.getNumeroOperacion())
                    .fechaOperacion(dto.getFechaOperacion())
                    .estadoPago("PENDIENTE_REVISION")
                    .idDocumentoComprobante(documentoVoucher != null ? documentoVoucher.getIdDocumento() : null)
                    .documentoComprobanteNombre(documentoVoucher != null ? documentoVoucher.getNombreOriginal() : null)
                    .documentoComprobanteUrl(documentoVoucher != null ? "/api/v1/documentos/" + documentoVoucher.getIdDocumento() + "/preview" : null)
                    .fechaRegistro(LocalDateTime.now())
                    .build();
        }

        Cotizacion cotizacion;
        if ("EXPEDIENTE".equalsIgnoreCase(dto.getTipoObligacion()) || "EXPEDIENTE_HEREDADO".equalsIgnoreCase(dto.getTipoObligacion())) {
            Expediente exp = expedienteRepository.findById(dto.getIdReferencia())
                    .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + dto.getIdReferencia()));
            if (exp.getCliente() == null || !exp.getCliente().getIdCliente().equals(idCliente)) {
                throw new BadRequestException("El expediente indicado no corresponde a su empresa");
            }
            cotizacion = exp.getCotizacion();
            if (cotizacion == null) {
                throw new BadRequestException("El expediente no tiene cotización asociada");
            }
        } else {
            // ANTICIPO_COTIZACION
            cotizacion = cotizacionRepository.findById(dto.getIdReferencia())
                    .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con ID: " + dto.getIdReferencia()));
            if (cotizacion.getCliente() == null || !cotizacion.getCliente().getIdCliente().equals(idCliente)) {
                throw new BadRequestException("La cotización indicada no corresponde a su empresa");
            }
        }

        String codigoPago = generarCodigoPagoCotizacion();
        BigDecimal tipoCambio = new BigDecimal("3.7500");
        BigDecimal montoUsd = dto.getMontoAbonado();

        BigDecimal totalCotizado = cotizacion.getTotalCotizado() != null ? cotizacion.getTotalCotizado() : BigDecimal.ZERO;
        BigDecimal abonadoActual = cotizacion.getTotalAbonado() != null ? cotizacion.getTotalAbonado() : BigDecimal.ZERO;
        BigDecimal saldoActual = cotizacion.getSaldoPendiente() != null
                ? cotizacion.getSaldoPendiente()
                : totalCotizado.subtract(abonadoActual).max(BigDecimal.ZERO);

        if (abonadoActual.compareTo(BigDecimal.ZERO) == 0
                && totalCotizado.compareTo(BigDecimal.ZERO) > 0
                && montoUsd.compareTo(saldoActual) < 0) {
            BigDecimal minimo30 = totalCotizado.multiply(new BigDecimal("0.30")).setScale(2, RoundingMode.HALF_UP);
            if (montoUsd.compareTo(minimo30) <= 0) {
                throw new BadRequestException("El anticipo inicial debe ser mayor al 30% del total cotizado ($ " + minimo30 + " USD) o cubrir el 100% del saldo.");
            }
        }

        BigDecimal montoEquivalentePen = montoUsd.multiply(tipoCambio).setScale(2, RoundingMode.HALF_UP);

        DocumentoAdjunto comprobante = null;
        if (dto.getIdDocumentoComprobante() != null) {
            comprobante = documentoRepository.findById(dto.getIdDocumentoComprobante()).orElse(null);
        }

        TipoAbonoCotizacion tipoAbono = (cotizacion.getSaldoPendiente() != null
                && dto.getMontoAbonado().compareTo(cotizacion.getSaldoPendiente()) >= 0)
                ? TipoAbonoCotizacion.PAGO_TOTAL
                : TipoAbonoCotizacion.ADELANTO;

        Usuario usuarioResp = cotizacion.getUsuarioComercial() != null
                ? cotizacion.getUsuarioComercial()
                : usuarioRepository.findFirstByOrderByIdUsuarioAsc().orElse(null);

        CotizacionPago pago = CotizacionPago.builder()
                .cotizacion(cotizacion)
                .codigoPago(codigoPago)
                .tipoAbono(tipoAbono)
                .montoAbonado(montoUsd)
                .moneda("USD")
                .tipoCambio(tipoCambio)
                .montoEquivalentePen(montoEquivalentePen)
                .metodoPago(dto.getMetodoPago())
                .bancoOrigen(dto.getBancoOrigen())
                .bancoDestino(dto.getBancoDestino())
                .numeroOperacion(dto.getNumeroOperacion())
                .fechaOperacion(dto.getFechaOperacion())
                .documentoComprobante(comprobante)
                .estadoPago(EstadoPagoCotizacion.PENDIENTE_REVISION)
                .usuarioRegistro(usuarioResp)
                .fechaRegistro(LocalDateTime.now())
                .observacionFinanzas(dto.getObservaciones())
                .build();

        CotizacionPago guardado = cotizacionPagoRepository.save(pago);

        if (!"ADELANTO_APROBADO".equals(cotizacion.getEstadoPago()) && !"PAGADO_TOTAL".equals(cotizacion.getEstadoPago())) {
            cotizacion.setEstadoPago("PENDIENTE_REVISION");
            cotizacionRepository.save(cotizacion);
        }

        auditar("REGISTRAR_VOUCHER_PORTAL", "COTIZACION_PAGO", guardado.getIdPago(),
                "Voucher de anticipo registrado en Portal B2B para " + cotizacion.getCodigoCotizacion()
                        + " (op: " + guardado.getNumeroOperacion() + ")");
        return mapToPortalPagoAnticipo(guardado);
    }

    @Override
    @Transactional
    public PortalPagoAnticipoDTO corregirPagoAnticipo(Integer idCliente, Integer idPago, PortalCorreccionPagoDTO dto, Integer idUsuarioCliente) {
        CotizacionPago pago = cotizacionPagoRepository.findById(idPago)
                .orElseThrow(() -> new ResourceNotFoundException("Pago no encontrado con ID: " + idPago));

        Cotizacion cotizacion = pago.getCotizacion();
        if (cotizacion == null || cotizacion.getCliente() == null || !cotizacion.getCliente().getIdCliente().equals(idCliente)) {
            throw new BadRequestException("El registro de pago no corresponde a su empresa");
        }

        if (pago.getEstadoPago() != EstadoPagoCotizacion.OBSERVADO && pago.getEstadoPago() != EstadoPagoCotizacion.RECHAZADO && pago.getEstadoPago() != EstadoPagoCotizacion.PENDIENTE_REVISION) {
            throw new BadRequestException("Solo se pueden subsanar o corregir pagos que se encuentran observados o pendientes de revisión. Estado actual: " + pago.getEstadoPago());
        }

        BigDecimal tipoCambio = new BigDecimal("3.7500");
        BigDecimal montoPen = "USD".equalsIgnoreCase(dto.getMoneda())
                ? dto.getMontoAbonado().multiply(tipoCambio).setScale(2, RoundingMode.HALF_UP)
                : dto.getMontoAbonado();

        pago.setMontoAbonado(dto.getMontoAbonado());
        pago.setMoneda(dto.getMoneda() != null ? dto.getMoneda().trim().toUpperCase() : "USD");
        pago.setTipoCambio(tipoCambio);
        pago.setMontoEquivalentePen(montoPen);
        pago.setMetodoPago(dto.getMetodoPago());
        pago.setBancoOrigen(dto.getBancoOrigen());
        pago.setBancoDestino(dto.getBancoDestino());
        pago.setNumeroOperacion(dto.getNumeroOperacion());
        pago.setFechaOperacion(dto.getFechaOperacion());

        if (dto.getIdDocumentoComprobante() != null) {
            DocumentoAdjunto nuevoDoc = documentoRepository.findById(dto.getIdDocumentoComprobante()).orElse(null);
            if (nuevoDoc != null) {
                pago.setDocumentoComprobante(nuevoDoc);
            }
        }

        // Subsanación del pago: regresa a la bandeja de pendientes de revisión del Contador
        pago.setEstadoPago(EstadoPagoCotizacion.PENDIENTE_REVISION);
        String notaSubsanacion = "Subsanado por cliente el " + LocalDate.now()
                + (dto.getObservaciones() != null && !dto.getObservaciones().isBlank() ? ": " + dto.getObservaciones().trim() : "");
        pago.setObservacionFinanzas(notaSubsanacion);

        CotizacionPago guardado = cotizacionPagoRepository.save(pago);

        if (!"ADELANTO_APROBADO".equals(cotizacion.getEstadoPago()) && !"PAGADO_TOTAL".equals(cotizacion.getEstadoPago())) {
            cotizacion.setEstadoPago("PENDIENTE_REVISION");
            cotizacionRepository.save(cotizacion);
        }

        log.info("Pago subsanado y reenviado a revisión contable: idPago={}, clienteId={}, cotizacion={}",
                pago.getIdPago(), idCliente, cotizacion.getCodigoCotizacion());

        auditar("CORREGIR_PAGO_PORTAL", "COTIZACION_PAGO", guardado.getIdPago(),
                "Pago observado subsanado en Portal B2B para " + cotizacion.getCodigoCotizacion());
        return mapToPortalPagoAnticipo(guardado);
    }

    // =========================================================================
    // MAPPERS PRIVADOS
    // =========================================================================

    private PortalCotizacionDTO mapToPortalCotizacion(Cotizacion c) {
        boolean aceptadaPorCliente = (c.getObservaciones() != null && c.getObservaciones().contains("Aceptada formalmente por el cliente"))
                || (c.getEstadoPago() != null && !"SIN_PAGO".equalsIgnoreCase(c.getEstadoPago()));

        return PortalCotizacionDTO.builder()
                .idCotizacion(c.getIdCotizacion())
                .codigoCotizacion(c.getCodigoCotizacion())
                .idCliente(c.getCliente() != null ? c.getCliente().getIdCliente() : null)
                .razonSocialCliente(c.getCliente() != null ? c.getCliente().getRazonSocial() : null)
                .rucCliente(c.getCliente() != null ? c.getCliente().getNumeroDocumento() : null)
                .idMercancia(c.getMercancia() != null ? c.getMercancia().getIdMercancia() : null)
                .codigoMercancia(c.getMercancia() != null ? c.getMercancia().getCodigo() : null)
                .nombreMercancia(c.getMercancia() != null ? c.getMercancia().getNombre() : null)
                .idTipoCarga(c.getTipoCarga() != null ? c.getTipoCarga().getIdTipoCarga() : null)
                .nombreTipoCarga(c.getTipoCarga() != null ? c.getTipoCarga().getNombre() : null)
                .regimenAduanero(c.getRegimenAduanero())
                .idPuertoEnvio(c.getPuertoEnvio() != null ? c.getPuertoEnvio().getIdPuerto() : null)
                .nombrePuertoEnvio(c.getPuertoEnvio() != null ? c.getPuertoEnvio().getNombre() : null)
                .idPuertoDestino(c.getPuertoDestino() != null ? c.getPuertoDestino().getIdPuerto() : null)
                .nombrePuertoDestino(c.getPuertoDestino() != null ? c.getPuertoDestino().getNombre() : null)
                .servicioTransporte(c.getServicioTransporte())
                .origenDepartamento(c.getOrigenDepartamento())
                .origenProvincia(c.getOrigenProvincia())
                .destinoDepartamento(c.getDestinoDepartamento())
                .destinoProvincia(c.getDestinoProvincia())
                .costoTransporteInterno(c.getCostoTransporteInterno())
                .idUsuarioComercial(c.getUsuarioComercial() != null ? c.getUsuarioComercial().getIdUsuario() : null)
                .usuarioComercialNombre(c.getUsuarioComercial() != null ? c.getUsuarioComercial().getNombreCompleto() : null)
                .valorFob(c.getValorFob())
                .montoFlete(c.getMontoFleteEstimado())
                .montoSeguroEstimado(c.getMontoSeguroEstimado())
                .montoHonorarios(c.getMontoHonorariosEstimado())
                .tasaAdvalorem(c.getTasaAdValorem())
                .tasaPercepcion(c.getTasaPercepcion())
                .montoTributos(c.getMontoTributosEstimado())
                .totalCotizado(c.getTotalCotizado())
                .validezDias(c.getValidezDias())
                .fechaEmision(c.getFechaEmision())
                .estado(c.getEstado() != null ? c.getEstado().name() : null)
                .fechaRevision(c.getFechaRevision())
                .motivoObservacion(c.getMotivoObservacion())
                .observaciones(c.getObservaciones())
                .aceptadaPorCliente(aceptadaPorCliente)
                .estadoPago(c.getEstadoPago())
                .totalAbonado(c.getTotalAbonado())
                .saldoPendiente(c.getSaldoPendiente())
                .build();
    }

    private PortalExpedienteDTO mapToPortalExpediente(Expediente e) {
        ControlAlmacen alm = e.getIdExpediente() != null
                ? controlAlmacenRepository.findByExpediente_IdExpediente(e.getIdExpediente()).orElse(null)
                : null;
        DespachoTransporte tra = e.getIdExpediente() != null
                ? despachoTransporteRepository
                        .findFirstByExpediente_IdExpedienteAndEstadoTransporteNotOrderByIdDespachoTransporteDesc(
                                e.getIdExpediente(), EstadoTransporte.CANCELADO)
                        .orElse(null)
                : null;
        return mapToPortalExpediente(e, alm, tra);
    }

    private PortalExpedienteDTO mapToPortalExpediente(Expediente e, ControlAlmacen alm, DespachoTransporte tra) {
        var merc = e.getMercancia() != null ? e.getMercancia() : (e.getCotizacion() != null ? e.getCotizacion().getMercancia() : null);
        var tc = e.getTipoCarga() != null ? e.getTipoCarga() : (e.getCotizacion() != null ? e.getCotizacion().getTipoCarga() : null);

        List<ExpedienteEventoDTO> eventos = (expedienteEventoRepository != null && e.getIdExpediente() != null)
                ? expedienteEventoRepository.findByExpediente_IdExpedienteOrderByFechaEventoAscIdEventoAsc(e.getIdExpediente())
                        .stream()
                        .map(ev -> ExpedienteEventoDTO.builder()
                                .idEvento(ev.getIdEvento())
                                .codigoEstado(ev.getCodigoEstado())
                                .titulo(ev.getTitulo())
                                .descripcion(ev.getDescripcion())
                                .regimen(ev.getRegimen())
                                .usuarioNombre(ev.getUsuarioNombre())
                                .fechaEvento(ev.getFechaEvento())
                                .build())
                        .toList()
                : List.of();

        PortalExpedienteDTO.PortalExpedienteDTOBuilder builder = PortalExpedienteDTO.builder()
                .idExpediente(e.getIdExpediente())
                .codigoExpediente(e.getCodigoExpediente())
                .idCotizacion(e.getCotizacion() != null ? e.getCotizacion().getIdCotizacion() : null)
                .codigoCotizacion(e.getCotizacion() != null ? e.getCotizacion().getCodigoCotizacion() : null)
                .regimenAduanero(e.getCotizacion() != null ? e.getCotizacion().getRegimenAduanero() : null)
                .servicioTransporte(e.getCotizacion() != null ? e.getCotizacion().getServicioTransporte() : null)
                .idCliente(e.getCliente() != null ? e.getCliente().getIdCliente() : null)
                .razonSocialCliente(e.getCliente() != null ? e.getCliente().getRazonSocial() : null)
                .rucCliente(e.getCliente() != null ? e.getCliente().getNumeroDocumento() : null)
                .idMercancia(merc != null ? merc.getIdMercancia() : null)
                .codigoMercancia(merc != null ? merc.getCodigo() : null)
                .nombreMercancia(merc != null ? merc.getNombre() : null)
                .idTipoCarga(tc != null ? tc.getIdTipoCarga() : null)
                .nombreTipoCarga(tc != null ? tc.getNombre() : null)
                .idAgenteAduanero(e.getAgenteAduanero() != null ? e.getAgenteAduanero().getIdUsuario() : null)
                .agenteAduaneroNombre(e.getAgenteAduanero() != null ? e.getAgenteAduanero().getNombreCompleto() : null)
                .pendienteAsignacionAgente(e.getAgenteAduanero() == null)
                .contactoExteriorNombre(e.getContactoExteriorNombre())
                .contactoExteriorTelefono(e.getContactoExteriorTelefono())
                .contactoExteriorCorreo(e.getContactoExteriorCorreo())
                .numeroDam(e.getNumeroDam())
                .canalSunat(e.getCanalSunat() != null ? e.getCanalSunat().name() : null)
                .fechaAsignacionCanal(e.getFechaAsignacionCanal())
                .fechaLevanteAutorizacion(e.getFechaLevanteAutorizacion())
                .detalleInspeccion(e.getDetalleInspeccion())
                .naveViaje(e.getNaveViaje())
                .tipoTransporteInternacional(e.getTipoTransporteInternacional())
                .numeroVuelo(e.getNumeroVuelo())
                .numeroContenedor(e.getNumeroContenedor())
                .precintoAduanero(e.getPrecintoAduanero())
                .fechaZarpeEtd(e.getFechaZarpeEtd())
                .fechaLlegadaEta(e.getFechaLlegadaEta())
                .fechaArriboAduana(e.getFechaArriboAduana())
                .enEsperaTransito(Boolean.TRUE.equals(e.getEnEsperaTransito()))
                .enRevisionAduana(Boolean.TRUE.equals(e.getEnRevisionAduana()))
                .idDocumentoConformidadDestino(e.getIdDocumentoConformidadDestino())
                .verificacionOrigenOk(Boolean.TRUE.equals(e.getVerificacionOrigenOk()))
                .fechaVerificacionOrigen(e.getFechaVerificacionOrigen())
                .cuadrillaSolicitada(Boolean.TRUE.equals(e.getCuadrillaSolicitada()))
                .cuadrillaDetalle(e.getCuadrillaDetalle())
                .fechaCitaInspeccion(e.getFechaCitaInspeccion())
                .codigoCitaInspeccion(e.getCodigoCitaInspeccion())
                .revisionRojoFinalizada(Boolean.TRUE.equals(e.getRevisionRojoFinalizada()))
                .envioSunatSimuladoOk(Boolean.TRUE.equals(e.getEnvioSunatSimuladoOk()))
                .codigoTransmisionSunat(e.getCodigoTransmisionSunat())
                .fechaEnvioSunat(e.getFechaEnvioSunat())
                .entregadoAlmacenVerde(Boolean.TRUE.equals(e.getEntregadoAlmacenVerde()))
                .fechaEntregaAlmacenVerde(e.getFechaEntregaAlmacenVerde())
                .informeConformidadTexto(e.getInformeConformidadTexto())
                .fechaInformeConformidad(e.getFechaInformeConformidad())
                .receptorConformidadNombre(e.getReceptorConformidadNombre())
                .receptorConformidadDni(e.getReceptorConformidadDni())
                .idDocumentoConformidadInforme(e.getIdDocumentoConformidadInforme())
                .eventos(eventos)
                .estadoExpediente(e.getEstadoExpediente() != null ? e.getEstadoExpediente().name() : null)
                .fechaApertura(e.getFechaApertura())
                .fechaCierre(e.getFechaCierre())
                .observaciones(e.getObservaciones())
                .tieneAlmacen(false)
                .tieneTransporte(false);

        if (alm != null) {
            builder.tieneAlmacen(true)
                    .depositoTemporal(alm.getDepositoTemporal())
                    .ubicacionBahia(alm.getUbicacionBahia())
                    .diasLibresAlmacen(alm.getDiasLibresAlmacen())
                    .fechaLimiteRetiro(alm.getFechaLimiteRetiro())
                    .cantidadBultos(alm.getCantidadBultos())
                    .pesoBrutoKg(alm.getPesoBrutoKg())
                    .estadoAlmacen(alm.getEstadoAlmacen());

            if (alm.getFechaLimiteRetiro() != null) {
                long dias = ChronoUnit.DAYS.between(LocalDate.now(), alm.getFechaLimiteRetiro());
                builder.diasRestantesAlmacen(dias);
            }
        }

        if (tra != null && tra.getEstadoTransporte() != EstadoTransporte.CANCELADO) {
            builder.tieneTransporte(true)
                    .codigoTracking(tra.getCodigoTracking())
                    .tipoTraslado(tra.getTipoTraslado() != null ? tra.getTipoTraslado().name() : null)
                    .guiaRemisionTransporte(tra.getGuiaRemisionTransporte())
                    .placaTracto(tra.getUnidad() != null ? tra.getUnidad().getPlacaTracto() : null)
                    .conductorNombre(tra.getConductor() != null ? (tra.getConductor().getNombres() + " " + tra.getConductor().getApellidos()) : null)
                    .direccionPartida(tra.getDireccionPartida())
                    .direccionLlegada(tra.getDireccionLlegada())
                    .distritoLlegada(tra.getDistritoLlegada())
                    .fechaProgramadaTransporte(tra.getFechaProgramada())
                    .fechaSalidaRuta(tra.getFechaSalidaRuta())
                    .fechaEntregaConforme(tra.getFechaEntregaConforme())
                    .estadoTransporte(tra.getEstadoTransporte() != null ? tra.getEstadoTransporte().name() : null);
        }

        return builder.build();
    }

    private PortalPagoAnticipoDTO mapToPortalPagoAnticipo(CotizacionPago p) {
        ComprobanteFactura factura = (comprobanteFacturaRepository != null && p.getIdPago() != null)
                ? comprobanteFacturaRepository
                        .findFirstByPagoCotizacion_IdPagoAndEstadoOrderByIdComprobanteDesc(p.getIdPago(), "EMITIDA")
                        .orElse(null)
                : null;
        return PortalPagoAnticipoDTO.builder()
                .idPago(p.getIdPago())
                .idCotizacion(p.getCotizacion() != null ? p.getCotizacion().getIdCotizacion() : null)
                .codigoCotizacion(p.getCotizacion() != null ? p.getCotizacion().getCodigoCotizacion() : null)
                .codigoPago(p.getCodigoPago())
                .tipoAbono(p.getTipoAbono() != null ? p.getTipoAbono().name() : null)
                .montoAbonado(p.getMontoAbonado())
                .moneda(p.getMoneda())
                .tipoCambio(p.getTipoCambio())
                .montoEquivalentePen(p.getMontoEquivalentePen())
                .metodoPago(p.getMetodoPago())
                .bancoOrigen(p.getBancoOrigen())
                .bancoDestino(p.getBancoDestino())
                .numeroOperacion(p.getNumeroOperacion())
                .fechaOperacion(p.getFechaOperacion())
                .estadoPago(p.getEstadoPago() != null ? p.getEstadoPago().name() : null)
                .observacionFinanzas(p.getObservacionFinanzas())
                .idDocumentoComprobante(p.getDocumentoComprobante() != null ? p.getDocumentoComprobante().getIdDocumento() : null)
                .documentoComprobanteNombre(p.getDocumentoComprobante() != null ? p.getDocumentoComprobante().getNombreOriginal() : null)
                .documentoComprobanteUrl(p.getDocumentoComprobante() != null ? "/api/v1/documentos/" + p.getDocumentoComprobante().getIdDocumento() + "/preview" : null)
                .idComprobanteFactura(factura != null ? factura.getIdComprobante() : null)
                .numeroFacturaEmitida(factura != null ? factura.getNumeroCompleto() : null)
                .tipoComprobanteFactura(factura != null ? factura.getTipoComprobante() : null)
                .fechaRegistro(p.getFechaRegistro())
                .build();
    }

    private PortalLiquidacionDTO mapToPortalLiquidacion(LiquidacionFinanciera l) {
        ComprobanteFactura factura = (comprobanteFacturaRepository != null && l.getIdLiquidacion() != null)
                ? comprobanteFacturaRepository
                        .findFirstByLiquidacion_IdLiquidacionAndEstadoOrderByIdComprobanteDesc(l.getIdLiquidacion(), "EMITIDA")
                        .orElse(null)
                : null;
        return PortalLiquidacionDTO.builder()
                .idLiquidacion(l.getIdLiquidacion())
                .codigoLiquidacion(l.getCodigoLiquidacion())
                .idExpediente(l.getExpediente() != null ? l.getExpediente().getIdExpediente() : null)
                .codigoExpediente(l.getExpediente() != null ? l.getExpediente().getCodigoExpediente() : null)
                .numeroDam(l.getExpediente() != null ? l.getExpediente().getNumeroDam() : null)
                .totalTributosSunat(l.getTotalTributosSunat())
                .gastosAlmacenPuerto(l.getGastosAlmacenPuerto())
                .fleteTransporteLocal(l.getFleteTransporteLocal())
                .honorariosAgencia(l.getHonorariosAgencia())
                .subtotalImponible(l.getSubtotalImponible())
                .igvMonto(l.getIgvMonto())
                .aplicaDetraccion(l.getAplicaDetraccion())
                .porcentajeDetraccion(l.getPorcentajeDetraccion())
                .montoDetraccion(l.getMontoDetraccion())
                .totalGeneralPen(l.getTotalGeneralPen())
                .totalGeneralUsd(l.getTotalGeneralUsd())
                .tipoCambio(l.getTipoCambio())
                .estadoPago(l.getEstadoPago())
                .numeroFactura(factura != null ? factura.getNumeroCompleto() : l.getNumeroFactura())
                .idComprobanteFactura(factura != null ? factura.getIdComprobante() : null)
                .fechaEmision(l.getFechaEmision())
                .fechaPago(l.getFechaPago())
                .metodoPago(l.getMetodoPago())
                .bancoDestino(l.getBancoDestino())
                .numeroOperacion(l.getNumeroOperacion())
                .build();
    }

    private void validarCliente(Integer idCliente) {
        if (idCliente == null) {
            throw new BadRequestException("El identificador del cliente es requerido");
        }
        if (!clienteRepository.existsById(idCliente)) {
            throw new ResourceNotFoundException("Cliente no registrado en el sistema con ID: " + idCliente);
        }
    }

    private String generarCodigoCotizacion() {
        int year = LocalDate.now().getYear();
        long count = cotizacionRepository.count() + 1;
        String codigo;
        do {
            codigo = String.format("COT-%d-%04d", year, count);
            count++;
        } while (cotizacionRepository.existsByCodigoCotizacion(codigo));
        return codigo;
    }

    private String generarCodigoPagoCotizacion() {
        int year = LocalDate.now().getYear();
        String prefix = String.format("PAG-COT-%d-", year);
        long nextSeq = cotizacionPagoRepository.findUltimoPagoPorPrefijo(prefix)
                .map(CotizacionPago::getCodigoPago)
                .map(cod -> {
                    try {
                        String numPart = cod.substring(prefix.length());
                        return Long.parseLong(numPart) + 1;
                    } catch (Exception e) {
                        return cotizacionPagoRepository.count() + 1;
                    }
                })
                .orElseGet(() -> cotizacionPagoRepository.count() + 1);

        String codigo;
        do {
            codigo = String.format("PAG-COT-%d-%04d", year, nextSeq);
            nextSeq++;
        } while (cotizacionPagoRepository.existsByCodigoPago(codigo));
        return codigo;
    }

    @Override
    @Transactional
    public void cambiarPassword(UsuarioCliente usuarioCliente, PortalCambioPasswordDTO dto) {
        if (!passwordEncoder.matches(dto.getPasswordActual(), usuarioCliente.getPasswordHash())) {
            throw new BadRequestException("La contraseña actual ingresada es incorrecta");
        }
        if (passwordEncoder.matches(dto.getNuevaPassword(), usuarioCliente.getPasswordHash())) {
            throw new BadRequestException("La nueva contraseña no puede ser idéntica a la anterior");
        }

        usuarioCliente.setPasswordHash(passwordEncoder.encode(dto.getNuevaPassword()));
        usuarioCliente.setDebeCambiarPassword(false);
        usuarioCliente.setFechaActualizacion(LocalDateTime.now());
        usuarioClienteRepository.save(usuarioCliente);

        crearNotificacion(
                usuarioCliente.getCliente().getIdCliente(),
                "Contraseña Actualizada Exitosamente",
                "Su contraseña de acceso al portal B2B ha sido cambiada. Su cuenta corporativa se encuentra regularizada.",
                "SEGURIDAD",
                "/cotizaciones"
        );
        log.info("Contraseña actualizada exitosamente para usuario cliente: id={}, username={}",
                usuarioCliente.getIdUsuarioCliente(), usuarioCliente.getUsername());
        auditarActor(usuarioCliente.getUsername(), "CAMBIAR_PASSWORD_PORTAL",
                usuarioCliente.getIdUsuarioCliente(),
                "Contraseña de acceso al Portal B2B actualizada por el usuario", "EXITO");
    }

    @Override
    @Transactional(readOnly = true)
    public List<NotificacionClienteDTO> listarNotificaciones(Integer idCliente) {
        validarCliente(idCliente);
        return notificacionClienteRepository.findByCliente_IdClienteOrderByFechaCreacionDesc(idCliente).stream()
                .map(this::mapToNotificacionDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long contarNotificacionesNoLeidas(Integer idCliente) {
        validarCliente(idCliente);
        return notificacionClienteRepository.countByCliente_IdClienteAndLeidoFalse(idCliente);
    }

    @Override
    @Transactional
    public void marcarNotificacionLeida(Integer idCliente, Integer idNotificacion) {
        validarCliente(idCliente);
        NotificacionCliente notif = notificacionClienteRepository.findByCliente_IdClienteAndIdNotificacion(idCliente, idNotificacion)
                .orElseThrow(() -> new ResourceNotFoundException("Notificación no encontrada"));
        if (!Boolean.TRUE.equals(notif.getLeido())) {
            notif.setLeido(true);
            notificacionClienteRepository.save(notif);
        }
    }

    @Override
    @Transactional
    public void marcarTodasNotificacionesLeidas(Integer idCliente) {
        validarCliente(idCliente);
        notificacionClienteRepository.marcarTodasLeidasPorCliente(idCliente);
    }

    @Override
    @Transactional
    public void crearNotificacion(Integer idCliente, String titulo, String mensaje, String tipo, String enlace) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + idCliente));

        NotificacionCliente notif = NotificacionCliente.builder()
                .cliente(cliente)
                .titulo(titulo)
                .mensaje(mensaje)
                .tipo(tipo != null ? tipo : "SISTEMA")
                .leido(false)
                .enlace(enlace)
                .fechaCreacion(LocalDateTime.now())
                .build();

        notificacionClienteRepository.save(notif);
    }

    private NotificacionClienteDTO mapToNotificacionDTO(NotificacionCliente n) {
        return NotificacionClienteDTO.builder()
                .idNotificacion(n.getIdNotificacion())
                .idCliente(n.getCliente().getIdCliente())
                .titulo(n.getTitulo())
                .mensaje(n.getMensaje())
                .tipo(n.getTipo())
                .leido(n.getLeido())
                .enlace(n.getEnlace())
                .fechaCreacion(n.getFechaCreacion())
                .build();
    }
}
