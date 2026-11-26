package pe.sga.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.sga.dto.request.ConductorRequestDTO;
import pe.sga.dto.response.ConductorResponseDTO;
import pe.sga.entity.Conductor;
import pe.sga.entity.enums.CategoriaLicenciaMtc;
import pe.sga.entity.enums.EstadoOperativoConductor;
import pe.sga.exception.BadRequestException;
import pe.sga.repository.ConductorRepository;
import pe.sga.repository.DespachoTransporteRepository;
import pe.sga.service.impl.ConductorServiceImpl;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unitaria")
class ConductorServiceImplTest {

    @Mock
    private ConductorRepository conductorRepo;

    @Mock
    private DespachoTransporteRepository despachoRepo;

    @InjectMocks
    private ConductorServiceImpl service;

    private Conductor conductor;

    @BeforeEach
    void setUp() {
        conductor = Conductor.builder()
                .idConductor(1)
                .nombres("Raul")
                .apellidos("Cardenas")
                .numeroDocumento("60123456")
                .numeroLicencia("Q60123456")
                .tipoLicencia(CategoriaLicenciaMtc.A_IIIC)
                .estadoOperativo(EstadoOperativoConductor.DISPONIBLE)
                .estadoActivo(true)
                .build();
    }

    @Test
    void crear_validaDniYLicenciaUnicos() {
        ConductorRequestDTO dto = ConductorRequestDTO.builder()
                .nombres("Raul")
                .apellidos("Cardenas")
                .numeroDocumento("60123456")
                .numeroLicencia(" q60123456 ")
                .build();

        when(conductorRepo.existsByNumeroLicencia("Q60123456")).thenReturn(false);
        when(conductorRepo.existsByNumeroDocumento("60123456")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.crear(dto));
        verify(conductorRepo, never()).save(any());

        when(conductorRepo.existsByNumeroDocumento("60123456")).thenReturn(false);
        when(conductorRepo.save(any(Conductor.class))).thenAnswer(i -> i.getArgument(0));
        ConductorResponseDTO res = service.crear(dto);
        assertEquals("Q60123456", res.getNumeroLicencia());
    }

    @Test
    void eliminar_fallaSiEstaEnDespachoActivo() {
        when(conductorRepo.findById(1)).thenReturn(Optional.of(conductor));
        when(despachoRepo.existsByConductor_IdConductorAndEstadoTransporteIn(eq(1), any())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.eliminar(1));
        verify(conductorRepo, never()).save(any());
    }

    @Test
    void actualizar_fallaSiCambiaASuspendidoConDespachoActivo() {
        when(conductorRepo.findById(1)).thenReturn(Optional.of(conductor));
        when(despachoRepo.existsByConductor_IdConductorAndEstadoTransporteIn(eq(1), any())).thenReturn(true);

        ConductorRequestDTO dto = ConductorRequestDTO.builder()
                .nombres("Raul")
                .apellidos("Cardenas")
                .numeroLicencia("Q60123456")
                .estadoOperativo(EstadoOperativoConductor.SUSPENDIDO)
                .build();

        assertThrows(BadRequestException.class, () -> service.actualizar(1, dto));
    }
}
