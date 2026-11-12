package pe.sga.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.sga.config.JwtTokenProvider;
import pe.sga.dto.portal.PortalCotizacionDTO;
import pe.sga.dto.portal.PortalRegistroVoucherDTO;
import pe.sga.entity.Cliente;
import pe.sga.entity.Cotizacion;
import pe.sga.entity.CotizacionPago;
import pe.sga.entity.Usuario;
import pe.sga.entity.enums.EstadoCotizacion;
import pe.sga.entity.enums.RolUsuario;
import pe.sga.repository.*;
import pe.sga.service.impl.PortalServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PortalServiceImplTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioClienteRepository usuarioClienteRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private CotizacionRepository cotizacionRepository;
    @Mock private ExpedienteRepository expedienteRepository;
    @Mock private CotizacionPagoRepository cotizacionPagoRepository;
    @Mock private LiquidacionFinancieraRepository liquidacionFinancieraRepository;
    @Mock private ControlAlmacenRepository controlAlmacenRepository;
    @Mock private DespachoTransporteRepository despachoTransporteRepository;
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
}
