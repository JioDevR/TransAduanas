package pe.sga.config;

import org.junit.jupiter.api.Test;
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
class DataInitializerTest {

    @Mock private SedeRepository sedeRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private CatalogoPuertoRepository puertoRepository;
    @Mock private CatalogoTipoCargaRepository tipoCargaRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private UsuarioClienteRepository usuarioClienteRepository;
    @Mock private UnidadTransporteRepository unidadTransporteRepository;
    @Mock private ConductorRepository conductorRepository;
    @Mock private CotizacionRepository cotizacionRepository;
    @Mock private CotizacionPagoRepository cotizacionPagoRepository;
    @Mock private ExpedienteRepository expedienteRepository;
    @Mock private DespachoTransporteRepository despachoTransporteRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private DataInitializer dataInitializer;

    @Test
    void run_enBaseVacia_creaCatalogosConductoresCotizacionesPagadasYDespachosCoherentes() {
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
        Usuario userOsanchez = Usuario.builder().idUsuario(57).username("osanchez").rol(RolUsuario.SUPERVISOR_TRANSPORTE).build();
        when(usuarioRepository.findByUsername(anyString())).thenAnswer(inv -> {
            String username = inv.getArgument(0);
            if ("admin".equals(username)) return Optional.of(admin);
            if ("osanchez".equals(username)) return Optional.of(userOsanchez);
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

        List<UnidadTransporte> unidadesGuardadas = new ArrayList<>();
        when(unidadTransporteRepository.count()).thenReturn(0L);
        when(unidadTransporteRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<UnidadTransporte> lista = inv.getArgument(0);
            unidadesGuardadas.addAll(lista);
            return lista;
        });
        when(unidadTransporteRepository.findAll()).thenReturn(unidadesGuardadas);
        when(unidadTransporteRepository.findByPlacaTracto(anyString())).thenAnswer(inv -> {
            String placa = inv.getArgument(0);
            return unidadesGuardadas.stream().filter(u -> placa.equals(u.getPlacaTracto())).findFirst();
        });

        List<Conductor> conductoresGuardados = new ArrayList<>();
        when(conductorRepository.count()).thenReturn(0L);
        when(conductorRepository.saveAll(anyList())).thenAnswer(inv -> {
            List<Conductor> lista = inv.getArgument(0);
            conductoresGuardados.addAll(lista);
            return lista;
        });
        when(conductorRepository.findAll()).thenReturn(conductoresGuardados);
        when(conductorRepository.findByNumeroLicencia(anyString())).thenAnswer(inv -> {
            String lic = inv.getArgument(0);
            return conductoresGuardados.stream().filter(c -> lic.equals(c.getNumeroLicencia())).findFirst();
        });

        when(despachoTransporteRepository.count()).thenReturn(0L);
        when(expedienteRepository.findByCodigoExpediente(anyString())).thenReturn(Optional.empty());
        when(cotizacionRepository.findByCodigoCotizacion(anyString())).thenReturn(Optional.empty());
        when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(i -> i.getArgument(0));
        when(cotizacionPagoRepository.existsByCodigoPago(anyString())).thenReturn(false);
        when(cotizacionPagoRepository.save(any(CotizacionPago.class))).thenAnswer(i -> i.getArgument(0));
        when(expedienteRepository.save(any(Expediente.class))).thenAnswer(i -> i.getArgument(0));
        when(despachoTransporteRepository.save(any(DespachoTransporte.class))).thenAnswer(i -> i.getArgument(0));

        dataInitializer.run();

        ArgumentCaptor<Sede> sedeCaptor = ArgumentCaptor.forClass(Sede.class);
        verify(sedeRepository).save(sedeCaptor.capture());
        assertEquals("SED-CORP-01", sedeCaptor.getValue().getCodigoSede());
        assertNotEquals("SED-PRUEBA", sedeCaptor.getValue().getCodigoSede());

        assertEquals(8, unidadesGuardadas.size());
        assertEquals(6, conductoresGuardados.size());
        assertTrue(conductoresGuardados.stream().noneMatch(c -> "Santillan Medina".equals(c.getApellidos())));
        Conductor oscar = conductoresGuardados.stream()
                .filter(c -> "Q60456789".equals(c.getNumeroLicencia()))
                .findFirst()
                .orElseThrow();
        assertEquals("Oscar", oscar.getNombres());
        assertEquals("Sanchez Huamanchumo", oscar.getApellidos());
        assertEquals(userOsanchez, oscar.getUsuario());

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

        ArgumentCaptor<DespachoTransporte> despachoCaptor = ArgumentCaptor.forClass(DespachoTransporte.class);
        verify(despachoTransporteRepository, times(4)).save(despachoCaptor.capture());
        List<DespachoTransporte> despachos = despachoCaptor.getAllValues();
        assertEquals("RECOJO_EXPORTACION", despachos.get(0).getTipoTraslado());
        assertEquals("Callao", despachos.get(0).getDistritoLlegada());
        assertEquals("RECOJO_EXPORTACION", despachos.get(1).getTipoTraslado());
        assertEquals("Callao", despachos.get(1).getDistritoLlegada());
        assertEquals("ENTREGA_IMPORTACION", despachos.get(2).getTipoTraslado());
        assertEquals("ENTREGA_IMPORTACION", despachos.get(3).getTipoTraslado());
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
        when(unidadTransporteRepository.count()).thenReturn(8L);
        when(conductorRepository.count()).thenReturn(6L);
        when(despachoTransporteRepository.count()).thenReturn(4L);

        dataInitializer.run();

        verify(sedeRepository, never()).save(any());
        verify(puertoRepository, never()).saveAll(any());
        verify(tipoCargaRepository, never()).saveAll(any());
        verify(usuarioRepository, never()).save(any());
        verify(unidadTransporteRepository, never()).saveAll(any());
        verify(conductorRepository, never()).saveAll(any());
        verify(cotizacionRepository, never()).save(any());
        verify(cotizacionPagoRepository, never()).save(any());
        verify(expedienteRepository, never()).save(any());
        verify(despachoTransporteRepository, never()).save(any());
    }
}
