package pe.sga.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.sga.dto.response.LiquidacionEstimadoResponseDTO;
import pe.sga.entity.Cliente;
import pe.sga.entity.Cotizacion;
import pe.sga.entity.Expediente;
import pe.sga.repository.ControlAlmacenRepository;
import pe.sga.repository.ExpedienteRepository;
import pe.sga.repository.LiquidacionFinancieraRepository;
import pe.sga.service.impl.LiquidacionFinancieraServiceImpl;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class LiquidacionFinancieraServiceTest {

    @Mock
    private LiquidacionFinancieraRepository liquidacionRepo;

    @Mock
    private ExpedienteRepository expedienteRepo;

    @Mock
    private ControlAlmacenRepository controlAlmacenRepo;

    @InjectMocks
    private LiquidacionFinancieraServiceImpl liquidacionService;

    @Test
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
}
