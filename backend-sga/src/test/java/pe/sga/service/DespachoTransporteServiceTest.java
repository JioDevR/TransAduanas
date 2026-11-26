package pe.sga.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.sga.dto.request.DespachoAsignarRecursosDTO;
import pe.sga.dto.request.DespachoEntregaDTO;
import pe.sga.dto.request.DespachoRutaDTO;
import pe.sga.dto.request.DespachoTransporteCrearDTO;
import pe.sga.dto.response.DespachoTransporteResponseDTO;
import pe.sga.entity.*;
import pe.sga.entity.enums.CategoriaLicenciaMtc;
import pe.sga.entity.enums.EstadoExpediente;
import pe.sga.entity.enums.EstadoOperativoConductor;
import pe.sga.entity.enums.EstadoOperativoFlota;
import pe.sga.entity.enums.EstadoTransporte;
import pe.sga.entity.enums.TipoTraslado;
import pe.sga.entity.enums.TipoVehiculo;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.*;
import pe.sga.service.impl.DespachoTransporteServiceImpl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unitaria")
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
        expediente = Expediente.builder()
                .idExpediente(10)
                .codigoExpediente("EXP-2026-0001")
                .estadoExpediente(EstadoExpediente.LEVANTE_AUTORIZADO)
                .cliente(cliente)
                .build();

        despacho = DespachoTransporte.builder()
                .idDespachoTransporte(100)
                .codigoTracking("TRK-2026-0001")
                .tipoTraslado(TipoTraslado.ENTREGA_IMPORTACION)
                .expediente(expediente)
                .estadoTransporte(EstadoTransporte.PENDIENTE_ASIGNACION)
                .direccionPartida("Callao")
                .direccionLlegada("Lurin")
                .distritoLlegada("Lurin")
                .fechaProgramada(LocalDateTime.now().plusDays(1))
                .build();

        unidad = UnidadTransporte.builder()
                .idUnidad(5)
                .placaTracto("V1A-780")
                .marcaModelo("Volvo FH 500")
                .tipoVehiculo(TipoVehiculo.TRACTO)
                .soatVencimiento(LocalDate.now().plusYears(1))
                .citvVencimiento(LocalDate.now().plusYears(1))
                .kilometrajeActual(100000)
                .estadoOperativo(EstadoOperativoFlota.DISPONIBLE)
                .estadoActivo(true)
                .build();

        conductor = Conductor.builder()
                .idConductor(8)
                .nombres("Carlos")
                .apellidos("Santillan")
                .numeroLicencia("Q12345678")
                .tipoLicencia(CategoriaLicenciaMtc.A_IIIC)
                .fechaVencimientoLicencia(LocalDate.now().plusYears(2))
                .estadoOperativo(EstadoOperativoConductor.DISPONIBLE)
                .estadoActivo(true)
                .build();
    }

    @Test
    void programar_conRecursosInvalidos_validaMtcYDisponibilidad() {
        unidad.setSoatVencimiento(LocalDate.now().minusDays(1));
        when(expedienteRepo.findById(10)).thenReturn(Optional.of(expediente));
        when(despachoRepo.existsByExpediente_IdExpedienteAndEstadoTransporteNot(10, EstadoTransporte.CANCELADO))
                .thenReturn(false);
        when(conductorRepo.findByIdForUpdate(8)).thenReturn(Optional.of(conductor));
        when(unidadRepo.findByIdForUpdate(5)).thenReturn(Optional.of(unidad));

        DespachoTransporteCrearDTO dto = DespachoTransporteCrearDTO.builder()
                .idExpediente(10)
                .idConductor(8)
                .idUnidad(5)
                .direccionPartida("Callao")
                .direccionLlegada("Lurin")
                .distritoLlegada("Lurin")
                .fechaProgramada(LocalDateTime.now().plusDays(1))
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () -> despachoService.programar(dto));
        assertTrue(ex.getMessage().contains("SOAT vencido"));
        verify(despachoRepo, never()).save(any());
    }

    @Test
    void programar_sinRecursos_creaPendienteAsignacion() {
        when(expedienteRepo.findById(10)).thenReturn(Optional.of(expediente));
        when(despachoRepo.existsByExpediente_IdExpedienteAndEstadoTransporteNot(10, EstadoTransporte.CANCELADO))
                .thenReturn(false);
        when(despachoRepo.findMaxId()).thenReturn(4);
        when(despachoRepo.existsByCodigoTracking(any())).thenReturn(false);
        when(despachoRepo.save(any(DespachoTransporte.class))).thenAnswer(i -> i.getArgument(0));

        DespachoTransporteCrearDTO dto = DespachoTransporteCrearDTO.builder()
                .idExpediente(10)
                .tipoTraslado(TipoTraslado.RECOJO_EXPORTACION)
                .direccionPartida("Chao")
                .direccionLlegada("Callao")
                .distritoLlegada("Callao")
                .fechaProgramada(LocalDateTime.now().plusDays(1))
                .build();

        DespachoTransporteResponseDTO res = despachoService.programar(dto);
        assertEquals(EstadoTransporte.PENDIENTE_ASIGNACION, res.getEstadoTransporte());
        assertEquals(TipoTraslado.RECOJO_EXPORTACION, res.getTipoTraslado());
    }

    @Test
    void asignarRecursos_fallaSiSoatVencido() {
        unidad.setSoatVencimiento(LocalDate.now().minusDays(1));
        when(despachoRepo.findByIdWithDetails(100)).thenReturn(Optional.of(despacho));
        when(conductorRepo.findByIdForUpdate(8)).thenReturn(Optional.of(conductor));
        when(unidadRepo.findByIdForUpdate(5)).thenReturn(Optional.of(unidad));

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
    void asignarRecursos_fallaSiCitvVencido() {
        unidad.setCitvVencimiento(LocalDate.now().minusDays(1));
        when(despachoRepo.findByIdWithDetails(100)).thenReturn(Optional.of(despacho));
        when(conductorRepo.findByIdForUpdate(8)).thenReturn(Optional.of(conductor));
        when(unidadRepo.findByIdForUpdate(5)).thenReturn(Optional.of(unidad));

        DespachoAsignarRecursosDTO dto = DespachoAsignarRecursosDTO.builder()
                .idConductor(8)
                .idUnidad(5)
                .build();

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                despachoService.asignarRecursos(100, dto)
        );
        assertTrue(ex.getMessage().contains("CITV vencida"));
    }

    @Test
    void asignarRecursos_fallaSiLicenciaConductorVencida() {
        conductor.setFechaVencimientoLicencia(LocalDate.now().minusDays(2));
        when(despachoRepo.findByIdWithDetails(100)).thenReturn(Optional.of(despacho));
        when(conductorRepo.findByIdForUpdate(8)).thenReturn(Optional.of(conductor));
        when(unidadRepo.findByIdForUpdate(5)).thenReturn(Optional.of(unidad));

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
        when(despachoRepo.findByIdWithDetails(100)).thenReturn(Optional.of(despacho));
        when(conductorRepo.findByIdForUpdate(8)).thenReturn(Optional.of(conductor));
        when(unidadRepo.findByIdForUpdate(5)).thenReturn(Optional.of(unidad));
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
    void asignarRecursos_fallaSiDespachoYaEnRuta() {
        despacho.setEstadoTransporte(EstadoTransporte.EN_RUTA);
        when(despachoRepo.findByIdWithDetails(100)).thenReturn(Optional.of(despacho));

        DespachoAsignarRecursosDTO dto = DespachoAsignarRecursosDTO.builder()
                .idConductor(8)
                .idUnidad(5)
                .build();

        assertThrows(BadRequestException.class, () -> despachoService.asignarRecursos(100, dto));
    }

    @Test
    void asignarRecursos_fallaSiUnidadNoDisponible() {
        unidad.setEstadoOperativo(EstadoOperativoFlota.MANTENIMIENTO);
        when(despachoRepo.findByIdWithDetails(100)).thenReturn(Optional.of(despacho));
        when(conductorRepo.findByIdForUpdate(8)).thenReturn(Optional.of(conductor));
        when(unidadRepo.findByIdForUpdate(5)).thenReturn(Optional.of(unidad));

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
        conductor.setEstadoOperativo(EstadoOperativoConductor.DESCANSO);
        when(despachoRepo.findByIdWithDetails(100)).thenReturn(Optional.of(despacho));
        when(conductorRepo.findByIdForUpdate(8)).thenReturn(Optional.of(conductor));
        when(unidadRepo.findByIdForUpdate(5)).thenReturn(Optional.of(unidad));

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
    void iniciarRuta_fallaSiNoEstaProgramado() {
        despacho.setEstadoTransporte(EstadoTransporte.PENDIENTE_ASIGNACION);
        when(despachoRepo.findByIdWithDetails(100)).thenReturn(Optional.of(despacho));

        assertThrows(BadRequestException.class, () ->
                despachoService.iniciarRuta(100, DespachoRutaDTO.builder().build()));
    }

    @Test
    void iniciarRuta_actualizaEstadosOperativosYEnviaNotificacion() {
        despacho.setEstadoTransporte(EstadoTransporte.PROGRAMADO);
        despacho.setUnidad(unidad);
        despacho.setConductor(conductor);
        when(despachoRepo.findByIdWithDetails(100)).thenReturn(Optional.of(despacho));
        when(conductorRepo.findByIdForUpdate(8)).thenReturn(Optional.of(conductor));
        when(unidadRepo.findByIdForUpdate(5)).thenReturn(Optional.of(unidad));
        when(despachoRepo.save(any(DespachoTransporte.class))).thenAnswer(i -> i.getArgument(0));

        DespachoRutaDTO dto = DespachoRutaDTO.builder().build();
        DespachoTransporteResponseDTO resp = despachoService.iniciarRuta(100, dto);

        assertEquals(EstadoTransporte.EN_RUTA, resp.getEstadoTransporte());
        assertEquals(EstadoOperativoFlota.EN_RUTA, unidad.getEstadoOperativo());
        assertEquals(EstadoOperativoConductor.EN_RUTA, conductor.getEstadoOperativo());
        ArgumentCaptor<NotificacionCliente> notifCaptor = ArgumentCaptor.forClass(NotificacionCliente.class);
        verify(notificacionClienteRepo, times(1)).save(notifCaptor.capture());
        assertEquals("TRANSPORTE", notifCaptor.getValue().getTipo());
        assertEquals("/expedientes", notifCaptor.getValue().getEnlace());
    }

    @Test
    void confirmarEntrega_fallaSiNoEstaEnRuta() {
        despacho.setEstadoTransporte(EstadoTransporte.PROGRAMADO);
        when(despachoRepo.findByIdWithDetails(100)).thenReturn(Optional.of(despacho));

        assertThrows(BadRequestException.class, () ->
                despachoService.confirmarEntrega(100, DespachoEntregaDTO.builder().build()));
    }

    @Test
    void confirmarEntrega_fallaSiDocumentoRemisionNoExiste() {
        despacho.setEstadoTransporte(EstadoTransporte.EN_RUTA);
        despacho.setUnidad(unidad);
        despacho.setConductor(conductor);
        when(despachoRepo.findByIdWithDetails(100)).thenReturn(Optional.of(despacho));
        when(documentoRepo.findById(999)).thenReturn(Optional.empty());

        DespachoEntregaDTO dto = DespachoEntregaDTO.builder().idDocumentoRemision(999).build();
        assertThrows(ResourceNotFoundException.class, () -> despachoService.confirmarEntrega(100, dto));
    }

    @Test
    void confirmarEntrega_fallaSiKilometrajeMenorAlActual() {
        despacho.setEstadoTransporte(EstadoTransporte.EN_RUTA);
        despacho.setUnidad(unidad);
        despacho.setConductor(conductor);
        when(despachoRepo.findByIdWithDetails(100)).thenReturn(Optional.of(despacho));

        DespachoEntregaDTO dto = DespachoEntregaDTO.builder().kilometrajeLlegada(90000).build();
        assertThrows(BadRequestException.class, () -> despachoService.confirmarEntrega(100, dto));
    }

    @Test
    void confirmarEntrega_conKilometrajeActualNull_asignaKilometrajeLlegada() {
        unidad.setKilometrajeActual(null);
        despacho.setEstadoTransporte(EstadoTransporte.EN_RUTA);
        despacho.setUnidad(unidad);
        despacho.setConductor(conductor);
        when(despachoRepo.findByIdWithDetails(100)).thenReturn(Optional.of(despacho));
        when(despachoRepo.save(any(DespachoTransporte.class))).thenAnswer(i -> i.getArgument(0));

        DespachoEntregaDTO dto = DespachoEntregaDTO.builder()
                .receptorNombre("Juan Perez")
                .kilometrajeLlegada(500)
                .build();

        despachoService.confirmarEntrega(100, dto);
        assertEquals(500, unidad.getKilometrajeActual());
    }

    @Test
    void confirmarEntrega_liberaRecursosActualizaKilometrajeYEnviaNotificacion() {
        despacho.setUnidad(unidad);
        despacho.setConductor(conductor);
        despacho.setEstadoTransporte(EstadoTransporte.EN_RUTA);
        unidad.setEstadoOperativo(EstadoOperativoFlota.EN_RUTA);
        conductor.setEstadoOperativo(EstadoOperativoConductor.EN_RUTA);

        when(despachoRepo.findByIdWithDetails(100)).thenReturn(Optional.of(despacho));
        when(despachoRepo.save(any(DespachoTransporte.class))).thenAnswer(i -> i.getArgument(0));

        DespachoEntregaDTO dto = DespachoEntregaDTO.builder()
                .receptorNombre("Juan Perez")
                .receptorDni("44556677")
                .kilometrajeLlegada(100150)
                .build();

        DespachoTransporteResponseDTO resp = despachoService.confirmarEntrega(100, dto);

        assertEquals(EstadoTransporte.ENTREGADO, resp.getEstadoTransporte());
        assertEquals(EstadoOperativoFlota.DISPONIBLE, unidad.getEstadoOperativo());
        assertEquals(EstadoOperativoConductor.DISPONIBLE, conductor.getEstadoOperativo());
        assertEquals(100150, unidad.getKilometrajeActual());
        ArgumentCaptor<NotificacionCliente> notifCaptor = ArgumentCaptor.forClass(NotificacionCliente.class);
        verify(notificacionClienteRepo, times(1)).save(notifCaptor.capture());
        assertEquals("TRANSPORTE", notifCaptor.getValue().getTipo());
        assertEquals("/expedientes", notifCaptor.getValue().getEnlace());
    }

    @Test
    void anular_despachoEntregado_lanzaExcepcion() {
        despacho.setEstadoTransporte(EstadoTransporte.ENTREGADO);
        when(despachoRepo.findByIdWithDetails(100)).thenReturn(Optional.of(despacho));

        assertThrows(BadRequestException.class, () -> despachoService.anular(100));
    }

    @Test
    void anular_despachoEnRuta_liberaRecursosSiNoTienenOtroDespachoActivo() {
        despacho.setEstadoTransporte(EstadoTransporte.EN_RUTA);
        despacho.setUnidad(unidad);
        despacho.setConductor(conductor);
        unidad.setEstadoOperativo(EstadoOperativoFlota.EN_RUTA);
        conductor.setEstadoOperativo(EstadoOperativoConductor.EN_RUTA);

        when(despachoRepo.findByIdWithDetails(100)).thenReturn(Optional.of(despacho));

        despachoService.anular(100);

        assertEquals(EstadoTransporte.CANCELADO, despacho.getEstadoTransporte());
        assertEquals(EstadoOperativoFlota.DISPONIBLE, unidad.getEstadoOperativo());
        assertEquals(EstadoOperativoConductor.DISPONIBLE, conductor.getEstadoOperativo());
        verify(despachoRepo).save(despacho);
    }

    @Test
    void reportarIncidencia_fallaEnEstadoEntregadoOBlanco() {
        assertThrows(BadRequestException.class, () -> despachoService.reportarIncidencia(100, "   "));

        despacho.setEstadoTransporte(EstadoTransporte.ENTREGADO);
        when(despachoRepo.findByIdWithDetails(100)).thenReturn(Optional.of(despacho));
        assertThrows(BadRequestException.class, () -> despachoService.reportarIncidencia(100, "Desperfecto"));
    }

    @Test
    void aceptarSolicitud_cambiaDePendienteAAceptado() {
        despacho.setEstadoTransporte(EstadoTransporte.PENDIENTE_ASIGNACION);
        when(despachoRepo.findByIdWithDetails(100)).thenReturn(Optional.of(despacho));
        when(despachoRepo.save(any(DespachoTransporte.class))).thenAnswer(inv -> inv.getArgument(0));

        DespachoTransporteResponseDTO res = despachoService.aceptarSolicitud(100);
        assertEquals(EstadoTransporte.ACEPTADO, res.getEstadoTransporte());
    }
}
