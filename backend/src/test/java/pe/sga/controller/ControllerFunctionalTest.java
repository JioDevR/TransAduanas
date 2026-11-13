package pe.sga.controller;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import pe.sga.service.*;
import pe.sga.dto.request.*;
import pe.sga.dto.response.*;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;
import pe.sga.entity.Usuario;
class ControllerFunctionalTest {
@Test void CP01_health(){var r=new HealthController().checkHealth(); assertEquals(200,r.getStatusCode().value()); assertEquals("UP",r.getBody().getData().get("status")); assertTrue(r.getBody().isSuccess());}
@Test void CP02_login(){var s=mock(IUsuarioService.class); var d=new LoginRequestDTO(); var o=new AuthResponseDTO(); when(s.autenticar(d)).thenReturn(o);var r=new AuthController(s).login(d);assertSame(o,r.getBody().getData());verify(s).autenticar(d);}
@Test void CP03_meSinPrincipal(){var s=mock(IUsuarioService.class);assertThrows(AccessDeniedException.class,()->new AuthController(s).me(null));verifyNoInteractions(s);}
@Test void CP04_filtroCliente(){var s=mock(IClienteService.class);when(s.listarTodos()).thenReturn(List.of());var r=new ClienteController(s).listar(false,null,null,null);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().getData().isEmpty());verify(s).listarTodos();verifyNoMoreInteractions(s);}
@Test void CP05_filtroCliente(){var s=mock(IClienteService.class);when(s.listarActivos()).thenReturn(List.of());var r=new ClienteController(s).listar(true,null,null,null);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().getData().isEmpty());verify(s).listarActivos();verifyNoMoreInteractions(s);}
@Test void CP06_filtroCliente(){var s=mock(IClienteService.class);when(s.buscarPorTermino("abc",true)).thenReturn(List.of());var r=new ClienteController(s).listar(true,null,null,"abc");assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().getData().isEmpty());verify(s).buscarPorTermino("abc",true);verifyNoMoreInteractions(s);}
@Test void CP07_filtroCliente(){var s=mock(IClienteService.class);when(s.listarPorTipoProspecto(true,false)).thenReturn(List.of());var r=new ClienteController(s).listar(false,true,null,null);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().getData().isEmpty());verify(s).listarPorTipoProspecto(true,false);verifyNoMoreInteractions(s);}
@Test void CP08_filtroCliente(){var s=mock(IClienteService.class);when(s.listarPorTipoProspecto(true,false)).thenReturn(List.of());var r=new ClienteController(s).listar(false,null,"PROSPECTO",null);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().getData().isEmpty());verify(s).listarPorTipoProspecto(true,false);verifyNoMoreInteractions(s);}
@Test void CP09_filtroCliente(){var s=mock(IClienteService.class);when(s.listarTodos()).thenReturn(List.of());var r=new ClienteController(s).listar(false,null,"TODOS",null);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().getData().isEmpty());verify(s).listarTodos();verifyNoMoreInteractions(s);}
@Test void CP10_filtroCliente(){var s=mock(IClienteService.class);when(s.listarTodos()).thenReturn(List.of());var r=new ClienteController(s).listar(false,null,null,"  ");assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().getData().isEmpty());verify(s).listarTodos();verifyNoMoreInteractions(s);}
@Test void CP11_cliente11(){var s=mock(IClienteService.class);var d=new ClienteRequestDTO();var o=new ClienteResponseDTO();when(s.crear(d)).thenReturn(o);var r=new ClienteController(s).crear(d);assertEquals(201,r.getStatusCode().value());assertSame(o,r.getBody().getData());verify(s).crear(d);}
@Test void CP12_cliente12(){var s=mock(IClienteService.class);var d=new ClienteRequestDTO();var o=new ClienteResponseDTO();when(s.actualizar(7,d)).thenReturn(o);var r=new ClienteController(s).actualizar(7,d);assertEquals(200,r.getStatusCode().value());assertSame(o,r.getBody().getData());verify(s).actualizar(7,d);}
@Test void CP13_eliminarCliente(){var s=mock(IClienteService.class);var r=new ClienteController(s).eliminar(7);assertEquals(200,r.getStatusCode().value());assertNull(r.getBody().getData());verify(s).eliminar(7);}
@Test void CP14_reactivarCliente(){var s=mock(IClienteService.class);var r=new ClienteController(s).reactivar(7);assertEquals(200,r.getStatusCode().value());assertNull(r.getBody().getData());verify(s).reactivar(7);}
@Test void CP15_excepcionCliente(){var s=mock(IClienteService.class);when(s.buscarPorId(999)).thenThrow(new IllegalArgumentException("inexistente"));assertThrows(IllegalArgumentException.class,()->new ClienteController(s).buscarPorId(999));}
@Test void CP16_cotizaciones(){var s=mock(ICotizacionService.class);var usuario=new Usuario();usuario.setIdUsuario(7);when(s.listar(false,null,7,null,usuario)).thenReturn(List.of());var r=new CotizacionController(s).listar(false,null,7,null,usuario);assertEquals(200,r.getStatusCode().value());verify(s).listar(false,null,7,null,usuario);}
@Test void CP17_cotizaciones(){var s=mock(ICotizacionService.class);var usuario=new Usuario();usuario.setIdUsuario(7);when(s.listar(true,null,null,null,usuario)).thenReturn(List.of());var r=new CotizacionController(s).listar(true,null,null,null,usuario);assertEquals(200,r.getStatusCode().value());verify(s).listar(true,null,null,null,usuario);}
@Test void CP18_crearCotizacion(){var s=mock(ICotizacionService.class);var usuario=new Usuario();usuario.setIdUsuario(7);var d=new CotizacionRequestDTO();var o=new CotizacionResponseDTO();when(s.crear(d,usuario)).thenReturn(o);var r=new CotizacionController(s).crear(d,usuario);assertEquals(201,r.getStatusCode().value());assertSame(o,r.getBody().getData());verify(s).crear(d,usuario);}
@Test void CP19_anularCotizacion(){var s=mock(ICotizacionService.class);var usuario=new Usuario();usuario.setIdUsuario(7);var r=new CotizacionController(s).eliminar(7,usuario);assertEquals(200,r.getStatusCode().value());verify(s).anular(7,usuario);}
@Test void CP20_convertirProspecto(){var s=mock(IClienteService.class);var o=new ClienteResponseDTO();when(s.convertirEnCliente(7)).thenReturn(o);var r=new ClienteController(s).convertirEnCliente(7);assertSame(o,r.getBody().getData());verify(s).convertirEnCliente(7);}
}
