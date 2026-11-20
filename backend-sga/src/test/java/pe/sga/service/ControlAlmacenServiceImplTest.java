package pe.sga.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.sga.dto.request.ControlAlmacenRequestDTO;
import pe.sga.dto.request.ControlAlmacenSalidaDTO;
import pe.sga.dto.response.ControlAlmacenResponseDTO;
import pe.sga.entity.ControlAlmacen;
import pe.sga.entity.Expediente;
import pe.sga.entity.enums.CanalSunat;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.ControlAlmacenRepository;
import pe.sga.repository.ExpedienteRepository;
import pe.sga.service.impl.ControlAlmacenServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - ControlAlmacenServiceImpl")
@Tag("unitaria")
class ControlAlmacenServiceImplTest {

    @Mock
    private ControlAlmacenRepository controlAlmacenRepository;

    @Mock
    private ExpedienteRepository expedienteRepository;

    @InjectMocks
    private ControlAlmacenServiceImpl almacenService;

    @Test
    @DisplayName("Normal / Límite: ingreso con tipoIngreso nulo infiere DIRECTO_VERDE para canal VERDE y POST_INSPECCION para ROJO")
    void registrarIngreso_tipoIngresoNulo_infiereSegunCanalSunat() {
        Expediente expVerde = Expediente.builder().idExpediente(1).canalSunat(CanalSunat.VERDE).build();
        Expediente expRojo = Expediente.builder().idExpediente(2).canalSunat(CanalSunat.ROJO).build();

        when(expedienteRepository.findById(1)).thenReturn(Optional.of(expVerde));
        when(expedienteRepository.findById(2)).thenReturn(Optional.of(expRojo));
        when(controlAlmacenRepository.existsByExpediente_IdExpediente(anyInt())).thenReturn(false);
        when(controlAlmacenRepository.save(any(ControlAlmacen.class))).thenAnswer(inv -> inv.getArgument(0));

        ControlAlmacenRequestDTO dtoVerde = ControlAlmacenRequestDTO.builder()
                .idExpediente(1)
                .depositoTemporal("  APM Terminals Callao  ")
                .tipoIngreso(null)
                .diasLibresAlmacen(0) // límite 0 días libres
                .build();

        ControlAlmacenRequestDTO dtoRojo = ControlAlmacenRequestDTO.builder()
                .idExpediente(2)
                .depositoTemporal("Neptunia")
                .tipoIngreso("   ")
                .diasLibresAlmacen(null) // default 15
                .build();

        ControlAlmacenResponseDTO resVerde = almacenService.registrarIngreso(dtoVerde);
        ControlAlmacenResponseDTO resRojo = almacenService.registrarIngreso(dtoRojo);

        assertEquals("DIRECTO_VERDE", resVerde.getTipoIngreso());
        assertEquals("APM Terminals Callao", resVerde.getDepositoTemporal());
        assertEquals(LocalDate.now(), resVerde.getFechaLimiteRetiro());
        assertEquals("EN_CUSTODIA", resVerde.getEstadoAlmacen());

        assertEquals("POST_INSPECCION", resRojo.getTipoIngreso());
        assertEquals(15, resRojo.getDiasLibresAlmacen());
        assertEquals(LocalDate.now().plusDays(15), resRojo.getFechaLimiteRetiro());
    }

    @Test
    @DisplayName("Excepción: registrar ingreso duplicado o para expediente inexistente lanza excepción")
    void registrarIngreso_duplicadoOInexistente_lanzaExcepcion() {
        when(expedienteRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> almacenService.registrarIngreso(ControlAlmacenRequestDTO.builder().idExpediente(99).depositoTemporal("DPW").build()));

        Expediente exp = Expediente.builder().idExpediente(1).build();
        when(expedienteRepository.findById(1)).thenReturn(Optional.of(exp));
        when(controlAlmacenRepository.existsByExpediente_IdExpediente(1)).thenReturn(true);

        assertThrows(BadRequestException.class,
                () -> almacenService.registrarIngreso(ControlAlmacenRequestDTO.builder().idExpediente(1).depositoTemporal("DPW").build()));
    }

    @Test
    @DisplayName("Excepción: actualizar registro en estado DESPACHADO o ANULADO lanza BadRequestException")
    void actualizar_estadoBloqueado_lanzaBadRequest() {
        ControlAlmacen despachado = ControlAlmacen.builder().idControlAlmacen(10).estadoAlmacen("DESPACHADO").build();
        ControlAlmacen anulado = ControlAlmacen.builder().idControlAlmacen(11).estadoAlmacen("ANULADO").build();

        when(controlAlmacenRepository.findById(10)).thenReturn(Optional.of(despachado));
        when(controlAlmacenRepository.findById(11)).thenReturn(Optional.of(anulado));

        ControlAlmacenRequestDTO dto = ControlAlmacenRequestDTO.builder().pesoBrutoKg(new BigDecimal("120.5")).build();

        assertThrows(BadRequestException.class, () -> almacenService.actualizar(10, dto));
        assertThrows(BadRequestException.class, () -> almacenService.actualizar(11, dto));
    }

    @Test
    @DisplayName("Normal / Nulo: autorizarSalida con fechaSalida nula asigna fecha actual y estado DESPACHADO")
    void autorizarSalida_fechaNula_asignaAhoraYDespachado() {
        ControlAlmacen ca = ControlAlmacen.builder()
                .idControlAlmacen(20)
                .estadoAlmacen("EN_CUSTODIA")
                .autorizadoMovimiento(false)
                .fechaIngreso(LocalDateTime.now().minusDays(2))
                .build();

        when(controlAlmacenRepository.findById(20)).thenReturn(Optional.of(ca));
        when(controlAlmacenRepository.save(any(ControlAlmacen.class))).thenAnswer(inv -> inv.getArgument(0));

        ControlAlmacenResponseDTO res = almacenService.autorizarSalida(20, ControlAlmacenSalidaDTO.builder()
                .fechaSalida(null)
                .observaciones("Retiro conforme")
                .build());

        assertTrue(res.getAutorizadoMovimiento());
        assertEquals("DESPACHADO", res.getEstadoAlmacen());
        assertNotNull(res.getFechaSalida());
        assertEquals("Retiro conforme", res.getObservaciones());
    }

    @Test
    @DisplayName("Normal / Nulo: anular cambia estado a ANULADO y listarTodos gestiona soloActivos nulo o true")
    void anularYListarTodos_comportamientoEsperado() {
        ControlAlmacen ca = ControlAlmacen.builder().idControlAlmacen(30).estadoAlmacen("EN_CUSTODIA").build();
        when(controlAlmacenRepository.findById(30)).thenReturn(Optional.of(ca));
        when(controlAlmacenRepository.findAll()).thenReturn(List.of(ca));
        when(controlAlmacenRepository.findByEstadoAlmacenNot("ANULADO")).thenReturn(List.of());

        almacenService.anular(30);
        assertEquals("ANULADO", ca.getEstadoAlmacen());

        assertEquals(1, almacenService.listarTodos(null).size());
        assertEquals(0, almacenService.listarTodos(true).size());
    }
}
