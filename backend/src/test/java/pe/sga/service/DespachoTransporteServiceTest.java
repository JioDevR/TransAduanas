package pe.sga.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.sga.dto.request.DespachoAsignarRecursosDTO;
import pe.sga.dto.request.DespachoEntregaDTO;
import pe.sga.dto.request.DespachoRutaDTO;
import pe.sga.dto.response.DespachoTransporteResponseDTO;
import pe.sga.entity.*;
import pe.sga.exception.BadRequestException;
import pe.sga.repository.*;
import pe.sga.service.impl.DespachoTransporteServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DespachoTransporteServiceTest {

    @Mock
    private DespachoTransporteRepository despachoRepo;

    @Mock
    private ConductorRepository conductorRepo;

    @Mock
    private UnidadTransporteRepository unidadRepo;

    @Mock
    private ExpedienteRepository expedienteRepo;

    @Mock
    private IDocumentoAdjuntoRepository documentoRepo;

    @Mock
    private NotificacionClienteRepository notificacionClienteRepo;

    @InjectMocks
    private DespachoTransporteServiceImpl despachoService;

    private DespachoTransporte despacho;
    private UnidadTransporte unidad;
    private Conductor conductor;
    private Cliente cliente;
    private Expediente expediente;

    @BeforeEach
    void setUp() {
        cliente = Cliente.builder().idCliente(1).razonSocial("Importadora Lima SAC").build();
        expediente = Expediente.builder().idExpediente(10).codigoExpediente("EXP-2026-0001").cliente(cliente).build();

        despacho = DespachoTransporte.builder()
                .idDespachoTransporte(100)
                .codigoTracking("TRK-2026-0001")
                .expediente(expediente)
                .estadoTransporte("PENDIENTE_ASIGNACION")
                .direccionPartida("Callao")
                .direccionLlegada("Lurin")
                .distritoLlegada("Lurin")
                .fechaProgramada(LocalDateTime.now().plusDays(1))
                .build();

        unidad = UnidadTransporte.builder()
                .idUnidad(5)
                .placaTracto("V1A-780")
                .marcaModelo("Volvo FH 500")
                .soatVencimiento(LocalDate.now().plusYears(1))
                .citvVencimiento(LocalDate.now().plusYears(1))
                .kilometrajeActual(100000)
                .estadoOperativo("DISPONIBLE")
                .estadoActivo(true)
                .build();

        conductor = Conductor.builder()
                .idConductor(8)
                .nombres("Carlos")
                .apellidos("Santillan")
                .numeroLicencia("Q12345678")
                .tipoLicencia("A-IIIc")
                .fechaVencimientoLicencia(LocalDate.now().plusYears(2))
                .estadoOperativo("DISPONIBLE")
                .estadoActivo(true)
                .build();
    }

    @Test
    void asignarRecursos_fallaSiSoatVencido() {
        unidad.setSoatVencimiento(LocalDate.now().minusDays(1));
        when(despachoRepo.findById(100)).thenReturn(Optional.of(despacho));
        when(conductorRepo.findById(8)).thenReturn(Optional.of(conductor));
        when(unidadRepo.findById(5)).thenReturn(Optional.of(unidad));

        DespachoAsignarRecursosDTO dto = DespachoAsignarRecursosDTO.builder()
                .idConductor(8)
                .idUnidad(5)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                despachoService.asignarRecursos(100, dto)
        );
        assertTrue(ex.getMessage().contains("SOAT vencido"));
    }

    @Test
    void asignarRecursos_fallaSiLicenciaConductorVencida() {
        conductor.setFechaVencimientoLicencia(LocalDate.now().minusDays(2));
        when(despachoRepo.findById(100)).thenReturn(Optional.of(despacho));
        when(conductorRepo.findById(8)).thenReturn(Optional.of(conductor));
        when(unidadRepo.findById(5)).thenReturn(Optional.of(unidad));

        DespachoAsignarRecursosDTO dto = DespachoAsignarRecursosDTO.builder()
                .idConductor(8)
                .idUnidad(5)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                despachoService.asignarRecursos(100, dto)
        );
        assertTrue(ex.getMessage().contains("licencia de conducir vencida"));
    }

    @Test
    void asignarRecursos_fallaSiUnidadYaComprometidaEnOtroDespacho() {
        when(despachoRepo.findById(100)).thenReturn(Optional.of(despacho));
        when(conductorRepo.findById(8)).thenReturn(Optional.of(conductor));
        when(unidadRepo.findById(5)).thenReturn(Optional.of(unidad));
        when(despachoRepo.existsByUnidad_IdUnidadAndEstadoTransporteInAndIdDespachoTransporteNot(
                eq(5), any(), eq(100))).thenReturn(true);

        DespachoAsignarRecursosDTO dto = DespachoAsignarRecursosDTO.builder()
                .idConductor(8)
                .idUnidad(5)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                despachoService.asignarRecursos(100, dto)
        );
        assertTrue(ex.getMessage().contains("ya se encuentra asignada en otro despacho activo"));
    }

    @Test
    void asignarRecursos_fallaSiUnidadNoDisponible() {
        unidad.setEstadoOperativo("MANTENIMIENTO");
        when(despachoRepo.findById(100)).thenReturn(Optional.of(despacho));
        when(conductorRepo.findById(8)).thenReturn(Optional.of(conductor));
        when(unidadRepo.findById(5)).thenReturn(Optional.of(unidad));

        DespachoAsignarRecursosDTO dto = DespachoAsignarRecursosDTO.builder()
                .idConductor(8)
                .idUnidad(5)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                despachoService.asignarRecursos(100, dto)
        );
        assertTrue(ex.getMessage().contains("no se encuentra DISPONIBLE"));
    }

    @Test
    void asignarRecursos_fallaSiConductorNoDisponible() {
        conductor.setEstadoOperativo("DESCANSO");
        when(despachoRepo.findById(100)).thenReturn(Optional.of(despacho));
        when(conductorRepo.findById(8)).thenReturn(Optional.of(conductor));
        when(unidadRepo.findById(5)).thenReturn(Optional.of(unidad));

        DespachoAsignarRecursosDTO dto = DespachoAsignarRecursosDTO.builder()
                .idConductor(8)
                .idUnidad(5)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                despachoService.asignarRecursos(100, dto)
        );
        assertTrue(ex.getMessage().contains("no se encuentra DISPONIBLE"));
    }

    @Test
    void confirmarEntrega_conKilometrajeActualNull_asignaKilometrajeLlegada() {
        unidad.setKilometrajeActual(null);
        despacho.setUnidad(unidad);
        despacho.setConductor(conductor);
        when(despachoRepo.findById(100)).thenReturn(Optional.of(despacho));
        when(despachoRepo.save(any(DespachoTransporte.class))).thenAnswer(i -> i.getArgument(0));

        DespachoEntregaDTO dto = DespachoEntregaDTO.builder()
                .receptorNombre("Juan Perez")
                .kilometrajeLlegada(500)
                .build();

        despachoService.confirmarEntrega(100, dto);
        assertEquals(500, unidad.getKilometrajeActual());
    }

    @Test
    void iniciarRuta_actualizaEstadosOperativosYEnviaNotificacion() {
        despacho.setUnidad(unidad);
        despacho.setConductor(conductor);
        when(despachoRepo.findById(100)).thenReturn(Optional.of(despacho));
        when(despachoRepo.save(any(DespachoTransporte.class))).thenAnswer(i -> i.getArgument(0));

        DespachoRutaDTO dto = DespachoRutaDTO.builder().build();
        DespachoTransporteResponseDTO resp = despachoService.iniciarRuta(100, dto);

        assertEquals("EN_RUTA", resp.getEstadoTransporte());
        assertEquals("EN_RUTA", unidad.getEstadoOperativo());
        assertEquals("EN_RUTA", conductor.getEstadoOperativo());
        verify(notificacionClienteRepo, times(1)).save(any(NotificacionCliente.class));
    }

    @Test
    void confirmarEntrega_liberaRecursosActualizaKilometrajeYEnviaNotificacion() {
        despacho.setUnidad(unidad);
        despacho.setConductor(conductor);
        despacho.setEstadoTransporte("EN_RUTA");
        unidad.setEstadoOperativo("EN_RUTA");
        conductor.setEstadoOperativo("EN_RUTA");

        when(despachoRepo.findById(100)).thenReturn(Optional.of(despacho));
        when(despachoRepo.save(any(DespachoTransporte.class))).thenAnswer(i -> i.getArgument(0));

        DespachoEntregaDTO dto = DespachoEntregaDTO.builder()
                .receptorNombre("Juan Perez")
                .receptorDni("44556677")
                .kilometrajeLlegada(100150)
                .build();

        DespachoTransporteResponseDTO resp = despachoService.confirmarEntrega(100, dto);

        assertEquals("ENTREGADO", resp.getEstadoTransporte());
        assertEquals("DISPONIBLE", unidad.getEstadoOperativo());
        assertEquals("DISPONIBLE", conductor.getEstadoOperativo());
        assertEquals(100150, unidad.getKilometrajeActual());
        verify(notificacionClienteRepo, times(1)).save(any(NotificacionCliente.class));
    }
}
