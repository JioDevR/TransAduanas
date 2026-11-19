package pe.sga.repository;

import org.junit.jupiter.api.Test;
import pe.sga.entity.Conductor;
import pe.sga.entity.UnidadTransporte;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class TransporteEntidadesTest {

    @Test
    void testCrearUnidadYConductorConObservaciones() {
        UnidadTransporte u = UnidadTransporte.builder()
                .placaTracto("V9Z-999")
                .marcaModelo("Volvo FH 500")
                .anoFabricacion(2023)
                .tipoVehiculo("TRACTO")
                .capacidadCargaKg(new BigDecimal("32000.00"))
                .estadoOperativo("DISPONIBLE")
                .kilometrajeActual(50000)
                .soatVencimiento(LocalDate.now().plusYears(1))
                .citvVencimiento(LocalDate.now().plusYears(1))
                .observaciones("Nota de prueba técnica")
                .estadoActivo(true)
                .build();
        assertEquals("Nota de prueba técnica", u.getObservaciones());

        Conductor c = Conductor.builder()
                .nombres("Juan")
                .apellidos("Perez")
                .numeroDocumento("44556677")
                .numeroLicencia("Q44556677")
                .tipoLicencia("A-IIIc")
                .fechaVencimientoLicencia(LocalDate.now().plusYears(2))
                .estadoOperativo("DISPONIBLE")
                .observaciones("Chofer certificado para materiales peligrosos")
                .estadoActivo(true)
                .build();
        assertEquals("Chofer certificado para materiales peligrosos", c.getObservaciones());
    }
}
