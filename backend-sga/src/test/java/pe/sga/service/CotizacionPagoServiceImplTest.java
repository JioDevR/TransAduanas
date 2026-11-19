package pe.sga.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import pe.sga.dto.request.CotizacionPagoRegistrarDTO;
import pe.sga.dto.request.CotizacionPagoValidarDTO;
import pe.sga.dto.response.CotizacionPagoResponseDTO;
import pe.sga.entity.Cliente;
import pe.sga.entity.Cotizacion;
import pe.sga.entity.CotizacionPago;
import pe.sga.entity.NotificacionCliente;
import pe.sga.entity.Usuario;
import pe.sga.entity.enums.EstadoCotizacion;
import pe.sga.entity.enums.EstadoPagoCotizacion;
import pe.sga.entity.enums.RolUsuario;
import pe.sga.entity.enums.TipoAbonoCotizacion;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.CotizacionPagoRepository;
import pe.sga.repository.CotizacionRepository;
import pe.sga.repository.IDocumentoAdjuntoRepository;
import pe.sga.repository.NotificacionClienteRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.impl.CotizacionPagoServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - CotizacionPagoServiceImpl")
class CotizacionPagoServiceImplTest {

    @Mock
    private CotizacionPagoRepository cotizacionPagoRepository;
    @Mock
    private CotizacionRepository cotizacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private IDocumentoAdjuntoRepository documentoRepository;
    @Mock
    private NotificacionClienteRepository notificacionClienteRepository;

    @InjectMocks
    private CotizacionPagoServiceImpl pagoService;

    @Test
    @DisplayName("Normal: registrar pago en USD calcula montoEquivalentePen y marca cotización en PENDIENTE_REVISION")
    void registrarPago_enUsdConTipoCambioValido_calculaEquivalenteYGuarda() {
        Usuario asesor = Usuario.builder().idUsuario(10).rol(RolUsuario.AGENTE_COMERCIAL).build();
        Cotizacion cot = Cotizacion.builder()
                .idCotizacion(1)
                .codigoCotizacion("COT-2026-0001")
                .estado(EstadoCotizacion.APROBADA)
                .estadoPago("SIN_PAGO")
                .usuarioComercial(asesor)
                .totalCotizado(new BigDecimal("1000.00"))
                .build();

        CotizacionPagoRegistrarDTO dto = CotizacionPagoRegistrarDTO.builder()
                .idCotizacion(1)
                .tipoAbono(TipoAbonoCotizacion.ADELANTO)
                .montoAbonado(new BigDecimal("400.00"))
                .moneda("usd")
                .tipoCambio(new BigDecimal("3.8000"))
                .metodoPago("TRANSFERENCIA")
                .bancoDestino("BCP")
                .numeroOperacion("OP-12345")
                .fechaOperacion(LocalDate.of(2026, 11, 10))
                .build();

        when(cotizacionRepository.findById(1)).thenReturn(Optional.of(cot));
        when(usuarioRepository.findById(10)).thenReturn(Optional.of(asesor));
        when(cotizacionPagoRepository.findUltimoPagoPorPrefijo(anyString())).thenReturn(Optional.empty());
        when(cotizacionPagoRepository.count()).thenReturn(0L);
        when(cotizacionPagoRepository.existsByCodigoPago(anyString())).thenReturn(false);
        when(cotizacionPagoRepository.save(any(CotizacionPago.class))).thenAnswer(inv -> {
            CotizacionPago p = inv.getArgument(0);
            p.setIdPago(50);
            return p;
        });

        CotizacionPagoResponseDTO res = pagoService.registrarPago(dto, 10);

        assertEquals(50, res.getIdPago());
        assertEquals("USD", res.getMoneda());
        assertEquals(new BigDecimal("1520.00"), res.getMontoEquivalentePen());
        assertEquals(EstadoPagoCotizacion.PENDIENTE_REVISION, res.getEstadoPago());
        assertEquals("PENDIENTE_REVISION", cot.getEstadoPago());
        verify(cotizacionRepository).save(cot);
    }

    @Test
    @DisplayName("Límite / Nulo: tipoCambio en 0 o null usa default 3.7500 y usuarioRegistro nulo recurre al primer usuario")
    void registrarPago_tipoCambioCeroYUsuarioNulo_aplicaFallbacks() {
        Usuario fallbackUser = Usuario.builder().idUsuario(1).rol(RolUsuario.ADMINISTRADOR_TI).build();
        Cotizacion cot = Cotizacion.builder()
                .idCotizacion(2)
                .estado(EstadoCotizacion.APROBADA)
                .estadoPago("ADELANTO_APROBADO")
                .build();

        CotizacionPagoRegistrarDTO dto = CotizacionPagoRegistrarDTO.builder()
                .idCotizacion(2)
                .tipoAbono(TipoAbonoCotizacion.PAGO_TOTAL)
                .montoAbonado(new BigDecimal("100.00"))
                .moneda("USD")
                .tipoCambio(BigDecimal.ZERO)
                .metodoPago("DEPOSITO")
                .bancoDestino("BBVA")
                .numeroOperacion("OP-999")
                .fechaOperacion(LocalDate.now())
                .build();

        when(cotizacionRepository.findById(2)).thenReturn(Optional.of(cot));
        when(usuarioRepository.findFirstByOrderByIdUsuarioAsc()).thenReturn(Optional.of(fallbackUser));
        when(cotizacionPagoRepository.findUltimoPagoPorPrefijo(anyString())).thenReturn(Optional.empty());
        when(cotizacionPagoRepository.count()).thenReturn(0L);
        when(cotizacionPagoRepository.existsByCodigoPago(anyString())).thenReturn(false);
        when(cotizacionPagoRepository.save(any(CotizacionPago.class))).thenAnswer(inv -> inv.getArgument(0));

        CotizacionPagoResponseDTO res = pagoService.registrarPago(dto, null);

        assertEquals(new BigDecimal("3.7500"), res.getTipoCambio());
        assertEquals(new BigDecimal("375.00"), res.getMontoEquivalentePen());
        // No sobreescribe ADELANTO_APROBADO
        assertEquals("ADELANTO_APROBADO", cot.getEstadoPago());
        verify(cotizacionRepository, never()).save(cot);
    }

    @Test
    @DisplayName("Excepción: registrar pago en cotización no APROBADA lanza BadRequestException")
    void registrarPago_cotizacionPendiente_lanzaBadRequest() {
        Cotizacion cot = Cotizacion.builder().idCotizacion(3).estado(EstadoCotizacion.PENDIENTE).build();
        when(cotizacionRepository.findById(3)).thenReturn(Optional.of(cot));

        CotizacionPagoRegistrarDTO dto = CotizacionPagoRegistrarDTO.builder()
                .idCotizacion(3)
                .montoAbonado(new BigDecimal("200.00"))
                .moneda("USD")
                .build();

        assertThrows(BadRequestException.class, () -> pagoService.registrarPago(dto, 1));
        verify(cotizacionPagoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Excepción: agente comercial intenta registrar pago de cotización ajena lanza AccessDeniedException")
    void registrarPago_agenteComercialAjeno_lanzaAccessDenied() {
        Usuario dueno = Usuario.builder().idUsuario(10).rol(RolUsuario.AGENTE_COMERCIAL).build();
        Usuario otroAgente = Usuario.builder().idUsuario(20).rol(RolUsuario.AGENTE_COMERCIAL).build();
        Cotizacion cot = Cotizacion.builder()
                .idCotizacion(4)
                .estado(EstadoCotizacion.APROBADA)
                .usuarioComercial(dueno)
                .build();

        when(cotizacionRepository.findById(4)).thenReturn(Optional.of(cot));
        when(usuarioRepository.findById(20)).thenReturn(Optional.of(otroAgente));

        CotizacionPagoRegistrarDTO dto = CotizacionPagoRegistrarDTO.builder()
                .idCotizacion(4)
                .montoAbonado(new BigDecimal("100.00"))
                .moneda("USD")
                .build();

        assertThrows(AccessDeniedException.class, () -> pagoService.registrarPago(dto, 20));
    }

    @Test
    @DisplayName("Normal / Límite: validarPago APROBAR con sobrepago deja saldoPendiente en 0, estado PAGADO_TOTAL y notifica al cliente")
    void validarPago_aprobarConSobrepago_recalculaACeroYNotifica() {
        Cliente cliente = Cliente.builder().idCliente(12).razonSocial("Andes SAC").build();
        Cotizacion cot = Cotizacion.builder()
                .idCotizacion(5)
                .codigoCotizacion("COT-2026-0005")
                .cliente(cliente)
                .totalCotizado(new BigDecimal("500.00"))
                .build();
        CotizacionPago pago = CotizacionPago.builder()
                .idPago(90)
                .codigoPago("PAG-COT-2026-0001")
                .cotizacion(cot)
                .moneda("USD")
                .montoAbonado(new BigDecimal("600.00"))
                .numeroOperacion("OP-777")
                .estadoPago(EstadoPagoCotizacion.PENDIENTE_REVISION)
                .build();
        Usuario contador = Usuario.builder().idUsuario(3).rol(RolUsuario.CONTADOR).nombres("Ana").apellidos("Ruiz").build();

        when(cotizacionPagoRepository.findById(90)).thenReturn(Optional.of(pago));
        when(usuarioRepository.findById(3)).thenReturn(Optional.of(contador));
        when(cotizacionPagoRepository.findByCotizacion_IdCotizacionOrderByFechaRegistroDesc(5)).thenReturn(List.of(pago));

        CotizacionPagoValidarDTO dto = CotizacionPagoValidarDTO.builder()
                .accion("APROBAR")
                .observacionFinanzas("Voucher conforme")
                .build();

        CotizacionPagoResponseDTO res = pagoService.validarPago(90, dto, 3);

        assertEquals(EstadoPagoCotizacion.APROBADO, res.getEstadoPago());
        assertEquals(BigDecimal.ZERO, cot.getSaldoPendiente());
        assertEquals("PAGADO_TOTAL", cot.getEstadoPago());

        ArgumentCaptor<NotificacionCliente> notifCaptor = ArgumentCaptor.forClass(NotificacionCliente.class);
        verify(notificacionClienteRepository).save(notifCaptor.capture());
        assertTrue(notifCaptor.getValue().getTitulo().contains("Anticipo de Pago Validado"));
    }

    @Test
    @DisplayName("Normal / Nulo: validarPago OBSERVAR sin observacionFinanzas usa motivo por defecto y genera notificación")
    void validarPago_observarSinMotivo_usaMotivoPorDefecto() {
        Cliente cliente = Cliente.builder().idCliente(12).build();
        Cotizacion cot = Cotizacion.builder().idCotizacion(6).cliente(cliente).totalCotizado(new BigDecimal("800.00")).build();
        CotizacionPago pago = CotizacionPago.builder()
                .idPago(91)
                .codigoPago("PAG-COT-2026-0002")
                .cotizacion(cot)
                .moneda("PEN")
                .montoAbonado(new BigDecimal("375.00"))
                .numeroOperacion("OP-888")
                .estadoPago(EstadoPagoCotizacion.PENDIENTE_REVISION)
                .build();
        Usuario contador = Usuario.builder().idUsuario(3).rol(RolUsuario.CONTADOR).build();

        when(cotizacionPagoRepository.findById(91)).thenReturn(Optional.of(pago));
        when(usuarioRepository.findById(3)).thenReturn(Optional.of(contador));
        when(cotizacionPagoRepository.findByCotizacion_IdCotizacionOrderByFechaRegistroDesc(6)).thenReturn(List.of(pago));

        pagoService.validarPago(91, CotizacionPagoValidarDTO.builder().accion("OBSERVAR").observacionFinanzas(null).build(), 3);

        assertEquals(EstadoPagoCotizacion.OBSERVADO, pago.getEstadoPago());
        assertEquals("OBSERVADO", cot.getEstadoPago());
        ArgumentCaptor<NotificacionCliente> notifCaptor = ArgumentCaptor.forClass(NotificacionCliente.class);
        verify(notificacionClienteRepository).save(notifCaptor.capture());
        assertTrue(notifCaptor.getValue().getMensaje().contains("extractos bancarios"));
    }

    @Test
    @DisplayName("Excepción: validarPago con acción inválida lanza BadRequestException y pago inexistente lanza ResourceNotFoundException")
    void validarPago_accionInvalidaOPagoInexistente_lanzanExcepciones() {
        when(cotizacionPagoRepository.findById(404)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> pagoService.validarPago(404, CotizacionPagoValidarDTO.builder().accion("APROBAR").build(), 1));

        CotizacionPago pago = CotizacionPago.builder().idPago(92).build();
        Usuario contador = Usuario.builder().idUsuario(3).rol(RolUsuario.CONTADOR).build();
        when(cotizacionPagoRepository.findById(92)).thenReturn(Optional.of(pago));
        when(usuarioRepository.findById(3)).thenReturn(Optional.of(contador));

        assertThrows(BadRequestException.class,
                () -> pagoService.validarPago(92, CotizacionPagoValidarDTO.builder().accion("INVALIDA").build(), 3));
    }
}
