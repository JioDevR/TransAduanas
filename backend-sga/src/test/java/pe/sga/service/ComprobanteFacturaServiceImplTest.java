package pe.sga.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import pe.sga.dto.response.ComprobanteFacturaResponseDTO;
import pe.sga.entity.Cliente;
import pe.sga.entity.ComprobanteFactura;
import pe.sga.entity.Cotizacion;
import pe.sga.entity.CotizacionPago;
import pe.sga.entity.Usuario;
import pe.sga.entity.enums.EstadoPagoCotizacion;
import pe.sga.entity.enums.TipoAbonoCotizacion;
import pe.sga.repository.ComprobanteFacturaRepository;
import pe.sga.repository.CotizacionPagoRepository;
import pe.sga.repository.LiquidacionFinancieraRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.impl.ComprobanteFacturaServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - ComprobanteFacturaServiceImpl")
@Tag("unitaria")
class ComprobanteFacturaServiceImplTest {

    @Mock
    private ComprobanteFacturaRepository facturaRepository;
    @Mock
    private CotizacionPagoRepository cotizacionPagoRepository;
    @Mock
    private LiquidacionFinancieraRepository liquidacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ComprobanteFacturaServiceImpl facturaService;

    @Test
    @DisplayName("Emite automáticamente FACTURA F001 para cliente con RUC y desglosa base imponible e IGV 18%")
    void emitirAutomaticoPorAbono_clienteConRuc_generaFacturaConIgv() {
        Cliente cliente = Cliente.builder()
                .idCliente(5)
                .tipoDocumento("RUC")
                .numeroDocumento("20555666777")
                .razonSocial("Importadora Pacífico S.A.C.")
                .direccionFiscal("Av. Argentina 1200, Callao")
                .build();

        Cotizacion cot = Cotizacion.builder()
                .idCotizacion(12)
                .codigoCotizacion("COT-2026-0012")
                .cliente(cliente)
                .build();

        CotizacionPago pago = CotizacionPago.builder()
                .idPago(30)
                .codigoPago("PAG-COT-2026-0030")
                .cotizacion(cot)
                .tipoAbono(TipoAbonoCotizacion.ADELANTO)
                .montoAbonado(new BigDecimal("1180.00"))
                .moneda("USD")
                .tipoCambio(new BigDecimal("3.7500"))
                .montoEquivalentePen(new BigDecimal("4425.00"))
                .bancoDestino("BCP USD")
                .numeroOperacion("OP-778899")
                .fechaOperacion(LocalDate.of(2026, 11, 24))
                .estadoPago(EstadoPagoCotizacion.APROBADO)
                .build();

        Usuario emisor = Usuario.builder().idUsuario(2).nombres("Ana").apellidos("Contador").build();

        when(facturaRepository.findFirstByPagoCotizacion_IdPagoAndEstadoOrderByIdComprobanteDesc(30, "EMITIDA"))
                .thenReturn(Optional.empty());
        when(facturaRepository.findMaxCorrelativoBySerie("F001")).thenReturn(4);
        when(facturaRepository.existsByNumeroCompleto("F001-000005")).thenReturn(false);
        when(facturaRepository.save(any(ComprobanteFactura.class))).thenAnswer(inv -> {
            ComprobanteFactura cf = inv.getArgument(0);
            cf.setIdComprobante(101);
            return cf;
        });

        ComprobanteFacturaResponseDTO res = facturaService.emitirAutomaticoPorAbono(pago, emisor);

        assertEquals(101, res.getIdComprobante());
        assertEquals("FACTURA", res.getTipoComprobante());
        assertEquals("F001-000005", res.getNumeroCompleto());
        assertEquals(new BigDecimal("1000.00"), res.getSubtotalGravado());
        assertEquals(new BigDecimal("180.00"), res.getIgvMonto());
        assertEquals(new BigDecimal("1180.00"), res.getTotalComprobante());
        assertEquals(new BigDecimal("4425.00"), res.getTotalEquivalentePen());
        assertEquals("EMITIDA", res.getEstado());
    }

    @Test
    @DisplayName("Es idempotente si ya existe una factura EMITIDA para el abono")
    void emitirAutomaticoPorAbono_yaEmitida_retornaExistenteSinDuplicar() {
        Cliente cliente = Cliente.builder().idCliente(5).tipoDocumento("RUC").numeroDocumento("20555666777").razonSocial("Cli").build();
        CotizacionPago pago = CotizacionPago.builder().idPago(30).codigoPago("PAG-1").build();
        ComprobanteFactura existente = ComprobanteFactura.builder()
                .idComprobante(88)
                .tipoComprobante("FACTURA")
                .serie("F001")
                .correlativo(1)
                .numeroCompleto("F001-000001")
                .origenTipo("ABONO_COTIZACION")
                .pagoCotizacion(pago)
                .cliente(cliente)
                .clienteRazonSocial("Cli")
                .clienteNumeroDocumento("20555666777")
                .codigoReferencia("COT-1 / PAG-1")
                .concepto("Anticipo")
                .estado("EMITIDA")
                .build();

        when(facturaRepository.findFirstByPagoCotizacion_IdPagoAndEstadoOrderByIdComprobanteDesc(30, "EMITIDA"))
                .thenReturn(Optional.of(existente));

        ComprobanteFacturaResponseDTO res = facturaService.emitirAutomaticoPorAbono(pago, null);

        assertEquals(88, res.getIdComprobante());
        assertEquals("F001-000001", res.getNumeroCompleto());
        verify(facturaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Bloquea acceso en Portal B2B cuando el comprobante pertenece a otro cliente")
    void buscarPorIdParaCliente_otroCliente_lanzaAccessDenied() {
        Cliente otro = Cliente.builder().idCliente(99).build();
        ComprobanteFactura cf = ComprobanteFactura.builder().idComprobante(7).cliente(otro).build();
        when(facturaRepository.findById(7)).thenReturn(Optional.of(cf));

        assertThrows(AccessDeniedException.class, () -> facturaService.buscarPorIdParaCliente(5, 7));
    }
}
