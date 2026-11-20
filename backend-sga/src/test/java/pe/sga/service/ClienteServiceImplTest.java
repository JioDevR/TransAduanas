package pe.sga.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pe.sga.support.PruebaDocente;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pe.sga.dto.request.ClienteRequestDTO;
import pe.sga.dto.response.ClienteResponseDTO;
import pe.sga.entity.Cliente;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.ClienteRepository;
import pe.sga.service.impl.ClienteServiceImpl;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Pruebas Unitarias - ClienteServiceImpl")
@Tag("unitaria")
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;

    @InjectMocks
    private ClienteServiceImpl clienteService;

    @Test
    @DisplayName("Normal: crear cliente normaliza espacios, tipoDocumento por defecto y estado PROSPECTO")
    @PruebaDocente(codigo="PU01", tipo="Unitaria", herramientas="JUnit Jupiter 6 + Mockito (MockitoExtension, @Mock, @InjectMocks); Maven Surefire", objetivo="Entrada con espacios y campos opcionales nulos: devuelve documento y razon social normalizados, tipo RUC y estado PROSPECTO; el guardado es simulado.")
    void crear_datosValidosConOpcionalesNulos_aplicaDefaultsYGuarda() {
        ClienteRequestDTO dto = ClienteRequestDTO.builder()
                .numeroDocumento(" 20609998881 ")
                .razonSocial("  Logística Pacífico SAC  ")
                .correoContacto("  contacto@pacifico.pe ")
                .build();

        when(clienteRepository.existsByNumeroDocumento(dto.getNumeroDocumento())).thenReturn(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setIdCliente(100);
            return c;
        });

        ClienteResponseDTO res = clienteService.crear(dto);

        assertNotNull(res);
        assertEquals(100, res.getIdCliente());
        assertEquals("RUC", res.getTipoDocumento());
        assertEquals("20609998881", res.getNumeroDocumento());
        assertEquals("Logística Pacífico SAC", res.getRazonSocial());
        assertEquals("PROSPECTO", res.getEstadoCliente());
        assertTrue(res.getEstadoActivo());
    }

    @Test
    @DisplayName("Excepción: crear cliente con número de documento duplicado lanza BadRequestException")
    @PruebaDocente(codigo="PU02", tipo="Unitaria", herramientas="JUnit Jupiter 6 + Mockito (MockitoExtension, @Mock, @InjectMocks); Maven Surefire", objetivo="Mockito simula que existe el documento; el servicio lanza BadRequestException y nunca llama a save.")
    void crear_documentoDuplicado_lanzaBadRequest() {
        ClienteRequestDTO dto = ClienteRequestDTO.builder()
                .numeroDocumento("20100000001")
                .razonSocial("Duplicado SAC")
                .correoContacto("dup@test.pe")
                .build();

        when(clienteRepository.existsByNumeroDocumento("20100000001")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> clienteService.crear(dto));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Normal: actualizar cliente con mismo RUC permite modificar datos sin fallar por duplicado")
    void actualizar_mismoDocumento_actualizaCorrectamente() {
        Cliente existente = Cliente.builder()
                .idCliente(5)
                .tipoDocumento("RUC")
                .numeroDocumento("20555555555")
                .razonSocial("Antigua SAC")
                .correoContacto("old@test.pe")
                .estadoCliente("PROSPECTO")
                .estadoActivo(true)
                .build();

        ClienteRequestDTO dto = ClienteRequestDTO.builder()
                .tipoDocumento("ruc")
                .numeroDocumento("20555555555")
                .razonSocial("Nueva Razón SAC")
                .correoContacto("new@test.pe")
                .estadoCliente("cliente_activo")
                .estadoActivo(true)
                .build();

        when(clienteRepository.findById(5)).thenReturn(Optional.of(existente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteResponseDTO res = clienteService.actualizar(5, dto);

        assertEquals("Nueva Razón SAC", res.getRazonSocial());
        assertEquals("CLIENTE_ACTIVO", res.getEstadoCliente());
        verify(clienteRepository, never()).existsByNumeroDocumento(anyString());
    }

    @Test
    @DisplayName("Excepción: actualizar cliente cambiando a un RUC que ya pertenece a otro cliente lanza BadRequestException")
    void actualizar_cambiaADocumentoExistente_lanzaBadRequest() {
        Cliente existente = Cliente.builder()
                .idCliente(5)
                .numeroDocumento("20555555555")
                .razonSocial("Empresa A")
                .correoContacto("a@test.pe")
                .build();

        ClienteRequestDTO dto = ClienteRequestDTO.builder()
                .numeroDocumento("20999999999")
                .razonSocial("Empresa A")
                .correoContacto("a@test.pe")
                .build();

        when(clienteRepository.findById(5)).thenReturn(Optional.of(existente));
        when(clienteRepository.existsByNumeroDocumento("20999999999")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> clienteService.actualizar(5, dto));
        verify(clienteRepository, never()).save(any());
    }

    @Test
    @DisplayName("Excepción: buscarPorId y buscarPorNumeroDocumento inexistentes lanzan ResourceNotFoundException")
    void buscar_inexistentes_lanzanResourceNotFound() {
        when(clienteRepository.findById(999)).thenReturn(Optional.empty());
        when(clienteRepository.findByNumeroDocumento("00000000000")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> clienteService.buscarPorId(999));
        assertThrows(ResourceNotFoundException.class, () -> clienteService.buscarPorNumeroDocumento("00000000000"));
    }

    @Test
    @DisplayName("Límite / Nulo: buscarPorTermino con cadena nula o en blanco delega a listarTodos o listarActivos")
    void buscarPorTermino_nuloOBlanco_delegaCorrectamente() {
        Cliente c = Cliente.builder().idCliente(1).numeroDocumento("20111111111").razonSocial("Alpha SAC").estadoActivo(true).build();
        when(clienteRepository.findByEstadoActivo(true)).thenReturn(List.of(c));
        when(clienteRepository.findAll()).thenReturn(List.of(c));

        List<ClienteResponseDTO> activos = clienteService.buscarPorTermino("   ", true);
        List<ClienteResponseDTO> todos = clienteService.buscarPorTermino(null, null);

        assertEquals(1, activos.size());
        assertEquals(1, todos.size());
        verify(clienteRepository).findByEstadoActivo(true);
        verify(clienteRepository).findAll();
    }

    @Test
    @DisplayName("Normal: eliminar realiza borrado lógico cambiando estadoActivo a false")
    void eliminar_clienteExistente_desactivaLogicamente() {
        Cliente existente = Cliente.builder().idCliente(8).estadoActivo(true).build();
        when(clienteRepository.findById(8)).thenReturn(Optional.of(existente));

        clienteService.eliminar(8);

        assertFalse(existente.getEstadoActivo());
        verify(clienteRepository).save(existente);
    }
}
