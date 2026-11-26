package pe.sga.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.sga.config.JwtTokenProvider;
import pe.sga.dto.portal.PortalCotizacionDTO;
import pe.sga.dto.portal.PortalCotizacionRechazoDTO;
import pe.sga.dto.portal.PortalExpedienteDTO;
import pe.sga.dto.portal.PortalExpedienteSimpleDTO;
import pe.sga.dto.portal.PortalObligacionPagoDTO;
import pe.sga.dto.portal.PortalRegistroVoucherDTO;
import pe.sga.entity.Cliente;
import pe.sga.entity.Cotizacion;
import pe.sga.entity.CotizacionPago;
import pe.sga.entity.Expediente;
import pe.sga.entity.NotificacionCliente;
import pe.sga.entity.Usuario;
import pe.sga.entity.enums.EstadoCotizacion;
import pe.sga.entity.enums.EstadoExpediente;
import pe.sga.entity.enums.RolUsuario;
import pe.sga.exception.BadRequestException;
import pe.sga.repository.*;
import pe.sga.service.impl.PortalServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unitaria")
public class PortalServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioClienteRepository usuarioClienteRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private CotizacionRepository cotizacionRepository;
    @Mock private ExpedienteRepository expedienteRepository;
    @Mock private CotizacionPagoRepository cotizacionPagoRepository;
    @Mock private LiquidacionFinancieraRepository liquidacionFinancieraRepository;
    @Mock private ControlAlmacenRepository controlAlmacenRepository;
    @Mock private CatalogoMercanciaRepository mercanciaRepository;
    @Mock private CatalogoTipoCargaRepository tipoCargaRepository;
    @Mock private CatalogoPuertoRepository puertoRepository;
    @Mock private IDocumentoAdjuntoRepository documentoRepository;
    @Mock private NotificacionClienteRepository notificacionClienteRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider tokenProvider;

    @InjectMocks
    private PortalServiceImpl portalService;

    private Cliente cliente;
    private Cotizacion cotizacion;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder()
                .idCliente(10)
                .razonSocial("Importadora Lima SAC")
                .numeroDocumento("20123456789")
                .build();

        cotizacion = Cotizacion.builder()
                .idCotizacion(55)
                .codigoCotizacion("COT-2026-0055")
                .cliente(cliente)
                .estado(EstadoCotizacion.APROBADA)
                .estadoPago("SIN_PAGO")
                .totalCotizado(new BigDecimal("1500.00"))
                .totalAbonado(BigDecimal.ZERO)
                .saldoPendiente(new BigDecimal("1500.00"))
                .build();
    }

    @Test
    void aceptarCotizacion_mantieneEstadoPagoSinPagoHastaRegistroDeVoucher() {
        when(cotizacionRepository.findById(55)).thenReturn(Optional.of(cotizacion));
        when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(i -> i.getArgument(0));

        PortalCotizacionDTO dto = portalService.aceptarCotizacion(10, 55);

        assertEquals("SIN_PAGO", dto.getEstadoPago());
        assertTrue(dto.getAceptadaPorCliente());
        assertEquals(new BigDecimal("1500.00"), dto.getSaldoPendiente());
        assertTrue(dto.getObservaciones().contains("Aceptada formalmente por el cliente"));
    }

    @Test
    void registrarVoucherPago_usaFindFirstByOrderByIdUsuarioAscCuandoNoHayComercial() {
        Usuario fallback = Usuario.builder()
                .idUsuario(1)
                .username("admin")
                .rol(RolUsuario.ADMINISTRADOR_TI)
                .build();

        when(cotizacionRepository.findById(55)).thenReturn(Optional.of(cotizacion));
        when(usuarioRepository.findFirstByOrderByIdUsuarioAsc()).thenReturn(Optional.of(fallback));
        when(cotizacionPagoRepository.save(any(CotizacionPago.class))).thenAnswer(i -> {
            CotizacionPago p = i.getArgument(0);
            p.setIdPago(100);
            return p;
        });

        PortalRegistroVoucherDTO req = PortalRegistroVoucherDTO.builder()
                .tipoObligacion("ANTICIPO_COTIZACION")
                .idReferencia(55)
                .montoAbonado(new BigDecimal("500.00"))
                .metodoPago("TRANSFERENCIA")
                .bancoOrigen("BCP")
                .bancoDestino("BBVA")
                .numeroOperacion("OP-998877")
                .fechaOperacion(LocalDate.now())
                .build();

        portalService.registrarVoucherPago(10, req, 20);

        verify(usuarioRepository, times(1)).findFirstByOrderByIdUsuarioAsc();
        verify(usuarioRepository, never()).findAll();
        assertEquals("PENDIENTE_REVISION", cotizacion.getEstadoPago());
    }

    @Test
    void marcarNotificacionLeida_omiteGuardadoSiYaEstabaLeida() {
        NotificacionCliente leida = NotificacionCliente.builder()
                .idNotificacion(7)
                .cliente(cliente)
                .titulo("Aviso")
                .mensaje("Detalle")
                .leido(true)
                .build();

        when(clienteRepository.existsById(10)).thenReturn(true);
        when(notificacionClienteRepository.findByCliente_IdClienteAndIdNotificacion(10, 7))
                .thenReturn(Optional.of(leida));

        portalService.marcarNotificacionLeida(10, 7);

        verify(notificacionClienteRepository, never()).save(any());
    }

    @Test
    void registrarVoucherPago_rechazaAnticipoMenorOIgualAl30PorCiento() {
        when(cotizacionRepository.findById(55)).thenReturn(Optional.of(cotizacion));

        PortalRegistroVoucherDTO req = PortalRegistroVoucherDTO.builder()
                .tipoObligacion("ANTICIPO_COTIZACION")
                .idReferencia(55)
                .montoAbonado(new BigDecimal("450.00")) // exactamente 30% de 1500
                .metodoPago("TRANSFERENCIA")
                .bancoOrigen("BCP")
                .bancoDestino("BCP Dolares")
                .numeroOperacion("OP-3000")
                .fechaOperacion(LocalDate.now())
                .build();

        assertThrows(BadRequestException.class, () -> portalService.registrarVoucherPago(10, req, 20));
        verify(cotizacionPagoRepository, never()).save(any());
    }

    @Test
    void rechazarCotizacion_cambiaEstadoYMotivoCuandoNoTienePagos() {
        when(cotizacionRepository.findById(55)).thenReturn(Optional.of(cotizacion));
        when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(i -> i.getArgument(0));

        PortalCotizacionDTO dto = portalService.rechazarCotizacion(10, 55,
                PortalCotizacionRechazoDTO.builder().accion("OBSERVAR").motivo("Ajustar flete").build());

        assertEquals("OBSERVADA", dto.getEstado());
        assertTrue(dto.getMotivoObservacion().contains("Ajustar flete"));
    }

    @Test
    void listarObligacionesPendientes_soloMuestraCotizacionTrasAceptacionDelCliente() {
        when(clienteRepository.existsById(10)).thenReturn(true);
        when(cotizacionRepository.findByCliente_IdCliente(10)).thenReturn(List.of(cotizacion));
        when(expedienteRepository.findByCliente_IdCliente(10)).thenReturn(List.of());
        when(liquidacionFinancieraRepository.findByExpediente_Cliente_IdCliente(10)).thenReturn(List.of());

        List<PortalObligacionPagoDTO> antes = portalService.listarObligacionesPendientes(10);
        assertTrue(antes.isEmpty());

        cotizacion.setObservaciones("Aceptada formalmente por el cliente vía Portal B2B el 2026-11-24");
        List<PortalObligacionPagoDTO> despues = portalService.listarObligacionesPendientes(10);
        assertEquals(1, despues.size());
        assertEquals("ANTICIPO_COTIZACION", despues.get(0).getTipoObligacion());
    }

    @Test
    void buscarExpedientePorId_incluyeRegimenAduaneroDeCotizacion() {
        cotizacion.setRegimenAduanero("EXPORTACION_DEFINITIVA");
        cotizacion.setServicioTransporte(true);
        Expediente exp = Expediente.builder()
                .idExpediente(88)
                .codigoExpediente("EXP-2026-0088")
                .cliente(cliente)
                .cotizacion(cotizacion)
                .estadoExpediente(EstadoExpediente.ASIGNADO)
                .build();

        when(expedienteRepository.findById(88)).thenReturn(Optional.of(exp));
        when(controlAlmacenRepository.findByExpediente_IdExpediente(88)).thenReturn(Optional.empty());

        PortalExpedienteDTO res = portalService.buscarExpedientePorId(10, 88);
        assertEquals("EXPORTACION_DEFINITIVA", res.getRegimenAduanero());
        assertEquals(Boolean.TRUE, res.getServicioTransporte());
        assertEquals(Boolean.FALSE, res.getTieneTransporte());
    }

    @Test
    void buscarExpedientePorId_incluyeSolicitudTransporteExternoCuandoFueRegistrada() {
        cotizacion.setServicioTransporte(true);
        Expediente exp = Expediente.builder()
                .idExpediente(89)
                .codigoExpediente("EXP-2026-0089")
                .cliente(cliente)
                .cotizacion(cotizacion)
                .estadoExpediente(EstadoExpediente.LEVANTE_AUTORIZADO)
                .transporteSolicitado(true)
                .transporteTipoTraslado("ENTREGA_IMPORTACION")
                .transporteDireccionPartida("DP World Callao")
                .transporteDireccionLlegada("Av. Industrial 500")
                .transporteDistritoLlegada("Ate")
                .build();

        when(expedienteRepository.findById(89)).thenReturn(Optional.of(exp));
        when(controlAlmacenRepository.findByExpediente_IdExpediente(89)).thenReturn(Optional.empty());

        PortalExpedienteDTO res = portalService.buscarExpedientePorId(10, 89);
        assertEquals(Boolean.TRUE, res.getTieneTransporte());
        assertEquals("SOLICITADO", res.getEstadoTransporte());
        assertEquals("Av. Industrial 500", res.getDireccionLlegada());
        assertEquals("Ate", res.getDistritoLlegada());
    }

    @Test
    void crearExpedienteSimple_creaExpedientePendienteDeAsignacionDeAgente() {
        cotizacion.setEstadoPago("ADELANTO_APROBADO");
        when(clienteRepository.existsById(10)).thenReturn(true);
        when(clienteRepository.findById(10)).thenReturn(Optional.of(cliente));
        when(cotizacionRepository.findById(55)).thenReturn(Optional.of(cotizacion));
        when(expedienteRepository.existsByCotizacion_IdCotizacion(55)).thenReturn(false);
        when(expedienteRepository.count()).thenReturn(4L);
        when(expedienteRepository.existsByCodigoExpediente(any())).thenReturn(false);
        when(expedienteRepository.save(any(Expediente.class))).thenAnswer(i -> {
            Expediente ex = i.getArgument(0);
            ex.setIdExpediente(99);
            return ex;
        });
        when(controlAlmacenRepository.findByExpediente_IdExpediente(99)).thenReturn(Optional.empty());

        PortalExpedienteSimpleDTO req = PortalExpedienteSimpleDTO.builder()
                .idCotizacion(55)
                .contactoExteriorNombre("Shenzhen Mobile Export")
                .contactoExteriorTelefono("+86 755 123456")
                .contactoExteriorCorreo("sales@shenzhen.cn")
                .build();

        PortalExpedienteDTO res = portalService.crearExpedienteSimple(10, req, 20);

        assertEquals(99, res.getIdExpediente());
        assertTrue(res.getPendienteAsignacionAgente());
        assertEquals("Shenzhen Mobile Export", res.getContactoExteriorNombre());
        verify(notificacionClienteRepository, times(1)).save(any(NotificacionCliente.class));
    }
}
