package pe.sga.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import pe.sga.dto.request.ConductorRequestDTO;
import pe.sga.dto.request.DespachoEntregaDTO;
import pe.sga.dto.request.DespachoIncidenciaDTO;
import pe.sga.dto.request.UnidadTransporteRequestDTO;
import pe.sga.dto.response.ConductorResponseDTO;
import pe.sga.dto.response.UnidadTransporteResponseDTO;
import pe.sga.entity.enums.CategoriaLicenciaMtc;
import pe.sga.entity.enums.EstadoOperativoConductor;
import pe.sga.entity.enums.EstadoOperativoFlota;
import pe.sga.entity.enums.TipoVehiculo;

import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@Tag("unitaria")
public class TransporteDTOTest {

    private ObjectMapper objectMapper;
    private Validator validator;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void testSerializacionYDeserializacionCanonicaUnidad() throws Exception {
        String json = """
        {
            "placaTracto": "ABC-123",
            "tipoVehiculo": "TRACTO",
            "marcaModelo": "VOLVO FH 500",
            "anoFabricacion": 2022,
            "kilometrajeActual": 120000,
            "soatVencimiento": "2027-05-10",
            "citvVencimiento": "2027-06-15",
            "estadoOperativo": "DISPONIBLE",
            "observaciones": "Unidad con GPS satelital"
        }
        """;
        UnidadTransporteRequestDTO dto = objectMapper.readValue(json, UnidadTransporteRequestDTO.class);
        assertEquals("ABC-123", dto.getPlacaTracto());
        assertEquals(TipoVehiculo.TRACTO, dto.getTipoVehiculo());
        assertEquals(2022, dto.getAnoFabricacion());
        assertEquals(LocalDate.of(2027, 5, 10), dto.getSoatVencimiento());
        assertEquals(LocalDate.of(2027, 6, 15), dto.getCitvVencimiento());
        assertEquals(EstadoOperativoFlota.DISPONIBLE, dto.getEstadoOperativo());
        assertEquals("Unidad con GPS satelital", dto.getObservaciones());

        UnidadTransporteResponseDTO resp = UnidadTransporteResponseDTO.builder()
                .anoFabricacion(2022)
                .tipoVehiculo(TipoVehiculo.FURGON)
                .soatVencimiento(LocalDate.of(2027, 5, 10))
                .citvVencimiento(LocalDate.of(2027, 6, 15))
                .observaciones("Nota prueba")
                .build();
        String respJson = objectMapper.writeValueAsString(resp);
        assertTrue(respJson.contains("\"tipoVehiculo\":\"FURGON\""));
        assertFalse(respJson.contains("fechaVencimientoSoat"));
    }

    @Test
    void testSerializacionYDeserializacionCanonicaConductor() throws Exception {
        String json = """
        {
            "nombres": "Carlos",
            "apellidos": "Santillan",
            "numeroDocumento": "09876543",
            "numeroLicencia": "Q09876543",
            "tipoLicencia": "A-IIIc",
            "fechaVencimientoLicencia": "2028-01-20",
            "estadoOperativo": "DISPONIBLE",
            "observaciones": "Experiencia en carga refrigerada"
        }
        """;
        ConductorRequestDTO dto = objectMapper.readValue(json, ConductorRequestDTO.class);
        assertEquals("Carlos", dto.getNombres());
        assertEquals(CategoriaLicenciaMtc.A_IIIC, dto.getTipoLicencia());
        assertEquals(EstadoOperativoConductor.DISPONIBLE, dto.getEstadoOperativo());
        assertEquals(LocalDate.of(2028, 1, 20), dto.getFechaVencimientoLicencia());

        ConductorResponseDTO resp = ConductorResponseDTO.builder()
                .tipoLicencia(CategoriaLicenciaMtc.A_IIIC)
                .observaciones("Chofer Senior")
                .build();
        String respJson = objectMapper.writeValueAsString(resp);
        assertTrue(respJson.contains("\"tipoLicencia\":\"A-IIIc\""));
        assertFalse(respJson.contains("categoriaLicencia"));
    }

    @Test
    void testDespachoEntregaYValidaciones() throws Exception {
        String json = """
        {
            "receptorNombre": "Juan Perez",
            "receptorDni": "12345678",
            "kilometrajeLlegada": 145890
        }
        """;
        DespachoEntregaDTO dto = objectMapper.readValue(json, DespachoEntregaDTO.class);
        assertEquals("Juan Perez", dto.getReceptorNombre());
        assertEquals(145890, dto.getKilometrajeLlegada());

        DespachoEntregaDTO invalidKm = DespachoEntregaDTO.builder().kilometrajeLlegada(-10).build();
        Set<ConstraintViolation<DespachoEntregaDTO>> violations = validator.validate(invalidKm);
        assertFalse(violations.isEmpty());

        DespachoIncidenciaDTO emptyInc = DespachoIncidenciaDTO.builder().incidencia("   ").build();
        assertFalse(validator.validate(emptyInc).isEmpty());
    }
}
