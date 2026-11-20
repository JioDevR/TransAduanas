package pe.sga.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.sga.support.PruebaDocente;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.sga.dto.request.ExpedienteActualizarDTO;
import pe.sga.dto.request.ExpedienteCrearDTO;
import pe.sga.dto.request.ExpedienteSalidaOrigenDTO;
import pe.sga.dto.response.ExpedienteResponseDTO;
import pe.sga.entity.Cliente;
import pe.sga.entity.Cotizacion;
import pe.sga.entity.Expediente;
import pe.sga.entity.NotificacionCliente;
import pe.sga.entity.Usuario;
import pe.sga.entity.enums.CanalSunat;
import pe.sga.entity.enums.EstadoCotizacion;
import pe.sga.entity.enums.EstadoExpediente;
import pe.sga.entity.enums.EstadoPagoCotizacion;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.CotizacionPagoRepository;
import pe.sga.repository.CotizacionRepository;
import pe.sga.repository.DespachoTransporteRepository;
import pe.sga.repository.ExpedienteRepository;
import pe.sga.repository.NotificacionClienteRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.impl.ExpedienteServiceImpl;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - ExpedienteServiceImpl")
@Tag("unitaria")
class ExpedienteServiceImplTest {

    @Mock
    private ExpedienteRepository expedienteRepository;
    @Mock
    private CotizacionRepository cotizacionRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private CotizacionPagoRepository cotizacionPagoRepository;
    @Mock
    private NotificacionClienteRepository notificacionClienteRepository;
    @Mock
    private DespachoTransporteRepository despachoRepository;
    @Mock
    private IDespachoTransporteService despachoTransporteService;

    @InjectMocks
    private ExpedienteServiceImpl expedienteService;

    @Test
    @DisplayName("Normal: crear expediente desde cotización aprobada con adelanto validado genera 2 notificaciones B2B")
    void crear_cotizacionAprobadaConPago_creaExpedienteYNotifica() {
        Cliente cliente = Cliente.builder().idCliente(10).razonSocial("Pacífico SAC").build();
        Cotizacion cot = Cotizacion.builder()
                .idCotizacion(1)
                .codigoCotizacion("COT-2026-0010")
                .estado(EstadoCotizacion.APROBADA)
                .estadoPago("ADELANTO_APROBADO")
                .cliente(cliente)
                .build();
        Usuario agente = Usuario.builder()
                .idUsuario(5)
                .nombres("Carlos")
                .apellidos("Mendoza")
                .correoInstitucional("cmendoza@transaduanas.pe")
                .build();

        ExpedienteCrearDTO dto = ExpedienteCrearDTO.builder()
                .idCotizacion(1)
                .idAgenteAduanero(5)
                .numeroContenedor("MSCU1234567")
                .build();

        when(cotizacionRepository.findById(1)).thenReturn(Optional.of(cot));
        when(expedienteRepository.existsByCotizacion_IdCotizacion(1)).thenReturn(false);
        when(usuarioRepository.findById(5)).thenReturn(Optional.of(agente));
        when(expedienteRepository.count()).thenReturn(0L);
        when(expedienteRepository.existsByCodigoExpediente(anyString())).thenReturn(false);
        when(expedienteRepository.save(any(Expediente.class))).thenAnswer(inv -> {
            Expediente e = inv.getArgument(0);
            e.setIdExpediente(77);
            return e;
        });
        when(despachoRepository.findByExpediente_IdExpediente(77)).thenReturn(Optional.empty());

        ExpedienteResponseDTO res = expedienteService.crear(dto);

        assertNotNull(res);
        assertEquals(77, res.getIdExpediente());
        assertEquals(EstadoExpediente.ASIGNADO, res.getEstadoExpediente());
        assertEquals(CanalSunat.PENDIENTE, res.getCanalSunat());
        verify(notificacionClienteRepository, times(2)).save(any(NotificacionCliente.class));
    }

    @Test
    @DisplayName("Excepción: compuerta financiera bloquea apertura si la cotización no tiene pago aprobado")
    @PruebaDocente(codigo="PU03", tipo="Unitaria", herramientas="JUnit Jupiter 6 + Mockito (MockitoExtension, @Mock, @InjectMocks); Maven Surefire", objetivo="Cotizacion aprobada sin pago aprobado: rechaza la apertura y nunca guarda un expediente en el repositorio simulado.")
    void crear_sinPagoAprobado_lanzaBadRequest() {
        Cotizacion cot = Cotizacion.builder()
                .idCotizacion(2)
                .estado(EstadoCotizacion.APROBADA)
                .estadoPago("PENDIENTE_REVISION")
                .build();

        when(cotizacionRepository.findById(2)).thenReturn(Optional.of(cot));
        when(cotizacionPagoRepository.existsByCotizacion_IdCotizacionAndEstadoPago(2, EstadoPagoCotizacion.APROBADO))
                .thenReturn(false);

        ExpedienteCrearDTO dto = ExpedienteCrearDTO.builder().idCotizacion(2).idAgenteAduanero(5).build();

        assertThrows(BadRequestException.class, () -> expedienteService.crear(dto));
        verify(expedienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Excepción: cotización no aprobada o expediente duplicado lanzan BadRequestException")
    void crear_cotizacionNoAprobadaODuplicada_lanzaBadRequest() {
        Cotizacion cotPendiente = Cotizacion.builder().idCotizacion(3).estado(EstadoCotizacion.PENDIENTE).build();
        when(cotizacionRepository.findById(3)).thenReturn(Optional.of(cotPendiente));
        assertThrows(BadRequestException.class,
                () -> expedienteService.crear(ExpedienteCrearDTO.builder().idCotizacion(3).idAgenteAduanero(5).build()));

        Cotizacion cotAprobada = Cotizacion.builder().idCotizacion(4).estado(EstadoCotizacion.APROBADA).estadoPago("PAGADO_TOTAL").build();
        when(cotizacionRepository.findById(4)).thenReturn(Optional.of(cotAprobada));
        when(expedienteRepository.existsByCotizacion_IdCotizacion(4)).thenReturn(true);
        assertThrows(BadRequestException.class,
                () -> expedienteService.crear(ExpedienteCrearDTO.builder().idCotizacion(4).idAgenteAduanero(5).build()));
    }

    @Test
    @DisplayName("Excepción: cotización o agente inexistente lanzan ResourceNotFoundException")
    void crear_cotizacionOAgenteInexistente_lanzaResourceNotFound() {
        when(cotizacionRepository.findById(99)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class,
                () -> expedienteService.crear(ExpedienteCrearDTO.builder().idCotizacion(99).idAgenteAduanero(5).build()));

        Cotizacion cot = Cotizacion.builder().idCotizacion(5).estado(EstadoCotizacion.APROBADA).estadoPago("PAGADO_TOTAL").build();
        when(cotizacionRepository.findById(5)).thenReturn(Optional.of(cot));
        when(expedienteRepository.existsByCotizacion_IdCotizacion(5)).thenReturn(false);
        when(usuarioRepository.findById(404)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> expedienteService.crear(ExpedienteCrearDTO.builder().idCotizacion(5).idAgenteAduanero(404).build()));
    }

    @Test
    @DisplayName("Excepción: actualizar expediente en estado CERRADO o ANULADO lanza BadRequestException")
    void actualizar_expedienteCerradoOAnulado_lanzaBadRequest() {
        Expediente cerrado = Expediente.builder().idExpediente(10).estadoExpediente(EstadoExpediente.CERRADO).build();
        Expediente anulado = Expediente.builder().idExpediente(11).estadoExpediente(EstadoExpediente.ANULADO).build();

        when(expedienteRepository.findById(10)).thenReturn(Optional.of(cerrado));
        when(expedienteRepository.findById(11)).thenReturn(Optional.of(anulado));

        ExpedienteActualizarDTO dto = new ExpedienteActualizarDTO();
        assertThrows(BadRequestException.class, () -> expedienteService.actualizar(10, dto));
        assertThrows(BadRequestException.class, () -> expedienteService.actualizar(11, dto));
    }

    @Test
    @DisplayName("Nulo / Límite: confirmarSalidaOrigen con fechaZarpeReal nula usa fecha actual y pasa a EN_TRAMITE")
    void confirmarSalidaOrigen_fechaNula_asignaHoyYEnTramite() {
        Expediente exp = Expediente.builder()
                .idExpediente(15)
                .codigoExpediente("EXP-2026-0015")
                .estadoExpediente(EstadoExpediente.ASIGNADO)
                .build();

        ExpedienteSalidaOrigenDTO dto = new ExpedienteSalidaOrigenDTO();
        dto.setFechaZarpeReal(null);
        dto.setNaveViaje("  MSC VALPARAISO  ");

        when(expedienteRepository.findById(15)).thenReturn(Optional.of(exp));
        when(expedienteRepository.save(any(Expediente.class))).thenAnswer(inv -> inv.getArgument(0));
        when(despachoRepository.findByExpediente_IdExpediente(15)).thenReturn(Optional.empty());

        ExpedienteResponseDTO res = expedienteService.confirmarSalidaOrigen(15, dto);

        assertEquals(LocalDate.now(), res.getFechaZarpeEtd());
        assertEquals("MSC VALPARAISO", res.getNaveViaje());
        assertEquals(EstadoExpediente.EN_TRAMITE, res.getEstadoExpediente());
    }

    @Test
    @DisplayName("Nulo: listarTodos con soloActivos=null consulta findAll; con soloActivos=true excluye ANULADO")
    void listarTodos_soloActivosNuloYTrue_usaConsultaCorrecta() {
        when(expedienteRepository.findAll()).thenReturn(List.of());
        when(expedienteRepository.findByEstadoExpedienteNot(EstadoExpediente.ANULADO)).thenReturn(List.of());

        expedienteService.listarTodos(null);
        expedienteService.listarTodos(true);

        verify(expedienteRepository).findAll();
        verify(expedienteRepository).findByEstadoExpedienteNot(EstadoExpediente.ANULADO);
    }
}
