package pe.sga.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pe.sga.dto.request.ConductorRequestDTO;
import pe.sga.dto.request.DespachoEntregaDTO;
import pe.sga.dto.request.UnidadTransporteRequestDTO;
import pe.sga.dto.response.ConductorResponseDTO;
import pe.sga.dto.response.UnidadTransporteResponseDTO;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

public class TransporteDTOTest {

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void testDeserializacionUnidadConNombresFrontend() throws Exception {
        String json = """
        {
            "placaTracto": "ABC-123",
            "tipoVehiculo": "TRACTO",
            "marcaModelo": "VOLVO FH 500",
            "anioFabricacion": 2022,
            "kilometrajeActual": 120000,
            "fechaVencimientoSoat": "2027-05-10",
            "fechaVencimientoCitv": "2027-06-15",
            "observaciones": "Unidad con GPS satelital"
        }
        """;
        UnidadTransporteRequestDTO dto = objectMapper.readValue(json, UnidadTransporteRequestDTO.class);
        assertEquals("ABC-123", dto.getPlacaTracto());
        assertEquals(2022, dto.getAnoFabricacion());
        assertEquals(LocalDate.of(2027, 5, 10), dto.getSoatVencimiento());
        assertEquals(LocalDate.of(2027, 6, 15), dto.getCitvVencimiento());
        assertEquals("Unidad con GPS satelital", dto.getObservaciones());

        UnidadTransporteResponseDTO resp = UnidadTransporteResponseDTO.builder()
                .anoFabricacion(2022)
                .soatVencimiento(LocalDate.of(2027, 5, 10))
                .citvVencimiento(LocalDate.of(2027, 6, 15))
                .observaciones("Nota prueba")
                .build();
        assertEquals(2022, resp.getAnioFabricacion());
        assertEquals(LocalDate.of(2027, 5, 10), resp.getFechaVencimientoSoat());
        assertEquals(LocalDate.of(2027, 6, 15), resp.getFechaVencimientoCitv());
        assertEquals("Nota prueba", resp.getObservaciones());
    }

    @Test
    void testDeserializacionConductorConNombresFrontend() throws Exception {
        String json = """
        {
            "nombres": "Carlos",
            "apellidos": "Santillan",
            "numeroDocumento": "09876543",
            "numeroLicencia": "Q09876543",
            "categoriaLicencia": "A-IIIc",
            "fechaVencimientoLicencia": "2028-01-20",
            "observaciones": "Experiencia en carga refrigerada"
        }
        """;
        ConductorRequestDTO dto = objectMapper.readValue(json, ConductorRequestDTO.class);
        assertEquals("Carlos", dto.getNombres());
        assertEquals("A-IIIc", dto.getTipoLicencia());
        assertEquals(LocalDate.of(2028, 1, 20), dto.getFechaVencimientoLicencia());
        assertEquals("Experiencia en carga refrigerada", dto.getObservaciones());

        ConductorResponseDTO resp = ConductorResponseDTO.builder()
                .tipoLicencia("A-IIIc")
                .username("carlos.s")
                .observaciones("Chofer Senior")
                .build();
        assertEquals("A-IIIc", resp.getCategoriaLicencia());
        assertEquals("carlos.s", resp.getUsuarioUsername());
        assertEquals("Chofer Senior", resp.getObservaciones());
    }

    @Test
    void testDespachoEntregaConKilometrajeLlegada() throws Exception {
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
    }
}
