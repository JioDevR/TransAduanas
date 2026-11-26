package pe.sga.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.sga.entity.*;
import pe.sga.entity.enums.EstadoPagoCotizacion;
import pe.sga.entity.enums.RolUsuario;
import pe.sga.entity.enums.TipoAbonoCotizacion;
import pe.sga.repository.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unitaria")
class DataInitializerTest {

    @Mock private SedeRepository sedeRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CatalogoPuertoRepository puertoRepository;
    @Mock private CatalogoTipoCargaRepository tipoCargaRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private UsuarioClienteRepository usuarioClienteRepository;
    @Mock private CotizacionRepository cotizacionRepository;
    @Mock private CotizacionPagoRepository cotizacionPagoRepository;
    @Mock private ExpedienteRepository expedienteRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void run_enBaseVacia_creaCatalogosCotizacionesPagadasYExpedientesExternosCoherentes() {
        when(sedeRepository.findAll()).thenReturn(List.of());
        when(sedeRepository.save(any(Sede.class))).thenAnswer(i -> i.getArgument(0));

        when(puertoRepository.count()).thenReturn(0L);
        when(puertoRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        when(tipoCargaRepository.count()).thenReturn(0L);
        when(tipoCargaRepository.saveAll(anyList())).thenAnswer(i -> i.getArgument(0));

        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(usuarioRepository.existsByUsername("admin")).thenReturn(false);
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(i -> i.getArgument(0));

        Usuario admin = Usuario.builder().idUsuario(1).username("admin").rol(RolUsuario.ADMINISTRADOR_TI).build();
        when(usuarioRepository.findByUsername(anyString())).thenAnswer(inv -> {
            String username = inv.getArgument(0);
            if ("admin".equals(username)) return Optional.of(admin);
            return Optional.empty();
        });

        List<Cliente> clientesGuardados = new ArrayList<>();
        when(clienteRepository.findByNumeroDocumento(anyString())).thenAnswer(inv -> {
            String ruc = inv.getArgument(0);
            return clientesGuardados.stream().filter(c -> ruc.equals(c.getNumeroDocumento())).findFirst();
        });
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setIdCliente(clientesGuardados.size() + 1);
            clientesGuardados.add(c);
            return c;
        });
        when(usuarioClienteRepository.findByCliente_IdCliente(anyInt())).thenReturn(Optional.empty());
        when(usuarioClienteRepository.existsByUsername(anyString())).thenReturn(false);

        when(expedienteRepository.count()).thenReturn(0L);
        when(expedienteRepository.findByCodigoExpediente(anyString())).thenReturn(Optional.empty());
        when(cotizacionRepository.findByCodigoCotizacion(anyString())).thenReturn(Optional.empty());
        when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(i -> i.getArgument(0));
        when(cotizacionPagoRepository.existsByCodigoPago(anyString())).thenReturn(false);
        when(cotizacionPagoRepository.save(any(CotizacionPago.class))).thenAnswer(i -> i.getArgument(0));
        when(expedienteRepository.save(any(Expediente.class))).thenAnswer(i -> i.getArgument(0));

        dataInitializer.run();

        ArgumentCaptor<Sede> sedeCaptor = ArgumentCaptor.forClass(Sede.class);
        verify(sedeRepository).save(sedeCaptor.capture());
        assertEquals("SED-CORP-01", sedeCaptor.getValue().getCodigoSede());
        assertNotEquals("SED-PRUEBA", sedeCaptor.getValue().getCodigoSede());

        ArgumentCaptor<Cotizacion> cotCaptor = ArgumentCaptor.forClass(Cotizacion.class);
        verify(cotizacionRepository, times(4)).save(cotCaptor.capture());
        for (Cotizacion cot : cotCaptor.getAllValues()) {
            assertEquals(EstadoPagoCotizacion.PAGADO_TOTAL.name(), cot.getEstadoPago());
            assertTrue(cot.getValorFob().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(cot.getTotalCotizado().compareTo(BigDecimal.ZERO) > 0);
            assertEquals(0, cot.getSaldoPendiente().compareTo(BigDecimal.ZERO));
            assertEquals(0, cot.getTotalAbonado().compareTo(cot.getTotalCotizado()));
        }

        ArgumentCaptor<CotizacionPago> pagoCaptor = ArgumentCaptor.forClass(CotizacionPago.class);
        verify(cotizacionPagoRepository, times(4)).save(pagoCaptor.capture());
        for (CotizacionPago pago : pagoCaptor.getAllValues()) {
            assertEquals(EstadoPagoCotizacion.APROBADO, pago.getEstadoPago());
            assertEquals(TipoAbonoCotizacion.PAGO_TOTAL, pago.getTipoAbono());
        }

        ArgumentCaptor<Expediente> expCaptor = ArgumentCaptor.forClass(Expediente.class);
        verify(expedienteRepository, times(4)).save(expCaptor.capture());
        List<Expediente> expedientes = expCaptor.getAllValues();
        assertTrue(expedientes.get(0).getTransporteSolicitado());
        assertEquals("RECOJO_EXPORTACION", expedientes.get(0).getTransporteTipoTraslado());
        assertEquals("Callao", expedientes.get(0).getTransporteDistritoLlegada());
        assertEquals("RECOJO_EXPORTACION", expedientes.get(1).getTransporteTipoTraslado());
        assertEquals("ENTREGA_IMPORTACION", expedientes.get(2).getTransporteTipoTraslado());
        assertEquals("ENTREGA_IMPORTACION", expedientes.get(3).getTransporteTipoTraslado());
    }

    @Test
    void run_conDatosSeedExistentes_noDuplicaRegistros() {
        Sede sedeExistente = Sede.builder().idSede(1).codigoSede("SED-CORP-01").nombre("Sede Central Corporativa").build();
        Cliente camposol = Cliente.builder().idCliente(1).numeroDocumento("20370146994").build();
        Cliente lg = Cliente.builder().idCliente(22).numeroDocumento("20511894520").build();

        when(sedeRepository.findAll()).thenReturn(List.of(sedeExistente));
        when(puertoRepository.count()).thenReturn(80L);
        when(tipoCargaRepository.count()).thenReturn(17L);
        when(usuarioRepository.existsByUsername("admin")).thenReturn(true);
        when(clienteRepository.findByNumeroDocumento("20370146994")).thenReturn(Optional.of(camposol));
        when(clienteRepository.findByNumeroDocumento("20511894520")).thenReturn(Optional.of(lg));
        when(usuarioClienteRepository.findByCliente_IdCliente(1)).thenReturn(Optional.of(new UsuarioCliente()));
        when(usuarioClienteRepository.findByCliente_IdCliente(22)).thenReturn(Optional.of(new UsuarioCliente()));
        when(expedienteRepository.count()).thenReturn(4L);

        dataInitializer.run();

        verify(sedeRepository, never()).save(any());
        verify(puertoRepository, never()).saveAll(any());
        verify(tipoCargaRepository, never()).saveAll(any());
        verify(usuarioRepository, never()).save(any());
        verify(cotizacionRepository, never()).save(any());
        verify(cotizacionPagoRepository, never()).save(any());
        verify(expedienteRepository, never()).save(any());
    }
}
