package pe.sga.repository;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import pe.sga.entity.Conductor;
import pe.sga.entity.DespachoTransporte;
import pe.sga.entity.UnidadTransporte;
import pe.sga.entity.enums.CategoriaLicenciaMtc;
import pe.sga.entity.enums.CategoriaLicenciaMtcConverter;
import pe.sga.entity.enums.EstadoOperativoConductor;
import pe.sga.entity.enums.EstadoOperativoFlota;
import pe.sga.entity.enums.EstadoTransporte;
import pe.sga.entity.enums.TipoTraslado;
import pe.sga.entity.enums.TipoVehiculo;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unitaria")
public class TransporteEntidadesTest {

    @Test
    void testDefaultsYEnumsDeEntidadesTransporte() {
        UnidadTransporte u = UnidadTransporte.builder()
                .placaTracto("V9Z-999")
                .marcaModelo("Volvo FH 500")
                .anoFabricacion(2023)
                .capacidadCargaKg(new BigDecimal("32000.00"))
                .kilometrajeActual(50000)
                .soatVencimiento(LocalDate.now().plusYears(1))
                .citvVencimiento(LocalDate.now().plusYears(1))
                .observaciones("Nota de prueba técnica")
                .build();
        assertEquals(TipoVehiculo.TRACTO, u.getTipoVehiculo());
        assertEquals(EstadoOperativoFlota.DISPONIBLE, u.getEstadoOperativo());
        assertTrue(u.getEstadoActivo());
        assertEquals("Nota de prueba técnica", u.getObservaciones());

        Conductor c = Conductor.builder()
                .nombres("Juan")
                .apellidos("Perez")
                .numeroDocumento("44556677")
                .numeroLicencia("Q44556677")
                .fechaVencimientoLicencia(LocalDate.now().plusYears(2))
                .observaciones("Chofer certificado para materiales peligrosos")
                .build();
        assertEquals(CategoriaLicenciaMtc.A_IIIC, c.getTipoLicencia());
        assertEquals(EstadoOperativoConductor.DISPONIBLE, c.getEstadoOperativo());
        assertTrue(c.getEstadoActivo());

        DespachoTransporte d = DespachoTransporte.builder()
                .codigoTracking("TRK-2026-0099")
                .build();
        assertEquals(EstadoTransporte.PENDIENTE_ASIGNACION, d.getEstadoTransporte());
        assertEquals(TipoTraslado.ENTREGA_IMPORTACION, d.getTipoTraslado());
    }

    @Test
    void testCategoriaLicenciaMtcConverter() {
        CategoriaLicenciaMtcConverter converter = new CategoriaLicenciaMtcConverter();
        assertEquals("A-IIIc", converter.convertToDatabaseColumn(CategoriaLicenciaMtc.A_IIIC));
        assertEquals("A-IIIb", converter.convertToDatabaseColumn(CategoriaLicenciaMtc.A_IIIB));
        assertEquals(CategoriaLicenciaMtc.A_IIIC, converter.convertToEntityAttribute("A-IIIc"));
        assertEquals(CategoriaLicenciaMtc.A_IV, converter.convertToEntityAttribute("A-IV"));
        assertNull(converter.convertToDatabaseColumn(null));
        assertNull(converter.convertToEntityAttribute(null));
    }
}
