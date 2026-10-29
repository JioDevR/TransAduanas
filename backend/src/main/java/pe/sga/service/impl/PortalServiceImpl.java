package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.config.JwtTokenProvider;
import pe.sga.dto.portal.*;
import pe.sga.dto.request.LoginRequestDTO;
import pe.sga.entity.*;
import pe.sga.entity.enums.EstadoCotizacion;
import pe.sga.entity.enums.EstadoPagoCotizacion;
import pe.sga.entity.enums.RolUsuario;
import pe.sga.entity.enums.TipoAbonoCotizacion;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.*;
import pe.sga.service.IPortalService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    @Override
    @Transactional
    public PortalAuthResponseDTO autenticarCliente(LoginRequestDTO loginRequest) {
        String identifier = loginRequest.getUsername() != null ? loginRequest.getUsername().trim() : "";
        UsuarioCliente uc = usuarioClienteRepository.findByUsername(identifier)
                .or(() -> usuarioClienteRepository.findByCorreoElectronico(identifier))
                .or(() -> usuarioClienteRepository.findByCliente_NumeroDocumento(identifier).stream().findFirst())
                .orElseThrow(() -> new BadCredentialsException("Credenciales de cliente incorrectas"));

        if (!Boolean.TRUE.equals(uc.getEstadoActivo())) {
            throw new BadCredentialsException("La cuenta de cliente se encuentra inactiva");
        }

        if (uc.getCliente() == null || !Boolean.TRUE.equals(uc.getCliente().getEstadoActivo())) {
            throw new BadCredentialsException("La empresa cliente se encuentra inactiva en el sistema");
        }

        if (!passwordEncoder.matches(loginRequest.getPassword(), uc.getPasswordHash())) {
            throw new BadCredentialsException("Credenciales de cliente incorrectas");
        }

        uc.setUltimoAcceso(LocalDateTime.now());
        usuarioClienteRepository.save(uc);

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

        Usuario usuarioComercial = usuarioRepository.findByRol(RolUsuario.AGENTE_COMERCIAL).stream().findFirst()
                .orElseGet(() -> usuarioRepository.findAll().stream().findFirst().orElse(null));

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

        BigDecimal fob = dto.getValorFob() != null ? dto.getValorFob() : BigDecimal.ZERO;
        BigDecimal fleteEstimado = fob.multiply(new BigDecimal("0.10")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal honorariosEstimados = new BigDecimal("500.00");
        BigDecimal seguroEstimado = fob.multiply(new BigDecimal("0.0075")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tasaAdv = mercancia != null && mercancia.getArancelBasePct() != null ? mercancia.getArancelBasePct() : BigDecimal.ZERO;
        BigDecimal cif = fob.add(fleteEstimado).add(seguroEstimado);
        BigDecimal advalorem = cif.multiply(tasaAdv.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP)).setScale(2, RoundingMode.HALF_UP);
        BigDecimal baseIgv = cif.add(advalorem);
        BigDecimal igv = baseIgv.multiply(new BigDecimal("0.18")).setScale(2, RoundingMode.HALF_UP);
        BigDecimal tributosEstimados = advalorem.add(igv);
        BigDecimal totalEstimado = fleteEstimado.add(honorariosEstimados).add(tributosEstimados);

        Cotizacion cotizacion = Cotizacion.builder()
                .codigoCotizacion(codigoCotizacion)
                .cliente(cliente)
                .usuarioComercial(usuarioComercial)
                .mercancia(mercancia)
                .tipoCarga(tipoCarga)
                .regimenAduanero(dto.getRegimenAduanero() != null ? dto.getRegimenAduanero().trim().toUpperCase() : "IMPORTACION_DEFINITIVA")
                .puertoEnvio(puertoEnvio)
                .puertoDestino(puertoDestino)
                .servicioTransporte(Boolean.TRUE.equals(dto.getServicioTransporte()))
                .origenDepartamento(dto.getOrigenDepartamento())
                .origenProvincia(dto.getOrigenProvincia())
                .destinoDepartamento(dto.getDestinoDepartamento())
                .destinoProvincia(dto.getDestinoProvincia())
                .costoTransporteInterno(BigDecimal.ZERO)
                .valorFob(fob)
                .montoFleteEstimado(fleteEstimado)
                .montoHonorariosEstimado(honorariosEstimados)
                .montoSeguroEstimado(seguroEstimado)
                .tasaAdValorem(tasaAdv)
                .tasaPercepcion(new BigDecimal("3.50"))
                .montoTributosEstimado(tributosEstimados)
                .totalCotizado(totalEstimado)
                .validezDias(15)
                .fechaEmision(LocalDate.now())
                .estado(EstadoCotizacion.PENDIENTE)
                .estadoPago("SIN_PAGO")
                .totalAbonado(BigDecimal.ZERO)
                .saldoPendiente(totalEstimado)
                .observaciones(dto.getObservaciones() != null ? "Solicitud generada desde el Portal del Cliente: " + dto.getObservaciones() : "Solicitud registrada por el cliente desde Portal B2B")
                .build();

        Cotizacion guardada = cotizacionRepository.save(cotizacion);
        log.info("Nueva cotización {} solicitada por cliente ID {}", codigoCotizacion, idCliente);
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

        cotizacion.setObservaciones((cotizacion.getObservaciones() != null ? cotizacion.getObservaciones() + " | " : "") + "Aceptada formalmente por el cliente vía Portal B2B el " + LocalDateTime.now());
        Cotizacion guardada = cotizacionRepository.save(cotizacion);
        return mapToPortalCotizacion(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PortalExpedienteDTO> listarExpedientes(Integer idCliente) {
        validarCliente(idCliente);
        return expedienteRepository.findByCliente_IdCliente(idCliente).stream()
                .map(this::mapToPortalExpediente)
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
                .filter(e -> !"FINALIZADO".equalsIgnoreCase(e.getEstadoExpediente().name()))
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
        List<PortalObligacionPagoDTO> lista = new ArrayList<>();

        for (Cotizacion c : cotizaciones) {
            if (c.getEstado() == EstadoCotizacion.ANULADA || c.getEstado() == EstadoCotizacion.RECHAZADA) {
                continue;
            }

            Optional<Expediente> expOpt = expedienteRepository.findByCotizacion_IdCotizacion(c.getIdCotizacion());
            BigDecimal saldoCot = c.getSaldoPendiente() != null ? c.getSaldoPendiente() : BigDecimal.ZERO;
            BigDecimal totalCot = c.getTotalCotizado() != null ? c.getTotalCotizado() : BigDecimal.ZERO;
            BigDecimal abonadoCot = c.getTotalAbonado() != null ? c.getTotalAbonado() : BigDecimal.ZERO;

            if (expOpt.isPresent()) {
                Expediente exp = expOpt.get();
                Optional<LiquidacionFinanciera> liqOpt = liquidacionFinancieraRepository.findByExpediente_IdExpediente(exp.getIdExpediente());
                BigDecimal liqUsd = liqOpt.map(l -> l.getTotalGeneralUsd() != null ? l.getTotalGeneralUsd() : BigDecimal.ZERO).orElse(BigDecimal.ZERO);

                BigDecimal totalConsolidado = totalCot.add(liqUsd);
                BigDecimal saldoConsolidado = saldoCot.add(liqUsd);

                if (saldoConsolidado.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal sugerido = saldoConsolidado.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
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
                if (saldoCot.compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal sugerido = saldoCot.divide(new BigDecimal("2"), 2, RoundingMode.HALF_UP);
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

            liquidacion.setMetodoPago(dto.getMetodoPago());
            liquidacion.setBancoDestino(dto.getBancoDestino());
            liquidacion.setNumeroOperacion(dto.getNumeroOperacion());
            liquidacion.setFechaPago(dto.getFechaOperacion().atStartOfDay());
            liquidacion.setEstadoPago("EN_CONCILIACION");
            liquidacion.setObservaciones((liquidacion.getObservaciones() != null ? liquidacion.getObservaciones() + " | " : "")
                    + "Voucher registrado por cliente el " + LocalDateTime.now() + " (Banco origen: " + dto.getBancoOrigen() + ")");
            liquidacionFinancieraRepository.save(liquidacion);

            return PortalPagoAnticipoDTO.builder()
                    .codigoPago("LIQ-PAG-" + liquidacion.getIdLiquidacion())
                    .codigoCotizacion(liquidacion.getCodigoLiquidacion())
                    .tipoAbono("LIQUIDACION_FINAL")
                    .montoAbonado(dto.getMontoAbonado())
                    .moneda("USD")
                    .metodoPago(dto.getMetodoPago())
                    .bancoOrigen(dto.getBancoOrigen())
                    .bancoDestino(dto.getBancoDestino())
                    .numeroOperacion(dto.getNumeroOperacion())
                    .fechaOperacion(dto.getFechaOperacion())
                    .estadoPago("PENDIENTE_REVISION")
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

        String codigoPago = "PAG-" + LocalDate.now().getYear() + "-" + String.format("%04d", cotizacionPagoRepository.count() + 1);
        BigDecimal tipoCambio = new BigDecimal("1.0000");
        BigDecimal montoUsd = dto.getMontoAbonado();

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
                : usuarioRepository.findAll().stream().findFirst().orElse(null);

        CotizacionPago pago = CotizacionPago.builder()
                .cotizacion(cotizacion)
                .codigoPago(codigoPago)
                .tipoAbono(tipoAbono)
                .montoAbonado(montoUsd)
                .moneda("USD")
                .tipoCambio(tipoCambio)
                .montoEquivalentePen(montoUsd)
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

        return mapToPortalPagoAnticipo(guardado);
    }

    // =========================================================================
    // MAPPERS PRIVADOS
    // =========================================================================

    private PortalCotizacionDTO mapToPortalCotizacion(Cotizacion c) {
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
                .estadoPago(c.getEstadoPago())
                .totalAbonado(c.getTotalAbonado())
                .saldoPendiente(c.getSaldoPendiente())
                .build();
    }

    private PortalExpedienteDTO mapToPortalExpediente(Expediente e) {
        PortalExpedienteDTO.PortalExpedienteDTOBuilder builder = PortalExpedienteDTO.builder()
                .idExpediente(e.getIdExpediente())
                .codigoExpediente(e.getCodigoExpediente())
                .idCotizacion(e.getCotizacion() != null ? e.getCotizacion().getIdCotizacion() : null)
                .codigoCotizacion(e.getCotizacion() != null ? e.getCotizacion().getCodigoCotizacion() : null)
                .idCliente(e.getCliente() != null ? e.getCliente().getIdCliente() : null)
                .razonSocialCliente(e.getCliente() != null ? e.getCliente().getRazonSocial() : null)
                .rucCliente(e.getCliente() != null ? e.getCliente().getNumeroDocumento() : null)
                .idMercancia(e.getCotizacion() != null && e.getCotizacion().getMercancia() != null ? e.getCotizacion().getMercancia().getIdMercancia() : null)
                .codigoMercancia(e.getCotizacion() != null && e.getCotizacion().getMercancia() != null ? e.getCotizacion().getMercancia().getCodigo() : null)
                .nombreMercancia(e.getCotizacion() != null && e.getCotizacion().getMercancia() != null ? e.getCotizacion().getMercancia().getNombre() : null)
                .idTipoCarga(e.getCotizacion() != null && e.getCotizacion().getTipoCarga() != null ? e.getCotizacion().getTipoCarga().getIdTipoCarga() : null)
                .nombreTipoCarga(e.getCotizacion() != null && e.getCotizacion().getTipoCarga() != null ? e.getCotizacion().getTipoCarga().getNombre() : null)
                .agenteAduaneroNombre(e.getAgenteAduanero() != null ? e.getAgenteAduanero().getNombreCompleto() : null)
                .numeroDam(e.getNumeroDam())
                .canalSunat(e.getCanalSunat() != null ? e.getCanalSunat().name() : null)
                .fechaAsignacionCanal(e.getFechaAsignacionCanal())
                .fechaLevanteAutorizacion(e.getFechaLevanteAutorizacion())
                .detalleInspeccion(e.getDetalleInspeccion())
                .naveViaje(e.getNaveViaje())
                .numeroContenedor(e.getNumeroContenedor())
                .precintoAduanero(e.getPrecintoAduanero())
                .fechaZarpeEtd(e.getFechaZarpeEtd())
                .fechaLlegadaEta(e.getFechaLlegadaEta())
                .fechaArriboAduana(e.getFechaArriboAduana())
                .estadoExpediente(e.getEstadoExpediente() != null ? e.getEstadoExpediente().name() : null)
                .fechaApertura(e.getFechaApertura())
                .fechaCierre(e.getFechaCierre())
                .observaciones(e.getObservaciones())
                .tieneAlmacen(false)
                .tieneTransporte(false);

        // Asociar control de almacén si existe
        if (e.getIdExpediente() != null) {
            controlAlmacenRepository.findByExpediente_IdExpediente(e.getIdExpediente()).ifPresent(alm -> {
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
            });

            despachoTransporteRepository.findByExpediente_IdExpediente(e.getIdExpediente()).ifPresent(tra -> {
                builder.tieneTransporte(true)
                        .codigoTracking(tra.getCodigoTracking())
                        .placaTracto(tra.getUnidad() != null ? tra.getUnidad().getPlacaTracto() : null)
                        .conductorNombre(tra.getConductor() != null ? (tra.getConductor().getNombres() + " " + tra.getConductor().getApellidos()) : null)
                        .direccionPartida(tra.getDireccionPartida())
                        .direccionLlegada(tra.getDireccionLlegada())
                        .distritoLlegada(tra.getDistritoLlegada())
                        .fechaProgramadaTransporte(tra.getFechaProgramada())
                        .estadoTransporte(tra.getEstadoTransporte());
            });
        }

        return builder.build();
    }

    private PortalPagoAnticipoDTO mapToPortalPagoAnticipo(CotizacionPago p) {
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
                .documentoComprobanteUrl(p.getDocumentoComprobante() != null ? p.getDocumentoComprobante().getRutaAlmacenamiento() : null)
                .fechaRegistro(p.getFechaRegistro())
                .build();
    }

    private PortalLiquidacionDTO mapToPortalLiquidacion(LiquidacionFinanciera l) {
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
                .numeroFactura(l.getNumeroFactura())
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
        notif.setLeido(true);
        notificacionClienteRepository.save(notif);
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
