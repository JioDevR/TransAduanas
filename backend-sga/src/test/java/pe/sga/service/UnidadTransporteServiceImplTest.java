package pe.sga.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.sga.dto.request.UnidadTransporteRequestDTO;
import pe.sga.dto.response.UnidadTransporteResponseDTO;
import pe.sga.entity.UnidadTransporte;
import pe.sga.entity.enums.EstadoOperativoFlota;
import pe.sga.entity.enums.TipoVehiculo;
import pe.sga.exception.BadRequestException;
import pe.sga.repository.DespachoTransporteRepository;
import pe.sga.repository.UnidadTransporteRepository;
import pe.sga.service.impl.UnidadTransporteServiceImpl;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unitaria")
class UnidadTransporteServiceImplTest {

    @Mock
    private UnidadTransporteRepository unidadRepo;

    @Mock
    private DespachoTransporteRepository despachoRepo;

    @InjectMocks
    private UnidadTransporteServiceImpl service;

    private UnidadTransporte unidad;

    @BeforeEach
    void setUp() {
        unidad = UnidadTransporte.builder()
                .idUnidad(1)
                .placaTracto("V1A-780")
                .placaCarreta("T1A-981")
                .marcaModelo("Volvo FH 500")
                .tipoVehiculo(TipoVehiculo.TRACTO)
                .capacidadCargaKg(new BigDecimal("32000.00"))
                .estadoOperativo(EstadoOperativoFlota.DISPONIBLE)
                .estadoActivo(true)
                .build();
    }

    @Test
    void crear_normalizaPlacaYValidaDuplicado() {
        UnidadTransporteRequestDTO dto = UnidadTransporteRequestDTO.builder()
                .placaTracto("  v9z-111 ")
                .marcaModelo("Scania R500")
                .build();

        when(unidadRepo.existsByPlacaTracto("V9Z-111")).thenReturn(false);
        when(unidadRepo.save(any(UnidadTransporte.class))).thenAnswer(i -> i.getArgument(0));

        UnidadTransporteResponseDTO res = service.crear(dto);
        assertEquals("V9Z-111", res.getPlacaTracto());
        assertEquals(TipoVehiculo.TRACTO, res.getTipoVehiculo());

        when(unidadRepo.existsByPlacaTracto("V9Z-111")).thenReturn(true);
        assertThrows(BadRequestException.class, () -> service.crear(dto));
    }

    @Test
    void actualizar_limpiaCamposOpcionalesCuandoVienenVacios() {
        when(unidadRepo.findById(1)).thenReturn(Optional.of(unidad));
        when(unidadRepo.save(any(UnidadTransporte.class))).thenAnswer(i -> i.getArgument(0));

        UnidadTransporteRequestDTO dto = UnidadTransporteRequestDTO.builder()
                .placaTracto("V1A-780")
                .placaCarreta("  ")
                .marcaModelo("Volvo FH 500")
                .observaciones("")
                .build();

        UnidadTransporteResponseDTO res = service.actualizar(1, dto);
        assertNull(res.getPlacaCarreta());
        assertNull(res.getObservaciones());
    }

    @Test
    void eliminar_fallaSiEstaEnDespachoActivo() {
        when(unidadRepo.findById(1)).thenReturn(Optional.of(unidad));
        when(despachoRepo.existsByUnidad_IdUnidadAndEstadoTransporteIn(eq(1), any())).thenReturn(true);

        assertThrows(BadRequestException.class, () -> service.eliminar(1));
        verify(unidadRepo, never()).save(any());
    }
}
