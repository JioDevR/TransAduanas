package pe.sga.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.sga.dto.request.LiquidacionFinancieraRequestDTO;
import pe.sga.dto.request.LiquidacionPagoDTO;
import pe.sga.dto.response.FinanzasMetricasResponseDTO;
import pe.sga.dto.response.LiquidacionEstimadoResponseDTO;
import pe.sga.dto.response.LiquidacionFinancieraResponseDTO;
import pe.sga.entity.Cliente;
import pe.sga.entity.ControlAlmacen;
import pe.sga.entity.Cotizacion;
import pe.sga.entity.Expediente;
import pe.sga.entity.LiquidacionFinanciera;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.ControlAlmacenRepository;
import pe.sga.repository.ExpedienteRepository;
import pe.sga.repository.IDocumentoAdjuntoRepository;
import pe.sga.repository.LiquidacionFinancieraRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.impl.LiquidacionFinancieraServiceImpl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - LiquidacionFinancieraServiceImpl")
@Tag("unitaria")
public class LiquidacionFinancieraServiceTest {

    @Mock
    private LiquidacionFinancieraRepository liquidacionRepo;

    @Mock
    private ExpedienteRepository expedienteRepo;

    @Mock
    private ControlAlmacenRepository controlAlmacenRepo;

    @Mock
    private UsuarioRepository usuarioRepo;

    @Mock
    private IDocumentoAdjuntoRepository documentoRepo;

    @InjectMocks
    private LiquidacionFinancieraServiceImpl liquidacionService;

    @Test
    @DisplayName("Normal: calcularEstimado utiliza costoTransporteInterno y no flete internacional")
    void calcularEstimado_utilizaCostoTransporteInternoYNoFleteInternacional() {
        Cotizacion cot = Cotizacion.builder()
                .idCotizacion(50)
                .costoTransporteInterno(new BigDecimal("350.00")) // Flete local: 350 USD
                .montoFleteEstimado(new BigDecimal("4200.00"))   // Flete internacional: 4200 USD
                .montoHonorariosEstimado(new BigDecimal("200.00"))
                .montoTributosEstimado(new BigDecimal("1000.00"))
                .build();

        Expediente exp = Expediente.builder()
                .idExpediente(1)
                .codigoExpediente("EXP-2026-0001")
                .cotizacion(cot)
                .cliente(Cliente.builder().idCliente(10).razonSocial("Test SAC").build())
                .build();

        when(expedienteRepo.findById(1)).thenReturn(Optional.of(exp));
        when(controlAlmacenRepo.findByExpediente_IdExpediente(1)).thenReturn(Optional.empty());

        LiquidacionEstimadoResponseDTO estimado = liquidacionService.calcularEstimado(1);

        assertNotNull(estimado);
        // TC es 3.7500. Flete local: 350 * 3.7500 = 1312.50
        // Si usara flete internacional: 4200 * 3.7500 = 15750.00
        assertEquals(new BigDecimal("1312.50"), estimado.getFleteTransporteLocalEstimado());
    }

    @Test
    @DisplayName("Límite: calcularEstimado suma recargo de almacén (+250) solo cuando cantidadBultos > 50")
    void calcularEstimado_limiteBultosAlmacen_aplicaRecargoSoloSobre50() {
        Expediente exp = Expediente.builder().idExpediente(2).codigoExpediente("EXP-2026-0002").build();
        when(expedienteRepo.findById(2)).thenReturn(Optional.of(exp));

        when(controlAlmacenRepo.findByExpediente_IdExpediente(2))
                .thenReturn(Optional.of(ControlAlmacen.builder().cantidadBultos(50).build()));
        LiquidacionEstimadoResponseDTO sinRecargo = liquidacionService.calcularEstimado(2);
        assertEquals(new BigDecimal("450.00"), sinRecargo.getGastosAlmacenPuertoEstimado());

        when(controlAlmacenRepo.findByExpediente_IdExpediente(2))
                .thenReturn(Optional.of(ControlAlmacen.builder().cantidadBultos(51).build()));
        LiquidacionEstimadoResponseDTO conRecargo = liquidacionService.calcularEstimado(2);
        assertEquals(new BigDecimal("700.00"), conRecargo.getGastosAlmacenPuertoEstimado());
    }

    @Test
    @DisplayName("Límite / Nulo: emitir bajo umbral SPOT (<700 PEN) deja detracción en 0 y tipoCambio<=0 usa 3.7500")
    void emitir_bajoUmbralSpotYTipoCambioCero_calculaSinDetraccionYUsaFallbackTC() {
        Expediente exp = Expediente.builder().idExpediente(3).codigoExpediente("EXP-2026-0003").build();
        when(expedienteRepo.findById(3)).thenReturn(Optional.of(exp));
        when(liquidacionRepo.existsByExpediente_IdExpediente(3)).thenReturn(false);
        when(liquidacionRepo.count()).thenReturn(0L);
        when(liquidacionRepo.existsByCodigoLiquidacion(anyString())).thenReturn(false);
        when(liquidacionRepo.save(any(LiquidacionFinanciera.class))).thenAnswer(inv -> inv.getArgument(0));

        LiquidacionFinancieraRequestDTO dto = LiquidacionFinancieraRequestDTO.builder()
                .idExpediente(3)
                .totalTributosSunat(BigDecimal.ZERO)
                .gastosAlmacenPuerto(new BigDecimal("100.00"))
                .fleteTransporteLocal(BigDecimal.ZERO)
                .honorariosAgencia(new BigDecimal("200.00"))
                .tipoCambio(BigDecimal.ZERO)
                .aplicaDetraccion(true)
                .build();

        LiquidacionFinancieraResponseDTO res = liquidacionService.emitir(dto, null);

        // subtotal = 300.00, igv = 54.00, totalPen = 354.00 (< 700 -> detraccion = 0)
        assertEquals(new BigDecimal("300.00"), res.getSubtotalImponible());
        assertEquals(new BigDecimal("54.00"), res.getIgvMonto());
        assertEquals(new BigDecimal("354.00"), res.getTotalGeneralPen());
        assertEquals(BigDecimal.ZERO, res.getMontoDetraccion());
        assertEquals(new BigDecimal("3.7500"), res.getTipoCambio());
    }

    @Test
    @DisplayName("Excepción: emitir duplicada o con expediente inexistente lanza BadRequestException / ResourceNotFoundException")
    void emitir_duplicadaOExpedienteInexistente_lanzaExcepcion() {
        when(expedienteRepo.findById(404)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> liquidacionService.emitir(LiquidacionFinancieraRequestDTO.builder().idExpediente(404).build()));

        Expediente exp = Expediente.builder().idExpediente(4).build();
        when(expedienteRepo.findById(4)).thenReturn(Optional.of(exp));
        when(liquidacionRepo.existsByExpediente_IdExpediente(4)).thenReturn(true);
        assertThrows(BadRequestException.class,
                () -> liquidacionService.emitir(LiquidacionFinancieraRequestDTO.builder().idExpediente(4).build()));
    }

    @Test
    @DisplayName("Excepción: registrarPago en liquidación ya PAGADA o ANULADA lanza BadRequestException")
    void registrarPago_estadoPagadoOAnulado_lanzaBadRequest() {
        LiquidacionFinanciera pagada = LiquidacionFinanciera.builder().idLiquidacion(10).estadoPago("PAGADO").build();
        LiquidacionFinanciera anulada = LiquidacionFinanciera.builder().idLiquidacion(11).estadoPago("ANULADO").build();

        when(liquidacionRepo.findById(10)).thenReturn(Optional.of(pagada));
        when(liquidacionRepo.findById(11)).thenReturn(Optional.of(anulada));

        LiquidacionPagoDTO pagoDTO = LiquidacionPagoDTO.builder().metodoPago("TRANSFERENCIA").numeroOperacion("OP-1").build();

        assertThrows(BadRequestException.class, () -> liquidacionService.registrarPago(10, pagoDTO));
        assertThrows(BadRequestException.class, () -> liquidacionService.registrarPago(11, pagoDTO));
    }

    @Test
    @DisplayName("Normal: obtenerMetricas agrupa correctamente montos de liquidadas, pendientes y anuladas")
    void obtenerMetricas_calculaTotalesPorEstado() {
        LiquidacionFinanciera pagada = LiquidacionFinanciera.builder()
                .estadoPago("PAGADO")
                .totalGeneralPen(new BigDecimal("1000.00"))
                .totalGeneralUsd(new BigDecimal("266.67"))
                .totalTributosSunat(new BigDecimal("400.00"))
                .montoDetraccion(new BigDecimal("72.00"))
                .build();
        LiquidacionFinanciera pendiente = LiquidacionFinanciera.builder()
                .estadoPago("PENDIENTE")
                .totalGeneralPen(new BigDecimal("500.00"))
                .totalGeneralUsd(new BigDecimal("133.33"))
                .totalTributosSunat(new BigDecimal("100.00"))
                .montoDetraccion(BigDecimal.ZERO)
                .build();
        LiquidacionFinanciera anulada = LiquidacionFinanciera.builder()
                .estadoPago("ANULADO")
                .totalGeneralPen(new BigDecimal("999.00"))
                .totalGeneralUsd(new BigDecimal("250.00"))
                .totalTributosSunat(new BigDecimal("300.00"))
                .montoDetraccion(new BigDecimal("50.00"))
                .build();

        when(liquidacionRepo.findAll()).thenReturn(List.of(pagada, pendiente, anulada));

        FinanzasMetricasResponseDTO metricas = liquidacionService.obtenerMetricas();

        assertEquals(new BigDecimal("1000.00"), metricas.getTotalCobradoPen());
        assertEquals(new BigDecimal("500.00"), metricas.getCuentasPorCobrarPen());
        assertEquals(new BigDecimal("500.00"), metricas.getTotalTributosSunatPen());
        assertEquals(new BigDecimal("72.00"), metricas.getTotalDetraccionesPen());
        assertEquals(1L, metricas.getLiquidacionesPagadas());
        assertEquals(1L, metricas.getLiquidacionesPendientes());
        assertEquals(1L, metricas.getLiquidacionesAnuladas());
    }
}
