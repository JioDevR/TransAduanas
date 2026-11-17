package pe.sga.controller;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import pe.sga.service.*;
import pe.sga.dto.request.*;
import pe.sga.dto.response.*;
import pe.sga.dto.portal.*;
import pe.sga.dto.portal.admin.*;
import pe.sga.entity.*;
import pe.sga.entity.enums.*;
import pe.sga.exception.BadRequestException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.core.io.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
import java.io.*;
class AllControllersFunctionalTest {
private Usuario interno(){var u=new Usuario();u.setIdUsuario(7);return u;}
private UsuarioCliente externo(){var c=new Cliente();c.setIdCliente(11);var u=new UsuarioCliente();u.setCliente(c);u.setIdUsuarioCliente(13);return u;}
@Test void UC001_CatalogoMercanciaController_buscarPorId() throws Exception {
var mercanciaService=mock(ICatalogoMercanciaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoMercanciaController(mercanciaService,reporteJasperService);
var id=7;
var expected=new CatalogoMercanciaResponseDTO();when(mercanciaService.buscarPorId(id)).thenReturn(expected);var r=ctl.buscarPorId(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(mercanciaService).buscarPorId(id);
}

@Test void UC002_CatalogoMercanciaController_buscarPorCodigo() throws Exception {
var mercanciaService=mock(ICatalogoMercanciaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoMercanciaController(mercanciaService,reporteJasperService);
var codigo="QA";
var expected=new CatalogoMercanciaResponseDTO();when(mercanciaService.buscarPorCodigo(codigo)).thenReturn(expected);var r=ctl.buscarPorCodigo(codigo);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(mercanciaService).buscarPorCodigo(codigo);
}

@Test void UC003_CatalogoMercanciaController_crear() throws Exception {
var mercanciaService=mock(ICatalogoMercanciaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoMercanciaController(mercanciaService,reporteJasperService);
var dto=new CatalogoMercanciaRequestDTO();
var expected=new CatalogoMercanciaResponseDTO();when(mercanciaService.crear(dto)).thenReturn(expected);var r=ctl.crear(dto);assertEquals(201,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(mercanciaService).crear(dto);
}

@Test void UC004_CatalogoMercanciaController_actualizar() throws Exception {
var mercanciaService=mock(ICatalogoMercanciaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoMercanciaController(mercanciaService,reporteJasperService);
var id=7;
var dto=new CatalogoMercanciaRequestDTO();
var expected=new CatalogoMercanciaResponseDTO();when(mercanciaService.actualizar(id, dto)).thenReturn(expected);var r=ctl.actualizar(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(mercanciaService).actualizar(id, dto);
}

@Test void UC005_CatalogoMercanciaController_eliminarPorEstado() throws Exception {
var mercanciaService=mock(ICatalogoMercanciaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoMercanciaController(mercanciaService,reporteJasperService);
var id=7;
var r=ctl.eliminarPorEstado(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(mercanciaService).eliminarPorEstado(id);
}

@Test void UC006_CatalogoMercanciaController_reactivar() throws Exception {
var mercanciaService=mock(ICatalogoMercanciaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoMercanciaController(mercanciaService,reporteJasperService);
var id=7;
var r=ctl.reactivar(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(mercanciaService).reactivar(id);
}

@Test void UC007_CatalogoPuertoController_buscarPorId() throws Exception {
var puertoService=mock(ICatalogoPuertoService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoPuertoController(puertoService,reporteJasperService);
var id=7;
var expected=new CatalogoPuertoResponseDTO();when(puertoService.buscarPorId(id)).thenReturn(expected);var r=ctl.buscarPorId(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(puertoService).buscarPorId(id);
}

@Test void UC008_CatalogoPuertoController_crear() throws Exception {
var puertoService=mock(ICatalogoPuertoService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoPuertoController(puertoService,reporteJasperService);
var dto=new CatalogoPuertoDTO();
var expected=new CatalogoPuertoResponseDTO();when(puertoService.crear(dto)).thenReturn(expected);var r=ctl.crear(dto);assertEquals(201,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(puertoService).crear(dto);
}

@Test void UC009_CatalogoPuertoController_actualizar() throws Exception {
var puertoService=mock(ICatalogoPuertoService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoPuertoController(puertoService,reporteJasperService);
var id=7;
var dto=new CatalogoPuertoDTO();
var expected=new CatalogoPuertoResponseDTO();when(puertoService.actualizar(id, dto)).thenReturn(expected);var r=ctl.actualizar(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(puertoService).actualizar(id, dto);
}

@Test void UC010_CatalogoPuertoController_eliminarPorEstado() throws Exception {
var puertoService=mock(ICatalogoPuertoService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoPuertoController(puertoService,reporteJasperService);
var id=7;
var r=ctl.eliminarPorEstado(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(puertoService).eliminarPorEstado(id);
}

@Test void UC011_CatalogoPuertoController_reactivar() throws Exception {
var puertoService=mock(ICatalogoPuertoService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoPuertoController(puertoService,reporteJasperService);
var id=7;
var r=ctl.reactivar(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(puertoService).reactivar(id);
}

@Test void UC012_CatalogoTipoCargaController_buscarPorId() throws Exception {
var tipoCargaService=mock(ICatalogoTipoCargaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoTipoCargaController(tipoCargaService,reporteJasperService);
var id=7;
var expected=new CatalogoTipoCargaResponseDTO();when(tipoCargaService.buscarPorId(id)).thenReturn(expected);var r=ctl.buscarPorId(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(tipoCargaService).buscarPorId(id);
}

@Test void UC013_CatalogoTipoCargaController_crear() throws Exception {
var tipoCargaService=mock(ICatalogoTipoCargaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoTipoCargaController(tipoCargaService,reporteJasperService);
var dto=new CatalogoTipoCargaDTO();
var expected=new CatalogoTipoCargaResponseDTO();when(tipoCargaService.crear(dto)).thenReturn(expected);var r=ctl.crear(dto);assertEquals(201,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(tipoCargaService).crear(dto);
}

@Test void UC014_CatalogoTipoCargaController_actualizar() throws Exception {
var tipoCargaService=mock(ICatalogoTipoCargaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoTipoCargaController(tipoCargaService,reporteJasperService);
var id=7;
var dto=new CatalogoTipoCargaDTO();
var expected=new CatalogoTipoCargaResponseDTO();when(tipoCargaService.actualizar(id, dto)).thenReturn(expected);var r=ctl.actualizar(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(tipoCargaService).actualizar(id, dto);
}

@Test void UC015_CatalogoTipoCargaController_eliminarPorEstado() throws Exception {
var tipoCargaService=mock(ICatalogoTipoCargaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoTipoCargaController(tipoCargaService,reporteJasperService);
var id=7;
var r=ctl.eliminarPorEstado(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(tipoCargaService).eliminarPorEstado(id);
}

@Test void UC016_CatalogoTipoCargaController_reactivar() throws Exception {
var tipoCargaService=mock(ICatalogoTipoCargaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoTipoCargaController(tipoCargaService,reporteJasperService);
var id=7;
var r=ctl.reactivar(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(tipoCargaService).reactivar(id);
}

@Test void UC017_ClienteController_buscarPorId() throws Exception {
var clienteService=mock(IClienteService.class);
var ctl=new ClienteController(clienteService);
var id=7;
var expected=new ClienteResponseDTO();when(clienteService.buscarPorId(id)).thenReturn(expected);var r=ctl.buscarPorId(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(clienteService).buscarPorId(id);
}

@Test void UC018_ClienteController_buscarPorNumeroDocumento() throws Exception {
var clienteService=mock(IClienteService.class);
var ctl=new ClienteController(clienteService);
var numeroDocumento="QA";
var expected=new ClienteResponseDTO();when(clienteService.buscarPorNumeroDocumento(numeroDocumento)).thenReturn(expected);var r=ctl.buscarPorNumeroDocumento(numeroDocumento);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(clienteService).buscarPorNumeroDocumento(numeroDocumento);
}

@Test void UC019_ClienteController_crear() throws Exception {
var clienteService=mock(IClienteService.class);
var ctl=new ClienteController(clienteService);
var dto=new ClienteRequestDTO();
var expected=new ClienteResponseDTO();when(clienteService.crear(dto)).thenReturn(expected);var r=ctl.crear(dto);assertEquals(201,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(clienteService).crear(dto);
}

@Test void UC020_ClienteController_actualizar() throws Exception {
var clienteService=mock(IClienteService.class);
var ctl=new ClienteController(clienteService);
var id=7;
var dto=new ClienteRequestDTO();
var expected=new ClienteResponseDTO();when(clienteService.actualizar(id, dto)).thenReturn(expected);var r=ctl.actualizar(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(clienteService).actualizar(id, dto);
}

@Test void UC021_ClienteController_eliminar() throws Exception {
var clienteService=mock(IClienteService.class);
var ctl=new ClienteController(clienteService);
var id=7;
var r=ctl.eliminar(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(clienteService).eliminar(id);
}

@Test void UC022_ClienteController_reactivar() throws Exception {
var clienteService=mock(IClienteService.class);
var ctl=new ClienteController(clienteService);
var id=7;
var r=ctl.reactivar(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(clienteService).reactivar(id);
}

@Test void UC023_ClienteController_convertirEnCliente() throws Exception {
var clienteService=mock(IClienteService.class);
var ctl=new ClienteController(clienteService);
var id=7;
var expected=new ClienteResponseDTO();when(clienteService.convertirEnCliente(id)).thenReturn(expected);var r=ctl.convertirEnCliente(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(clienteService).convertirEnCliente(id);
}

@Test void UC024_ConductorController_buscarPorId() throws Exception {
var conductorService=mock(IConductorService.class);
var ctl=new ConductorController(conductorService);
var id=7;
var expected=new ConductorResponseDTO();when(conductorService.buscarPorId(id)).thenReturn(expected);var r=ctl.buscarPorId(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(conductorService).buscarPorId(id);
}

@Test void UC025_ConductorController_buscarPorLicencia() throws Exception {
var conductorService=mock(IConductorService.class);
var ctl=new ConductorController(conductorService);
var numeroLicencia="QA";
var expected=new ConductorResponseDTO();when(conductorService.buscarPorLicencia(numeroLicencia)).thenReturn(expected);var r=ctl.buscarPorLicencia(numeroLicencia);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(conductorService).buscarPorLicencia(numeroLicencia);
}

@Test void UC026_ConductorController_crear() throws Exception {
var conductorService=mock(IConductorService.class);
var ctl=new ConductorController(conductorService);
var dto=new ConductorRequestDTO();
var expected=new ConductorResponseDTO();when(conductorService.crear(dto)).thenReturn(expected);var r=ctl.crear(dto);assertEquals(201,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(conductorService).crear(dto);
}

@Test void UC027_ConductorController_actualizar() throws Exception {
var conductorService=mock(IConductorService.class);
var ctl=new ConductorController(conductorService);
var id=7;
var dto=new ConductorRequestDTO();
var expected=new ConductorResponseDTO();when(conductorService.actualizar(id, dto)).thenReturn(expected);var r=ctl.actualizar(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(conductorService).actualizar(id, dto);
}

@Test void UC028_ConductorController_eliminar() throws Exception {
var conductorService=mock(IConductorService.class);
var ctl=new ConductorController(conductorService);
var id=7;
var r=ctl.eliminar(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(conductorService).eliminar(id);
}

@Test void UC029_ConductorController_reactivar() throws Exception {
var conductorService=mock(IConductorService.class);
var ctl=new ConductorController(conductorService);
var id=7;
var r=ctl.reactivar(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(conductorService).reactivar(id);
}

@Test void UC030_ControlAlmacenController_buscarPorId() throws Exception {
var controlAlmacenService=mock(IControlAlmacenService.class);
var ctl=new ControlAlmacenController(controlAlmacenService);
var id=7;
var expected=new ControlAlmacenResponseDTO();when(controlAlmacenService.buscarPorId(id)).thenReturn(expected);var r=ctl.buscarPorId(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(controlAlmacenService).buscarPorId(id);
}

@Test void UC031_ControlAlmacenController_buscarPorExpediente() throws Exception {
var controlAlmacenService=mock(IControlAlmacenService.class);
var ctl=new ControlAlmacenController(controlAlmacenService);
var idExpediente=7;
var expected=new ControlAlmacenResponseDTO();when(controlAlmacenService.buscarPorExpediente(idExpediente)).thenReturn(expected);var r=ctl.buscarPorExpediente(idExpediente);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(controlAlmacenService).buscarPorExpediente(idExpediente);
}

@Test void UC032_ControlAlmacenController_registrarIngreso() throws Exception {
var controlAlmacenService=mock(IControlAlmacenService.class);
var ctl=new ControlAlmacenController(controlAlmacenService);
var dto=new ControlAlmacenRequestDTO();
var expected=new ControlAlmacenResponseDTO();when(controlAlmacenService.registrarIngreso(dto)).thenReturn(expected);var r=ctl.registrarIngreso(dto);assertEquals(201,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(controlAlmacenService).registrarIngreso(dto);
}

@Test void UC033_ControlAlmacenController_actualizar() throws Exception {
var controlAlmacenService=mock(IControlAlmacenService.class);
var ctl=new ControlAlmacenController(controlAlmacenService);
var id=7;
var dto=new ControlAlmacenRequestDTO();
var expected=new ControlAlmacenResponseDTO();when(controlAlmacenService.actualizar(id, dto)).thenReturn(expected);var r=ctl.actualizar(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(controlAlmacenService).actualizar(id, dto);
}

@Test void UC034_ControlAlmacenController_autorizarSalida() throws Exception {
var controlAlmacenService=mock(IControlAlmacenService.class);
var ctl=new ControlAlmacenController(controlAlmacenService);
var id=7;
var dto=new ControlAlmacenSalidaDTO();
var expected=new ControlAlmacenResponseDTO();when(controlAlmacenService.autorizarSalida(id, dto)).thenReturn(expected);var r=ctl.autorizarSalida(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(controlAlmacenService).autorizarSalida(id, dto);
}

@Test void UC035_ControlAlmacenController_eliminar() throws Exception {
var controlAlmacenService=mock(IControlAlmacenService.class);
var ctl=new ControlAlmacenController(controlAlmacenService);
var id=7;
var r=ctl.eliminar(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(controlAlmacenService).anular(id);
}

@Test void UC036_CotizacionController_buscarPorId() throws Exception {
var cotizacionService=mock(ICotizacionService.class);
var ctl=new CotizacionController(cotizacionService);
var usuario=interno();
var id=7;
var expected=new CotizacionResponseDTO();when(cotizacionService.buscarPorId(id,usuario)).thenReturn(expected);var r=ctl.buscarPorId(id,usuario);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(cotizacionService).buscarPorId(id,usuario);
}

@Test void UC037_CotizacionController_buscarPorCodigo() throws Exception {
var cotizacionService=mock(ICotizacionService.class);
var ctl=new CotizacionController(cotizacionService);
var usuario=interno();
var codigo="QA";
var expected=new CotizacionResponseDTO();when(cotizacionService.buscarPorCodigo(codigo,usuario)).thenReturn(expected);var r=ctl.buscarPorCodigo(codigo,usuario);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(cotizacionService).buscarPorCodigo(codigo,usuario);
}

@Test void UC038_CotizacionController_crear() throws Exception {
var cotizacionService=mock(ICotizacionService.class);
var ctl=new CotizacionController(cotizacionService);
var usuario=interno();
var dto=new CotizacionRequestDTO();
var expected=new CotizacionResponseDTO();when(cotizacionService.crear(dto,usuario)).thenReturn(expected);var r=ctl.crear(dto,usuario);assertEquals(201,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(cotizacionService).crear(dto,usuario);
}

@Test void UC039_CotizacionController_actualizar() throws Exception {
var cotizacionService=mock(ICotizacionService.class);
var ctl=new CotizacionController(cotizacionService);
var usuario=interno();
var id=7;
var dto=new CotizacionRequestDTO();
var expected=new CotizacionResponseDTO();when(cotizacionService.actualizar(id, dto,usuario)).thenReturn(expected);var r=ctl.actualizar(id,dto,usuario);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(cotizacionService).actualizar(id, dto,usuario);
}

@Test void UC040_CotizacionController_revisarTecnicamente() throws Exception {
var cotizacionService=mock(ICotizacionService.class);
var ctl=new CotizacionController(cotizacionService);
var id=7;
var dto=new CotizacionRevisionDTO();
var expected=new CotizacionResponseDTO();when(cotizacionService.revisarTecnicamente(id, dto)).thenReturn(expected);var r=ctl.revisarTecnicamente(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(cotizacionService).revisarTecnicamente(id, dto);
}

@Test void UC041_CotizacionController_cambiarEstado() throws Exception {
var cotizacionService=mock(ICotizacionService.class);
var ctl=new CotizacionController(cotizacionService);
var id=7;
var dto=new CotizacionEstadoDTO();
var expected=new CotizacionResponseDTO();when(cotizacionService.cambiarEstado(id, dto.getEstado(), dto.getMotivoObservacion())).thenReturn(expected);var r=ctl.cambiarEstado(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(cotizacionService).cambiarEstado(id, dto.getEstado(), dto.getMotivoObservacion());
}

@Test void UC042_CotizacionController_eliminar() throws Exception {
var cotizacionService=mock(ICotizacionService.class);
var ctl=new CotizacionController(cotizacionService);
var usuario=interno();
var id=7;
var r=ctl.eliminar(id,usuario);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(cotizacionService).anular(id,usuario);
}

@Test void UC043_CotizacionPagoController_listarPorCotizacion() throws Exception {
var cotizacionPagoService=mock(ICotizacionPagoService.class);
var ctl=new CotizacionPagoController(cotizacionPagoService);
var idCotizacion=7;var usuario=interno();
var expected=List.of(new CotizacionPagoResponseDTO());when(cotizacionPagoService.listarPorCotizacion(idCotizacion,usuario)).thenReturn(expected);var r=ctl.listarPorCotizacion(idCotizacion,usuario);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(cotizacionPagoService).listarPorCotizacion(idCotizacion,usuario);
}

@Test void UC044_CotizacionPagoController_listarAnticiposPendientes() throws Exception {
var cotizacionPagoService=mock(ICotizacionPagoService.class);
var ctl=new CotizacionPagoController(cotizacionPagoService);

var expected=List.of(new CotizacionPagoResponseDTO());when(cotizacionPagoService.listarPorEstado(EstadoPagoCotizacion.PENDIENTE_REVISION)).thenReturn(expected);var r=ctl.listarAnticiposPendientes();assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(cotizacionPagoService).listarPorEstado(EstadoPagoCotizacion.PENDIENTE_REVISION);
}

@Test void UC045_CotizacionPagoController_buscarPorId() throws Exception {
var cotizacionPagoService=mock(ICotizacionPagoService.class);
var ctl=new CotizacionPagoController(cotizacionPagoService);
var idPago=7;
var expected=new CotizacionPagoResponseDTO();when(cotizacionPagoService.buscarPorId(idPago)).thenReturn(expected);var r=ctl.buscarPorId(idPago);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(cotizacionPagoService).buscarPorId(idPago);
}

@Test void UC046_DespachoTransporteController_buscarPorId() throws Exception {
var despachoService=mock(IDespachoTransporteService.class);
var ctl=new DespachoTransporteController(despachoService);
var id=7;
var expected=new DespachoTransporteResponseDTO();when(despachoService.buscarPorId(id)).thenReturn(expected);var r=ctl.buscarPorId(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(despachoService).buscarPorId(id);
}

@Test void UC047_DespachoTransporteController_buscarPorTracking() throws Exception {
var despachoService=mock(IDespachoTransporteService.class);
var ctl=new DespachoTransporteController(despachoService);
var codigoTracking="QA";
var expected=new DespachoTransporteResponseDTO();when(despachoService.buscarPorTracking(codigoTracking)).thenReturn(expected);var r=ctl.buscarPorTracking(codigoTracking);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(despachoService).buscarPorTracking(codigoTracking);
}

@Test void UC048_DespachoTransporteController_buscarPorExpediente() throws Exception {
var despachoService=mock(IDespachoTransporteService.class);
var ctl=new DespachoTransporteController(despachoService);
var idExpediente=7;
var expected=new DespachoTransporteResponseDTO();when(despachoService.buscarPorExpediente(idExpediente)).thenReturn(expected);var r=ctl.buscarPorExpediente(idExpediente);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(despachoService).buscarPorExpediente(idExpediente);
}

@Test void UC049_DespachoTransporteController_programar() throws Exception {
var despachoService=mock(IDespachoTransporteService.class);
var ctl=new DespachoTransporteController(despachoService);
var dto=new DespachoTransporteCrearDTO();
var expected=new DespachoTransporteResponseDTO();when(despachoService.programar(dto)).thenReturn(expected);var r=ctl.programar(dto);assertEquals(201,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(despachoService).programar(dto);
}

@Test void UC050_DespachoTransporteController_asignarRecursos() throws Exception {
var despachoService=mock(IDespachoTransporteService.class);
var ctl=new DespachoTransporteController(despachoService);
var id=7;
var dto=new DespachoAsignarRecursosDTO();
var expected=new DespachoTransporteResponseDTO();when(despachoService.asignarRecursos(id, dto)).thenReturn(expected);var r=ctl.asignarRecursos(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(despachoService).asignarRecursos(id, dto);
}

@Test void UC051_DespachoTransporteController_iniciarRuta() throws Exception {
var despachoService=mock(IDespachoTransporteService.class);
var ctl=new DespachoTransporteController(despachoService);
var id=7;
var dto=new DespachoRutaDTO();
var expected=new DespachoTransporteResponseDTO();when(despachoService.iniciarRuta(id, dto)).thenReturn(expected);var r=ctl.iniciarRuta(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(despachoService).iniciarRuta(id, dto);
}

@Test void UC052_DespachoTransporteController_confirmarEntrega() throws Exception {
var despachoService=mock(IDespachoTransporteService.class);
var ctl=new DespachoTransporteController(despachoService);
var id=7;
var dto=new DespachoEntregaDTO();
var expected=new DespachoTransporteResponseDTO();when(despachoService.confirmarEntrega(id, dto)).thenReturn(expected);var r=ctl.confirmarEntrega(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(despachoService).confirmarEntrega(id, dto);
}

@Test void UC053_DespachoTransporteController_eliminar() throws Exception {
var despachoService=mock(IDespachoTransporteService.class);
var ctl=new DespachoTransporteController(despachoService);
var id=7;
var r=ctl.eliminar(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(despachoService).anular(id);
}

@Test void UC054_ExpedienteController_buscarPorId() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var id=7;
var expected=new ExpedienteResponseDTO();when(expedienteService.buscarPorId(id)).thenReturn(expected);var r=ctl.buscarPorId(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(expedienteService).buscarPorId(id);
}

@Test void UC055_ExpedienteController_buscarPorCodigo() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var codigo="QA";
var expected=new ExpedienteResponseDTO();when(expedienteService.buscarPorCodigo(codigo)).thenReturn(expected);var r=ctl.buscarPorCodigo(codigo);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(expedienteService).buscarPorCodigo(codigo);
}

@Test void UC056_ExpedienteController_buscarPorCotizacion() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var idCotizacion=7;
var expected=new ExpedienteResponseDTO();when(expedienteService.buscarPorCotizacion(idCotizacion)).thenReturn(expected);var r=ctl.buscarPorCotizacion(idCotizacion);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(expedienteService).buscarPorCotizacion(idCotizacion);
}

@Test void UC057_ExpedienteController_crear() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var dto=new ExpedienteCrearDTO();
var expected=new ExpedienteResponseDTO();when(expedienteService.crear(dto)).thenReturn(expected);var r=ctl.crear(dto);assertEquals(201,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(expedienteService).crear(dto);
}

@Test void UC058_ExpedienteController_actualizar() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var id=7;
var dto=new ExpedienteActualizarDTO();
var expected=new ExpedienteResponseDTO();when(expedienteService.actualizar(id, dto)).thenReturn(expected);var r=ctl.actualizar(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(expedienteService).actualizar(id, dto);
}

@Test void UC059_ExpedienteController_asignarCanal() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var id=7;
var dto=new ExpedienteCanalDTO();
var expected=new ExpedienteResponseDTO();when(expedienteService.asignarCanal(id, dto)).thenReturn(expected);var r=ctl.asignarCanal(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(expedienteService).asignarCanal(id, dto);
}

@Test void UC060_ExpedienteController_registrarDiligencia() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var id=7;
var dto=new ExpedienteDiligenciaDTO();
var expected=new ExpedienteResponseDTO();when(expedienteService.registrarDiligencia(id, dto)).thenReturn(expected);var r=ctl.registrarDiligencia(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(expedienteService).registrarDiligencia(id, dto);
}

@Test void UC061_ExpedienteController_autorizarLevante() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var id=7;
var dto=new ExpedienteLevanteDTO();
var expected=new ExpedienteResponseDTO();when(expedienteService.autorizarLevante(id, dto)).thenReturn(expected);var r=ctl.autorizarLevante(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(expedienteService).autorizarLevante(id, dto);
}

@Test void UC062_ExpedienteController_solicitarRecojoExportacion() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var id=7;
var dto=new ExpedienteRecojoExportacionDTO();
var expected=new ExpedienteResponseDTO();when(expedienteService.solicitarRecojoExportacion(id, dto)).thenReturn(expected);var r=ctl.solicitarRecojoExportacion(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(expedienteService).solicitarRecojoExportacion(id, dto);
}

@Test void UC063_ExpedienteController_confirmarIngresoOrigen() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var id=7;
var dto=new ExpedienteIngresoOrigenDTO();
var expected=new ExpedienteResponseDTO();when(expedienteService.confirmarIngresoOrigen(id, dto)).thenReturn(expected);var r=ctl.confirmarIngresoOrigen(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(expedienteService).confirmarIngresoOrigen(id, dto);
}

@Test void UC064_ExpedienteController_confirmarEmbarque() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var id=7;
var dto=new ExpedienteEmbarqueExportacionDTO();
var expected=new ExpedienteResponseDTO();when(expedienteService.confirmarEmbarqueOrigen(id, dto)).thenReturn(expected);var r=ctl.confirmarEmbarque(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(expedienteService).confirmarEmbarqueOrigen(id, dto);
}

@Test void UC065_ExpedienteController_confirmarRecepcionExterior() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var id=7;
var dto=new ExpedienteRecepcionExteriorDTO();
var expected=new ExpedienteResponseDTO();when(expedienteService.confirmarRecepcionExterior(id, dto)).thenReturn(expected);var r=ctl.confirmarRecepcionExterior(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(expedienteService).confirmarRecepcionExterior(id, dto);
}

@Test void UC066_ExpedienteController_solicitarTransporte() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var id=7;
var dto=new ExpedienteSolicitudTransporteDTO();
var expected=new ExpedienteResponseDTO();when(expedienteService.solicitarTransporte(id, dto)).thenReturn(expected);var r=ctl.solicitarTransporte(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(expedienteService).solicitarTransporte(id, dto);
}

@Test void UC067_ExpedienteController_registrarRetiroDirecto() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var id=7;
var dto=new ExpedienteRetiroDirectoDTO();
var expected=new ExpedienteResponseDTO();when(expedienteService.registrarRetiroDirecto(id, dto)).thenReturn(expected);var r=ctl.registrarRetiroDirecto(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(expedienteService).registrarRetiroDirecto(id, dto);
}

@Test void UC068_ExpedienteController_cerrar() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var id=7;
var dto=new ExpedienteCerrarDTO();
var expected=new ExpedienteResponseDTO();when(expedienteService.cerrar(id, dto)).thenReturn(expected);var r=ctl.cerrar(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(expedienteService).cerrar(id, dto);
}

@Test void UC069_ExpedienteController_eliminar() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var id=7;
var r=ctl.eliminar(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(expedienteService).anular(id);
}

@Test void UC070_LiquidacionFinancieraController_obtenerMetricas() throws Exception {
var liquidacionService=mock(ILiquidacionFinancieraService.class);
var ctl=new LiquidacionFinancieraController(liquidacionService);

var expected=new FinanzasMetricasResponseDTO();when(liquidacionService.obtenerMetricas()).thenReturn(expected);var r=ctl.obtenerMetricas();assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(liquidacionService).obtenerMetricas();
}

@Test void UC071_LiquidacionFinancieraController_calcularEstimado() throws Exception {
var liquidacionService=mock(ILiquidacionFinancieraService.class);
var ctl=new LiquidacionFinancieraController(liquidacionService);
var idExpediente=7;
var expected=new LiquidacionEstimadoResponseDTO();when(liquidacionService.calcularEstimado(idExpediente)).thenReturn(expected);var r=ctl.calcularEstimado(idExpediente);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(liquidacionService).calcularEstimado(idExpediente);
}

@Test void UC072_LiquidacionFinancieraController_buscarPorId() throws Exception {
var liquidacionService=mock(ILiquidacionFinancieraService.class);
var ctl=new LiquidacionFinancieraController(liquidacionService);
var id=7;
var expected=new LiquidacionFinancieraResponseDTO();when(liquidacionService.buscarPorId(id)).thenReturn(expected);var r=ctl.buscarPorId(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(liquidacionService).buscarPorId(id);
}

@Test void UC073_LiquidacionFinancieraController_buscarPorCodigo() throws Exception {
var liquidacionService=mock(ILiquidacionFinancieraService.class);
var ctl=new LiquidacionFinancieraController(liquidacionService);
var codigo="QA";
var expected=new LiquidacionFinancieraResponseDTO();when(liquidacionService.buscarPorCodigo(codigo)).thenReturn(expected);var r=ctl.buscarPorCodigo(codigo);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(liquidacionService).buscarPorCodigo(codigo);
}

@Test void UC074_LiquidacionFinancieraController_buscarPorExpediente() throws Exception {
var liquidacionService=mock(ILiquidacionFinancieraService.class);
var ctl=new LiquidacionFinancieraController(liquidacionService);
var idExpediente=7;
var expected=new LiquidacionFinancieraResponseDTO();when(liquidacionService.buscarPorExpediente(idExpediente)).thenReturn(expected);var r=ctl.buscarPorExpediente(idExpediente);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(liquidacionService).buscarPorExpediente(idExpediente);
}

@Test void UC075_LiquidacionFinancieraController_actualizar() throws Exception {
var liquidacionService=mock(ILiquidacionFinancieraService.class);
var ctl=new LiquidacionFinancieraController(liquidacionService);
var id=7;
var dto=new LiquidacionFinancieraRequestDTO();
var expected=new LiquidacionFinancieraResponseDTO();when(liquidacionService.actualizar(id, dto)).thenReturn(expected);var r=ctl.actualizar(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(liquidacionService).actualizar(id, dto);
}

@Test void UC076_LiquidacionFinancieraController_registrarPago() throws Exception {
var liquidacionService=mock(ILiquidacionFinancieraService.class);
var ctl=new LiquidacionFinancieraController(liquidacionService);
var id=7;
var dto=new LiquidacionPagoDTO();
var expected=new LiquidacionFinancieraResponseDTO();when(liquidacionService.registrarPago(id, dto)).thenReturn(expected);var r=ctl.registrarPago(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(liquidacionService).registrarPago(id, dto);
}

@Test void UC077_LiquidacionFinancieraController_eliminar() throws Exception {
var liquidacionService=mock(ILiquidacionFinancieraService.class);
var ctl=new LiquidacionFinancieraController(liquidacionService);
var id=7;
var r=ctl.eliminar(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(liquidacionService).anular(id);
}

@Test void UC078_RolController_buscarPorId() throws Exception {
var rolService=mock(IRolService.class);
var ctl=new RolController(rolService);
var id=7;
var expected=new RolResponseDTO();when(rolService.buscarPorId(id)).thenReturn(expected);var r=ctl.buscarPorId(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(rolService).buscarPorId(id);
}

@Test void UC079_RolController_listarTodos() {
var rolService=mock(IRolService.class);
var ctl=new RolController(rolService);
var expected=List.of(new RolResponseDTO());
when(rolService.listarTodos()).thenReturn(expected);
var r=ctl.listar(false);
assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());
assertSame(expected,r.getBody().getData());verify(rolService).listarTodos();verifyNoMoreInteractions(rolService);
}

@Test void UC080_RolController_listarActivos() {
var rolService=mock(IRolService.class);
var ctl=new RolController(rolService);
var expected=List.of(new RolResponseDTO());
when(rolService.listarActivos()).thenReturn(expected);
var r=ctl.listar(true);
assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());
assertSame(expected,r.getBody().getData());verify(rolService).listarActivos();verifyNoMoreInteractions(rolService);
}

@Test void UC081_RolController_listarTodosVacio() {
var rolService=mock(IRolService.class);
when(rolService.listarTodos()).thenReturn(List.of());
var r=new RolController(rolService).listar(false);
assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().getData().isEmpty());
verify(rolService).listarTodos();verifyNoMoreInteractions(rolService);
}

@Test void UC082_RolController_listarActivosVacio() {
var rolService=mock(IRolService.class);
when(rolService.listarActivos()).thenReturn(List.of());
var r=new RolController(rolService).listar(true);
assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().getData().isEmpty());
verify(rolService).listarActivos();verifyNoMoreInteractions(rolService);
}

@Test void UC083_SedeController_buscarPorId() throws Exception {
var sedeService=mock(ISedeService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new SedeController(sedeService,reporteJasperService);
var id=7;
var expected=new SedeResponseDTO();when(sedeService.buscarPorId(id)).thenReturn(expected);var r=ctl.buscarPorId(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(sedeService).buscarPorId(id);
}

@Test void UC084_SedeController_crear() throws Exception {
var sedeService=mock(ISedeService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new SedeController(sedeService,reporteJasperService);
var dto=new SedeCreateDTO();
var expected=new SedeResponseDTO();when(sedeService.crear(dto)).thenReturn(expected);var r=ctl.crear(dto);assertEquals(201,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(sedeService).crear(dto);
}

@Test void UC085_SedeController_actualizar() throws Exception {
var sedeService=mock(ISedeService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new SedeController(sedeService,reporteJasperService);
var id=7;
var dto=new SedeUpdateDTO();
var expected=new SedeResponseDTO();when(sedeService.actualizar(id, dto)).thenReturn(expected);var r=ctl.actualizar(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(sedeService).actualizar(id, dto);
}

@Test void UC086_SedeController_eliminarPorEstado() throws Exception {
var sedeService=mock(ISedeService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new SedeController(sedeService,reporteJasperService);
var id=7;
var r=ctl.eliminarPorEstado(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(sedeService).eliminarPorEstado(id);
}

@Test void UC087_SedeController_reactivar() throws Exception {
var sedeService=mock(ISedeService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new SedeController(sedeService,reporteJasperService);
var id=7;
var r=ctl.reactivar(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(sedeService).reactivar(id);
}

@Test void UC088_UnidadTransporteController_buscarPorId() throws Exception {
var unidadService=mock(IUnidadTransporteService.class);
var ctl=new UnidadTransporteController(unidadService);
var id=7;
var expected=new UnidadTransporteResponseDTO();when(unidadService.buscarPorId(id)).thenReturn(expected);var r=ctl.buscarPorId(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(unidadService).buscarPorId(id);
}

@Test void UC089_UnidadTransporteController_buscarPorPlaca() throws Exception {
var unidadService=mock(IUnidadTransporteService.class);
var ctl=new UnidadTransporteController(unidadService);
var placaTracto="QA";
var expected=new UnidadTransporteResponseDTO();when(unidadService.buscarPorPlaca(placaTracto)).thenReturn(expected);var r=ctl.buscarPorPlaca(placaTracto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(unidadService).buscarPorPlaca(placaTracto);
}

@Test void UC090_UnidadTransporteController_crear() throws Exception {
var unidadService=mock(IUnidadTransporteService.class);
var ctl=new UnidadTransporteController(unidadService);
var dto=new UnidadTransporteRequestDTO();
var expected=new UnidadTransporteResponseDTO();when(unidadService.crear(dto)).thenReturn(expected);var r=ctl.crear(dto);assertEquals(201,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(unidadService).crear(dto);
}

@Test void UC091_UnidadTransporteController_actualizar() throws Exception {
var unidadService=mock(IUnidadTransporteService.class);
var ctl=new UnidadTransporteController(unidadService);
var id=7;
var dto=new UnidadTransporteRequestDTO();
var expected=new UnidadTransporteResponseDTO();when(unidadService.actualizar(id, dto)).thenReturn(expected);var r=ctl.actualizar(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(unidadService).actualizar(id, dto);
}

@Test void UC092_UnidadTransporteController_eliminar() throws Exception {
var unidadService=mock(IUnidadTransporteService.class);
var ctl=new UnidadTransporteController(unidadService);
var id=7;
var r=ctl.eliminar(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(unidadService).eliminar(id);
}

@Test void UC093_UnidadTransporteController_reactivar() throws Exception {
var unidadService=mock(IUnidadTransporteService.class);
var ctl=new UnidadTransporteController(unidadService);
var id=7;
var r=ctl.reactivar(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(unidadService).reactivar(id);
}

@Test void UC094_UsuarioClienteAdminController_listar() throws Exception {
var usuarioClienteService=mock(IUsuarioClienteService.class);
var ctl=new UsuarioClienteAdminController(usuarioClienteService,mock(IReporteJasperService.class));
var idCliente=7;
var soloActivos=false;
var buscar="QA";
var expected=List.of(new UsuarioClienteResponseDTO());when(usuarioClienteService.listar(idCliente, soloActivos, buscar)).thenReturn(expected);var r=ctl.listar(idCliente,soloActivos,buscar);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(usuarioClienteService).listar(idCliente, soloActivos, buscar);
}

@Test void UC095_UsuarioClienteAdminController_buscarPorId() throws Exception {
var usuarioClienteService=mock(IUsuarioClienteService.class);
var ctl=new UsuarioClienteAdminController(usuarioClienteService,mock(IReporteJasperService.class));
var id=7;
var expected=new UsuarioClienteResponseDTO();when(usuarioClienteService.buscarPorId(id)).thenReturn(expected);var r=ctl.buscarPorId(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(usuarioClienteService).buscarPorId(id);
}

@Test void UC096_UsuarioClienteAdminController_crear() throws Exception {
var usuarioClienteService=mock(IUsuarioClienteService.class);
var ctl=new UsuarioClienteAdminController(usuarioClienteService,mock(IReporteJasperService.class));
var dto=new UsuarioClienteCreateDTO();
var expected=new UsuarioClienteResponseDTO();when(usuarioClienteService.crear(dto)).thenReturn(expected);var r=ctl.crear(dto);assertEquals(201,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(usuarioClienteService).crear(dto);
}

@Test void UC097_UsuarioClienteAdminController_actualizar() throws Exception {
var usuarioClienteService=mock(IUsuarioClienteService.class);
var ctl=new UsuarioClienteAdminController(usuarioClienteService,mock(IReporteJasperService.class));
var id=7;
var dto=new UsuarioClienteUpdateDTO();
var expected=new UsuarioClienteResponseDTO();when(usuarioClienteService.actualizar(id, dto)).thenReturn(expected);var r=ctl.actualizar(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(usuarioClienteService).actualizar(id, dto);
}

@Test void UC098_UsuarioClienteAdminController_eliminarPorEstado() throws Exception {
var usuarioClienteService=mock(IUsuarioClienteService.class);
var ctl=new UsuarioClienteAdminController(usuarioClienteService,mock(IReporteJasperService.class));
var id=7;
var r=ctl.eliminarPorEstado(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(usuarioClienteService).eliminarPorEstado(id);
}

@Test void UC099_UsuarioClienteAdminController_reactivar() throws Exception {
var usuarioClienteService=mock(IUsuarioClienteService.class);
var ctl=new UsuarioClienteAdminController(usuarioClienteService,mock(IReporteJasperService.class));
var id=7;
var r=ctl.reactivar(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(usuarioClienteService).reactivar(id);
}

@Test void UC100_UsuarioClienteAdminController_resetearPassword() throws Exception {
var usuarioClienteService=mock(IUsuarioClienteService.class);
var ctl=new UsuarioClienteAdminController(usuarioClienteService,mock(IReporteJasperService.class));
var id=7;
var dto=new UsuarioClienteResetPasswordDTO();
var r=ctl.resetearPassword(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(usuarioClienteService).resetearPassword(id, dto);
}

@Test void UC101_UsuarioController_buscarPorId() throws Exception {
var usuarioService=mock(IUsuarioService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new UsuarioController(usuarioService,reporteJasperService);
var id=7;
var expected=new UsuarioResponseDTO();when(usuarioService.buscarPorId(id)).thenReturn(expected);var r=ctl.buscarPorId(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(usuarioService).buscarPorId(id);
}

@Test void UC102_UsuarioController_crear() throws Exception {
var usuarioService=mock(IUsuarioService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new UsuarioController(usuarioService,reporteJasperService);
var dto=new UsuarioCreateDTO();
var expected=new UsuarioResponseDTO();when(usuarioService.crear(dto)).thenReturn(expected);var r=ctl.crear(dto);assertEquals(201,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(usuarioService).crear(dto);
}

@Test void UC103_UsuarioController_actualizar() throws Exception {
var usuarioService=mock(IUsuarioService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new UsuarioController(usuarioService,reporteJasperService);
var id=7;
var dto=new UsuarioUpdateDTO();
var expected=new UsuarioResponseDTO();when(usuarioService.actualizar(id, dto)).thenReturn(expected);var r=ctl.actualizar(id,dto);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(usuarioService).actualizar(id, dto);
}

@Test void UC104_UsuarioController_eliminarPorEstado() throws Exception {
var usuarioService=mock(IUsuarioService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new UsuarioController(usuarioService,reporteJasperService);
var id=7;
var r=ctl.eliminarPorEstado(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(usuarioService).eliminarPorEstado(id);
}

@Test void UC105_UsuarioController_reactivar() throws Exception {
var usuarioService=mock(IUsuarioService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new UsuarioController(usuarioService,reporteJasperService);
var id=7;
var r=ctl.reactivar(id);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertNull(r.getBody().getData());verify(usuarioService).reactivar(id);
}

@Test void UC106_CatalogoMercanciaController_listar_true() throws Exception {
var mercanciaService=mock(ICatalogoMercanciaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoMercanciaController(mercanciaService,reporteJasperService);
var expected=List.of(new CatalogoMercanciaResponseDTO());when(mercanciaService.listarActivos()).thenReturn(expected);var r=ctl.listar(true,null);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(mercanciaService).listarActivos();verifyNoMoreInteractions(mercanciaService);
}

@Test void UC107_CatalogoMercanciaController_listar_false() throws Exception {
var mercanciaService=mock(ICatalogoMercanciaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoMercanciaController(mercanciaService,reporteJasperService);
var expected=List.of(new CatalogoMercanciaResponseDTO());when(mercanciaService.listarTodos()).thenReturn(expected);var r=ctl.listar(false,null);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(mercanciaService).listarTodos();verifyNoMoreInteractions(mercanciaService);
}

@Test void UC108_CatalogoPuertoController_listar_true() throws Exception {
var puertoService=mock(ICatalogoPuertoService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoPuertoController(puertoService,reporteJasperService);
var expected=List.of(new CatalogoPuertoResponseDTO());when(puertoService.listarActivos()).thenReturn(expected);var r=ctl.listar(true);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(puertoService).listarActivos();verifyNoMoreInteractions(puertoService);
}

@Test void UC109_CatalogoPuertoController_listar_false() throws Exception {
var puertoService=mock(ICatalogoPuertoService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoPuertoController(puertoService,reporteJasperService);
var expected=List.of(new CatalogoPuertoResponseDTO());when(puertoService.listarTodos()).thenReturn(expected);var r=ctl.listar(false);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(puertoService).listarTodos();verifyNoMoreInteractions(puertoService);
}

@Test void UC110_CatalogoTipoCargaController_listar_true() throws Exception {
var tipoCargaService=mock(ICatalogoTipoCargaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoTipoCargaController(tipoCargaService,reporteJasperService);
var expected=List.of(new CatalogoTipoCargaResponseDTO());when(tipoCargaService.listarActivos()).thenReturn(expected);var r=ctl.listar(true);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(tipoCargaService).listarActivos();verifyNoMoreInteractions(tipoCargaService);
}

@Test void UC111_CatalogoTipoCargaController_listar_false() throws Exception {
var tipoCargaService=mock(ICatalogoTipoCargaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoTipoCargaController(tipoCargaService,reporteJasperService);
var expected=List.of(new CatalogoTipoCargaResponseDTO());when(tipoCargaService.listarTodos()).thenReturn(expected);var r=ctl.listar(false);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(tipoCargaService).listarTodos();verifyNoMoreInteractions(tipoCargaService);
}

@Test void UC112_ConductorController_listar_true() throws Exception {
var conductorService=mock(IConductorService.class);
var ctl=new ConductorController(conductorService);
var expected=List.of(new ConductorResponseDTO());when(conductorService.listarActivos()).thenReturn(expected);var r=ctl.listar(true);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(conductorService).listarActivos();verifyNoMoreInteractions(conductorService);
}

@Test void UC113_ConductorController_listar_false() throws Exception {
var conductorService=mock(IConductorService.class);
var ctl=new ConductorController(conductorService);
var expected=List.of(new ConductorResponseDTO());when(conductorService.listarTodos()).thenReturn(expected);var r=ctl.listar(false);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(conductorService).listarTodos();verifyNoMoreInteractions(conductorService);
}

@Test void UC114_RolController_listar_true() throws Exception {
var rolService=mock(IRolService.class);
var ctl=new RolController(rolService);
var expected=List.of(new RolResponseDTO());when(rolService.listarActivos()).thenReturn(expected);var r=ctl.listar(true);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(rolService).listarActivos();verifyNoMoreInteractions(rolService);
}

@Test void UC115_RolController_listar_false() throws Exception {
var rolService=mock(IRolService.class);
var ctl=new RolController(rolService);
var expected=List.of(new RolResponseDTO());when(rolService.listarTodos()).thenReturn(expected);var r=ctl.listar(false);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(rolService).listarTodos();verifyNoMoreInteractions(rolService);
}

@Test void UC116_SedeController_listar_true() throws Exception {
var sedeService=mock(ISedeService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new SedeController(sedeService,reporteJasperService);
var expected=List.of(new SedeResponseDTO());when(sedeService.listarOperativas()).thenReturn(expected);var r=ctl.listar(true);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(sedeService).listarOperativas();verifyNoMoreInteractions(sedeService);
}

@Test void UC117_SedeController_listar_false() throws Exception {
var sedeService=mock(ISedeService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new SedeController(sedeService,reporteJasperService);
var expected=List.of(new SedeResponseDTO());when(sedeService.listarTodas()).thenReturn(expected);var r=ctl.listar(false);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(sedeService).listarTodas();verifyNoMoreInteractions(sedeService);
}

@Test void UC118_UsuarioController_listar_true() throws Exception {
var usuarioService=mock(IUsuarioService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new UsuarioController(usuarioService,reporteJasperService);
var expected=List.of(new UsuarioResponseDTO());when(usuarioService.listarActivos()).thenReturn(expected);var r=ctl.listar(true);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(usuarioService).listarActivos();verifyNoMoreInteractions(usuarioService);
}

@Test void UC119_UsuarioController_listar_false() throws Exception {
var usuarioService=mock(IUsuarioService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new UsuarioController(usuarioService,reporteJasperService);
var expected=List.of(new UsuarioResponseDTO());when(usuarioService.listarTodos()).thenReturn(expected);var r=ctl.listar(false);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(usuarioService).listarTodos();verifyNoMoreInteractions(usuarioService);
}

@Test void UC120_CatalogoMercanciaController_listar_categoriaPrioritaria() throws Exception {
var mercanciaService=mock(ICatalogoMercanciaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoMercanciaController(mercanciaService,reporteJasperService);
var expected=List.of(new CatalogoMercanciaResponseDTO());when(mercanciaService.listarPorCategoria("GENERAL")).thenReturn(expected);var r=ctl.listar(true,"  GENERAL  ");assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(mercanciaService).listarPorCategoria("GENERAL");verifyNoMoreInteractions(mercanciaService);
}

@Test void UC121_CatalogoMercanciaController_listar_categoriaBlanca() throws Exception {
var mercanciaService=mock(ICatalogoMercanciaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoMercanciaController(mercanciaService,reporteJasperService);
var expected=List.of(new CatalogoMercanciaResponseDTO());when(mercanciaService.listarTodos()).thenReturn(expected);var r=ctl.listar(false,"  ");assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(mercanciaService).listarTodos();verifyNoMoreInteractions(mercanciaService);
}

@Test void UC122_UnidadTransporteController_listar_listarTodas() throws Exception {
var unidadService=mock(IUnidadTransporteService.class);
var ctl=new UnidadTransporteController(unidadService);
var expected=List.of(new UnidadTransporteResponseDTO());when(unidadService.listarTodas()).thenReturn(expected);var r=ctl.listar(false,false);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(unidadService).listarTodas();verifyNoMoreInteractions(unidadService);
}

@Test void UC123_UnidadTransporteController_listar_listarActivas() throws Exception {
var unidadService=mock(IUnidadTransporteService.class);
var ctl=new UnidadTransporteController(unidadService);
var expected=List.of(new UnidadTransporteResponseDTO());when(unidadService.listarActivas()).thenReturn(expected);var r=ctl.listar(true,false);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(unidadService).listarActivas();verifyNoMoreInteractions(unidadService);
}

@Test void UC124_UnidadTransporteController_listar_listarDisponibles() throws Exception {
var unidadService=mock(IUnidadTransporteService.class);
var ctl=new UnidadTransporteController(unidadService);
var expected=List.of(new UnidadTransporteResponseDTO());when(unidadService.listarDisponibles()).thenReturn(expected);var r=ctl.listar(true,true);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(unidadService).listarDisponibles();verifyNoMoreInteractions(unidadService);
}

@Test void UC125_ControlAlmacenController_listar_listarTodos124() throws Exception {
var controlAlmacenService=mock(IControlAlmacenService.class);
var ctl=new ControlAlmacenController(controlAlmacenService);
var expected=List.of(new ControlAlmacenResponseDTO());when(controlAlmacenService.listarTodos(true)).thenReturn(expected);var r=ctl.listar(true,null);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(controlAlmacenService).listarTodos(true);verifyNoMoreInteractions(controlAlmacenService);
}

@Test void UC126_ControlAlmacenController_listar_listarPorDeposito125() throws Exception {
var controlAlmacenService=mock(IControlAlmacenService.class);
var ctl=new ControlAlmacenController(controlAlmacenService);
var expected=List.of(new ControlAlmacenResponseDTO());when(controlAlmacenService.listarPorDeposito("DEP")).thenReturn(expected);var r=ctl.listar(false,"  DEP  ");assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(controlAlmacenService).listarPorDeposito("DEP");verifyNoMoreInteractions(controlAlmacenService);
}

@Test void UC127_ControlAlmacenController_listar_listarTodos126() throws Exception {
var controlAlmacenService=mock(IControlAlmacenService.class);
var ctl=new ControlAlmacenController(controlAlmacenService);
var expected=List.of(new ControlAlmacenResponseDTO());when(controlAlmacenService.listarTodos(false)).thenReturn(expected);var r=ctl.listar(false,"  ");assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(controlAlmacenService).listarTodos(false);verifyNoMoreInteractions(controlAlmacenService);
}

@Test void UC128_LiquidacionFinancieraController_listar_listarTodas() throws Exception {
var liquidacionService=mock(ILiquidacionFinancieraService.class);
var ctl=new LiquidacionFinancieraController(liquidacionService);
var expected=List.of(new LiquidacionFinancieraResponseDTO());when(liquidacionService.listarTodas(true)).thenReturn(expected);var r=ctl.listar(true,null);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(liquidacionService).listarTodas(true);verifyNoMoreInteractions(liquidacionService);
}

@Test void UC129_LiquidacionFinancieraController_listar_listarPorEstado() throws Exception {
var liquidacionService=mock(ILiquidacionFinancieraService.class);
var ctl=new LiquidacionFinancieraController(liquidacionService);
var expected=List.of(new LiquidacionFinancieraResponseDTO());when(liquidacionService.listarPorEstado("PENDIENTE")).thenReturn(expected);var r=ctl.listar(false,"  PENDIENTE  ");assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(liquidacionService).listarPorEstado("PENDIENTE");verifyNoMoreInteractions(liquidacionService);
}

@Test void UC130_DespachoTransporteController_listar_listarTodos() throws Exception {
var despachoService=mock(IDespachoTransporteService.class);
var ctl=new DespachoTransporteController(despachoService);
var expected=List.of(new DespachoTransporteResponseDTO());when(despachoService.listarTodos(false)).thenReturn(expected);var r=ctl.listar(false,null,null,null);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(despachoService).listarTodos(false);verifyNoMoreInteractions(despachoService);
}

@Test void UC131_DespachoTransporteController_listar_listarPorEstado() throws Exception {
var despachoService=mock(IDespachoTransporteService.class);
var ctl=new DespachoTransporteController(despachoService);
var expected=List.of(new DespachoTransporteResponseDTO());when(despachoService.listarPorEstado("EN_RUTA")).thenReturn(expected);var r=ctl.listar(true,"  EN_RUTA  ",7,8);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(despachoService).listarPorEstado("EN_RUTA");verifyNoMoreInteractions(despachoService);
}

@Test void UC132_DespachoTransporteController_listar_listarPorConductor() throws Exception {
var despachoService=mock(IDespachoTransporteService.class);
var ctl=new DespachoTransporteController(despachoService);
var expected=List.of(new DespachoTransporteResponseDTO());when(despachoService.listarPorConductor(7)).thenReturn(expected);var r=ctl.listar(false,null,7,8);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(despachoService).listarPorConductor(7);verifyNoMoreInteractions(despachoService);
}

@Test void UC133_DespachoTransporteController_listar_listarPorUnidad() throws Exception {
var despachoService=mock(IDespachoTransporteService.class);
var ctl=new DespachoTransporteController(despachoService);
var expected=List.of(new DespachoTransporteResponseDTO());when(despachoService.listarPorUnidad(8)).thenReturn(expected);var r=ctl.listar(false,null,null,8);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(despachoService).listarPorUnidad(8);verifyNoMoreInteractions(despachoService);
}

@Test void UC134_ExpedienteController_listar_listarTodos() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var expected=List.of(new ExpedienteResponseDTO());when(expedienteService.listarTodos(false)).thenReturn(expected);var r=ctl.listar(false,null,null,null,null);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(expedienteService).listarTodos(false);verifyNoMoreInteractions(expedienteService);
}

@Test void UC135_ExpedienteController_listar_listarPorCanal() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var expected=List.of(new ExpedienteResponseDTO());when(expedienteService.listarPorCanal(CanalSunat.values()[0])).thenReturn(expected);var r=ctl.listar(true,CanalSunat.values()[0],EstadoExpediente.values()[0],7,8);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(expedienteService).listarPorCanal(CanalSunat.values()[0]);verifyNoMoreInteractions(expedienteService);
}

@Test void UC136_ExpedienteController_listar_listarPorEstado() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var expected=List.of(new ExpedienteResponseDTO());when(expedienteService.listarPorEstado(EstadoExpediente.values()[0])).thenReturn(expected);var r=ctl.listar(false,null,EstadoExpediente.values()[0],7,8);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(expedienteService).listarPorEstado(EstadoExpediente.values()[0]);verifyNoMoreInteractions(expedienteService);
}

@Test void UC137_ExpedienteController_listar_listarPorAgente() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var expected=List.of(new ExpedienteResponseDTO());when(expedienteService.listarPorAgente(7)).thenReturn(expected);var r=ctl.listar(false,null,null,7,8);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(expedienteService).listarPorAgente(7);verifyNoMoreInteractions(expedienteService);
}

@Test void UC138_ExpedienteController_listar_listarPorCliente() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
var expected=List.of(new ExpedienteResponseDTO());when(expedienteService.listarPorCliente(8)).thenReturn(expected);var r=ctl.listar(false,null,null,null,8);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(expedienteService).listarPorCliente(8);verifyNoMoreInteractions(expedienteService);
}

@Test void UC139_CotizacionPagoController_listarAnticipos_listarTodos() throws Exception {
var cotizacionPagoService=mock(ICotizacionPagoService.class);
var ctl=new CotizacionPagoController(cotizacionPagoService);
var expected=List.of(new CotizacionPagoResponseDTO());when(cotizacionPagoService.listarTodos()).thenReturn(expected);var r=ctl.listarAnticipos(null);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(cotizacionPagoService).listarTodos();verifyNoMoreInteractions(cotizacionPagoService);
}

@Test void UC140_CotizacionPagoController_listarAnticipos_listarPorEstado() throws Exception {
var cotizacionPagoService=mock(ICotizacionPagoService.class);
var ctl=new CotizacionPagoController(cotizacionPagoService);
var expected=List.of(new CotizacionPagoResponseDTO());when(cotizacionPagoService.listarPorEstado(EstadoPagoCotizacion.PENDIENTE_REVISION)).thenReturn(expected);var r=ctl.listarAnticipos(EstadoPagoCotizacion.PENDIENTE_REVISION);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(cotizacionPagoService).listarPorEstado(EstadoPagoCotizacion.PENDIENTE_REVISION);verifyNoMoreInteractions(cotizacionPagoService);
}

@Test void UC141_AuthController_meAutenticado() throws Exception {
var usuarioService=mock(IUsuarioService.class);
var ctl=new AuthController(usuarioService);
var u=interno();var expected=new UsuarioResponseDTO();when(usuarioService.buscarPorId(7)).thenReturn(expected);var r=ctl.me(u);assertEquals(200,r.getStatusCode().value());assertSame(expected,r.getBody().getData());verify(usuarioService).buscarPorId(7);
}

@Test void UC142_AuthController_meSinId() throws Exception {
var usuarioService=mock(IUsuarioService.class);
var ctl=new AuthController(usuarioService);
assertThrows(AccessDeniedException.class,()->ctl.me(new Usuario()));verifyNoInteractions(usuarioService);
}

@Test void UC143_CotizacionController_listar_estado() throws Exception {
var cotizacionService=mock(ICotizacionService.class);
var ctl=new CotizacionController(cotizacionService);
var usuario=interno();
var expected=List.of(new CotizacionResponseDTO());when(cotizacionService.listar(true,EstadoCotizacion.APROBADA,null,null,usuario)).thenReturn(expected);var r=ctl.listar(true,EstadoCotizacion.APROBADA,null,null,usuario);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(cotizacionService).listar(true,EstadoCotizacion.APROBADA,null,null,usuario);verifyNoMoreInteractions(cotizacionService);
}

@Test void UC144_CatalogoMercanciaController_exportarMercanciasPdf() throws Exception {
var mercanciaService=mock(ICatalogoMercanciaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoMercanciaController(mercanciaService,reporteJasperService);
byte[] bytes="%PDF-QA".getBytes();when(reporteJasperService.exportarMercanciasPdf()).thenReturn(new ByteArrayInputStream(bytes));var r=ctl.exportarMercanciasPdf();assertEquals(200,r.getStatusCode().value());assertEquals("application/pdf",r.getHeaders().getContentType().toString());assertTrue(r.getHeaders().getFirst("Content-Disposition").startsWith("inline;"));assertArrayEquals(bytes,r.getBody().getInputStream().readAllBytes());verify(reporteJasperService).exportarMercanciasPdf();
}

@Test void UC145_CatalogoPuertoController_exportarPuertosPdf() throws Exception {
var puertoService=mock(ICatalogoPuertoService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoPuertoController(puertoService,reporteJasperService);
byte[] bytes="%PDF-QA".getBytes();when(reporteJasperService.exportarPuertosPdf()).thenReturn(new ByteArrayInputStream(bytes));var r=ctl.exportarPuertosPdf();assertEquals(200,r.getStatusCode().value());assertEquals("application/pdf",r.getHeaders().getContentType().toString());assertTrue(r.getHeaders().getFirst("Content-Disposition").startsWith("inline;"));assertArrayEquals(bytes,r.getBody().getInputStream().readAllBytes());verify(reporteJasperService).exportarPuertosPdf();
}

@Test void UC146_CatalogoTipoCargaController_exportarTiposCargaPdf() throws Exception {
var tipoCargaService=mock(ICatalogoTipoCargaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoTipoCargaController(tipoCargaService,reporteJasperService);
byte[] bytes="%PDF-QA".getBytes();when(reporteJasperService.exportarTiposCargaPdf()).thenReturn(new ByteArrayInputStream(bytes));var r=ctl.exportarTiposCargaPdf();assertEquals(200,r.getStatusCode().value());assertEquals("application/pdf",r.getHeaders().getContentType().toString());assertTrue(r.getHeaders().getFirst("Content-Disposition").startsWith("inline;"));assertArrayEquals(bytes,r.getBody().getInputStream().readAllBytes());verify(reporteJasperService).exportarTiposCargaPdf();
}

@Test void UC147_SedeController_exportarSedesPdf() throws Exception {
var sedeService=mock(ISedeService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new SedeController(sedeService,reporteJasperService);
byte[] bytes="%PDF-QA".getBytes();when(reporteJasperService.exportarSedesPdf()).thenReturn(new ByteArrayInputStream(bytes));var r=ctl.exportarSedesPdf();assertEquals(200,r.getStatusCode().value());assertEquals("application/pdf",r.getHeaders().getContentType().toString());assertTrue(r.getHeaders().getFirst("Content-Disposition").startsWith("inline;"));assertArrayEquals(bytes,r.getBody().getInputStream().readAllBytes());verify(reporteJasperService).exportarSedesPdf();
}

@Test void UC148_UsuarioController_exportarUsuariosPdf() throws Exception {
var usuarioService=mock(IUsuarioService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new UsuarioController(usuarioService,reporteJasperService);
byte[] bytes="%PDF-QA".getBytes();when(reporteJasperService.exportarUsuariosPdf(false)).thenReturn(new ByteArrayInputStream(bytes));var r=ctl.exportarUsuariosPdf(false);assertEquals(200,r.getStatusCode().value());assertEquals("application/pdf",r.getHeaders().getContentType().toString());assertTrue(r.getHeaders().getFirst("Content-Disposition").startsWith("inline;"));assertArrayEquals(bytes,r.getBody().getInputStream().readAllBytes());verify(reporteJasperService).exportarUsuariosPdf(false);
}

@Test void UC149_AdminLogsController_getLogs() throws Exception {
var adminLogsService=mock(IAdminLogsService.class);
var ctl=new AdminLogsController(adminLogsService);
var expected=Map.<String,Object>of("all",List.of("QA"));when(adminLogsService.getLogs("all",20)).thenReturn(expected);var r=ctl.getLogs("all",20);assertEquals(200,r.getStatusCode().value());assertSame(expected,r.getBody().getData());verify(adminLogsService).getLogs("all",20);
}

@Test void UC150_AdminLogsController_downloadLog_nombre() throws Exception {
var adminLogsService=mock(IAdminLogsService.class);
var ctl=new AdminLogsController(adminLogsService);
var file=mock(Resource.class);when(file.getFilename()).thenReturn("qa.log");when(adminLogsService.getLogFileResource("all")).thenReturn(file);var r=ctl.downloadLog("all");assertEquals(200,r.getStatusCode().value());assertSame(file,r.getBody());assertEquals("text/plain",r.getHeaders().getContentType().toString());assertTrue(r.getHeaders().getFirst("Content-Disposition").contains("qa.log"));verify(adminLogsService).getLogFileResource("all");
}

@Test void UC151_AdminLogsController_downloadLog_respaldo() throws Exception {
var adminLogsService=mock(IAdminLogsService.class);
var ctl=new AdminLogsController(adminLogsService);
var file=mock(Resource.class);when(file.getFilename()).thenReturn(null);when(adminLogsService.getLogFileResource("all")).thenReturn(file);var r=ctl.downloadLog("all");assertEquals(200,r.getStatusCode().value());assertSame(file,r.getBody());assertEquals("text/plain",r.getHeaders().getContentType().toString());assertTrue(r.getHeaders().getFirst("Content-Disposition").contains("sga-backend.log"));verify(adminLogsService).getLogFileResource("all");
}

@Test void UC152_AdminLogsController_clearLog() throws Exception {
var adminLogsService=mock(IAdminLogsService.class);
var ctl=new AdminLogsController(adminLogsService);
var r=ctl.clearLog("all");assertEquals(200,r.getStatusCode().value());assertNull(r.getBody().getData());verify(adminLogsService).clearLogFile("all");
}

@Test void UC153_CotizacionPagoController_registrarPago_usuario() throws Exception {
var cotizacionPagoService=mock(ICotizacionPagoService.class);
var ctl=new CotizacionPagoController(cotizacionPagoService);
var dto=new CotizacionPagoRegistrarDTO();var expected=new CotizacionPagoResponseDTO();when(cotizacionPagoService.registrarPago(dto,7)).thenReturn(expected);var r=ctl.registrarPago(7,dto,interno());assertEquals(201,r.getStatusCode().value());assertSame(expected,r.getBody().getData());verify(cotizacionPagoService).registrarPago(dto,7);assertEquals(7,dto.getIdCotizacion());
}

@Test void UC154_CotizacionPagoController_registrarPago_sinUsuario() throws Exception {
var cotizacionPagoService=mock(ICotizacionPagoService.class);
var ctl=new CotizacionPagoController(cotizacionPagoService);
var dto=new CotizacionPagoRegistrarDTO();var expected=new CotizacionPagoResponseDTO();when(cotizacionPagoService.registrarPago(dto,null)).thenReturn(expected);var r=ctl.registrarPago(7,dto,null);assertEquals(201,r.getStatusCode().value());assertSame(expected,r.getBody().getData());verify(cotizacionPagoService).registrarPago(dto,null);assertEquals(7,dto.getIdCotizacion());
}

@Test void UC155_CotizacionPagoController_validarPago_usuario() throws Exception {
var cotizacionPagoService=mock(ICotizacionPagoService.class);
var ctl=new CotizacionPagoController(cotizacionPagoService);
var dto=new CotizacionPagoValidarDTO();var expected=new CotizacionPagoResponseDTO();when(cotizacionPagoService.validarPago(7,dto,7)).thenReturn(expected);var r=ctl.validarPago(7,dto,interno());assertEquals(200,r.getStatusCode().value());assertSame(expected,r.getBody().getData());verify(cotizacionPagoService).validarPago(7,dto,7);
}

@Test void UC156_CotizacionPagoController_validarPago_sinUsuario() throws Exception {
var cotizacionPagoService=mock(ICotizacionPagoService.class);
var ctl=new CotizacionPagoController(cotizacionPagoService);
var dto=new CotizacionPagoValidarDTO();var expected=new CotizacionPagoResponseDTO();when(cotizacionPagoService.validarPago(7,dto,null)).thenReturn(expected);var r=ctl.validarPago(7,dto,null);assertEquals(200,r.getStatusCode().value());assertSame(expected,r.getBody().getData());verify(cotizacionPagoService).validarPago(7,dto,null);
}

@Test void UC157_LiquidacionFinancieraController_emitir_usuario() throws Exception {
var liquidacionService=mock(ILiquidacionFinancieraService.class);
var ctl=new LiquidacionFinancieraController(liquidacionService);
var dto=new LiquidacionFinancieraRequestDTO();var expected=new LiquidacionFinancieraResponseDTO();when(liquidacionService.emitir(dto,7)).thenReturn(expected);var r=ctl.emitir(dto,interno());assertEquals(201,r.getStatusCode().value());assertSame(expected,r.getBody().getData());verify(liquidacionService).emitir(dto,7);
}

@Test void UC158_LiquidacionFinancieraController_emitir_sinUsuario() throws Exception {
var liquidacionService=mock(ILiquidacionFinancieraService.class);
var ctl=new LiquidacionFinancieraController(liquidacionService);
var dto=new LiquidacionFinancieraRequestDTO();var expected=new LiquidacionFinancieraResponseDTO();when(liquidacionService.emitir(dto,null)).thenReturn(expected);var r=ctl.emitir(dto,null);assertEquals(201,r.getStatusCode().value());assertSame(expected,r.getBody().getData());verify(liquidacionService).emitir(dto,null);
}

@Test void UC159_DespachoTransporteController_obtenerMiDespachoActivo_usuario() throws Exception {
var despachoService=mock(IDespachoTransporteService.class);
var ctl=new DespachoTransporteController(despachoService);
var expected=new DespachoTransporteResponseDTO();when(despachoService.obtenerDespachoActivoPorUsuario(7)).thenReturn(expected);var r=ctl.obtenerMiDespachoActivo(interno());assertEquals(200,r.getStatusCode().value());assertSame(expected,r.getBody().getData());verify(despachoService).obtenerDespachoActivoPorUsuario(7);
}

@Test void UC160_DespachoTransporteController_obtenerMiDespachoActivo_nulo() throws Exception {
var despachoService=mock(IDespachoTransporteService.class);
var ctl=new DespachoTransporteController(despachoService);
var r=ctl.obtenerMiDespachoActivo(null);assertEquals(200,r.getStatusCode().value());assertNull(r.getBody().getData());verifyNoInteractions(despachoService);
}

@Test void UC161_DespachoTransporteController_listarMisDespachos_usuario() throws Exception {
var despachoService=mock(IDespachoTransporteService.class);
var ctl=new DespachoTransporteController(despachoService);
var expected=List.of(new DespachoTransporteResponseDTO());when(despachoService.listarMisDespachos(7)).thenReturn(expected);var r=ctl.listarMisDespachos(interno());assertEquals(200,r.getStatusCode().value());assertSame(expected,r.getBody().getData());verify(despachoService).listarMisDespachos(7);
}

@Test void UC162_DespachoTransporteController_listarMisDespachos_nulo() throws Exception {
var despachoService=mock(IDespachoTransporteService.class);
var ctl=new DespachoTransporteController(despachoService);
var r=ctl.listarMisDespachos(null);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().getData().isEmpty());verifyNoInteractions(despachoService);
}

@Test void UC163_DespachoTransporteController_reportarIncidencia_True() throws Exception {
var despachoService=mock(IDespachoTransporteService.class);
var ctl=new DespachoTransporteController(despachoService);
var expected=new DespachoTransporteResponseDTO();when(despachoService.reportarIncidencia(7,"QA")).thenReturn(expected);var r=ctl.reportarIncidencia(7,Map.of("incidencia","QA"));assertSame(expected,r.getBody().getData());verify(despachoService).reportarIncidencia(7,"QA");
}

@Test void UC164_DespachoTransporteController_reportarIncidencia_False() throws Exception {
var despachoService=mock(IDespachoTransporteService.class);
var ctl=new DespachoTransporteController(despachoService);
var expected=new DespachoTransporteResponseDTO();when(despachoService.reportarIncidencia(7,"Incidencia no detallada")).thenReturn(expected);var r=ctl.reportarIncidencia(7,Map.of());assertSame(expected,r.getBody().getData());verify(despachoService).reportarIncidencia(7,"Incidencia no detallada");
}

@Test void UC165_PortalController_login() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var loginRequest=new LoginRequestDTO();
var expected=new PortalAuthResponseDTO();when(portalService.autenticarCliente(loginRequest)).thenReturn(expected);var r=ctl.login(loginRequest);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(portalService).autenticarCliente(loginRequest);
}

@Test void UC166_PortalController_cambiarPassword() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var dto=new PortalCambioPasswordDTO();
var usuarioCliente=externo();
var r=ctl.cambiarPassword(dto,usuarioCliente);assertEquals(200,r.getStatusCode().value());assertNull(r.getBody().getData());verify(portalService).cambiarPassword(usuarioCliente, dto);
}

@Test void UC167_PortalController_obtenerPerfil() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var usuarioCliente=externo();
var expected=new PortalPerfilDTO();when(portalService.obtenerPerfil(usuarioCliente)).thenReturn(expected);var r=ctl.obtenerPerfil(usuarioCliente);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(portalService).obtenerPerfil(usuarioCliente);
}

@Test void UC168_PortalController_listarCotizaciones() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var usuarioCliente=externo();
var expected=List.of(new PortalCotizacionDTO());when(portalService.listarCotizaciones(11)).thenReturn(expected);var r=ctl.listarCotizaciones(usuarioCliente);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(portalService).listarCotizaciones(11);
}

@Test void UC169_PortalController_buscarCotizacionPorId() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var id=7;
var usuarioCliente=externo();
var expected=new PortalCotizacionDTO();when(portalService.buscarCotizacionPorId(11, id)).thenReturn(expected);var r=ctl.buscarCotizacionPorId(id,usuarioCliente);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(portalService).buscarCotizacionPorId(11, id);
}

@Test void UC170_PortalController_solicitarCotizacion() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var dto=new PortalCotizacionSolicitudDTO();
var usuarioCliente=externo();
var expected=new PortalCotizacionDTO();when(portalService.solicitarCotizacion(11, dto, 13)).thenReturn(expected);var r=ctl.solicitarCotizacion(dto,usuarioCliente);assertEquals(201,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(portalService).solicitarCotizacion(11, dto, 13);
}

@Test void UC171_PortalController_aceptarCotizacion() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var id=7;
var usuarioCliente=externo();
var expected=new PortalCotizacionDTO();when(portalService.aceptarCotizacion(11, id)).thenReturn(expected);var r=ctl.aceptarCotizacion(id,usuarioCliente);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(portalService).aceptarCotizacion(11, id);
}

@Test void UC172_PortalController_listarExpedientes() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var usuarioCliente=externo();
var expected=List.of(new PortalExpedienteDTO());when(portalService.listarExpedientes(11)).thenReturn(expected);var r=ctl.listarExpedientes(usuarioCliente);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(portalService).listarExpedientes(11);
}

@Test void UC173_PortalController_buscarExpedientePorId() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var id=7;
var usuarioCliente=externo();
var expected=new PortalExpedienteDTO();when(portalService.buscarExpedientePorId(11, id)).thenReturn(expected);var r=ctl.buscarExpedientePorId(id,usuarioCliente);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(portalService).buscarExpedientePorId(11, id);
}

@Test void UC174_PortalController_obtenerResumenFinanciero() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var usuarioCliente=externo();
var expected=new PortalResumenFinancieroDTO();when(portalService.obtenerResumenFinanciero(11)).thenReturn(expected);var r=ctl.obtenerResumenFinanciero(usuarioCliente);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(portalService).obtenerResumenFinanciero(11);
}

@Test void UC175_PortalController_listarObligacionesPendientes() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var usuarioCliente=externo();
var expected=List.of(new PortalObligacionPagoDTO());when(portalService.listarObligacionesPendientes(11)).thenReturn(expected);var r=ctl.listarObligacionesPendientes(usuarioCliente);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(portalService).listarObligacionesPendientes(11);
}

@Test void UC176_PortalController_listarAnticipos() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var usuarioCliente=externo();
var expected=List.of(new PortalPagoAnticipoDTO());when(portalService.listarAnticipos(11)).thenReturn(expected);var r=ctl.listarAnticipos(usuarioCliente);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(portalService).listarAnticipos(11);
}

@Test void UC177_PortalController_listarLiquidaciones() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var usuarioCliente=externo();
var expected=List.of(new PortalLiquidacionDTO());when(portalService.listarLiquidaciones(11)).thenReturn(expected);var r=ctl.listarLiquidaciones(usuarioCliente);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(portalService).listarLiquidaciones(11);
}

@Test void UC178_PortalController_registrarVoucher() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var dto=new PortalRegistroVoucherDTO();
var usuarioCliente=externo();
var expected=new PortalPagoAnticipoDTO();when(portalService.registrarVoucherPago(11, dto, 13)).thenReturn(expected);var r=ctl.registrarVoucher(dto,usuarioCliente);assertEquals(201,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(portalService).registrarVoucherPago(11, dto, 13);
}

@Test void UC179_PortalController_corregirVoucher() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var idPago=7;
var dto=new PortalCorreccionPagoDTO();
var usuarioCliente=externo();
var expected=new PortalPagoAnticipoDTO();when(portalService.corregirPagoAnticipo(11, idPago, dto, 13)).thenReturn(expected);var r=ctl.corregirVoucher(idPago,dto,usuarioCliente);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(portalService).corregirPagoAnticipo(11, idPago, dto, 13);
}

@Test void UC180_PortalController_listarNotificaciones() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var usuarioCliente=externo();
var expected=List.of(new NotificacionClienteDTO());when(portalService.listarNotificaciones(11)).thenReturn(expected);var r=ctl.listarNotificaciones(usuarioCliente);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertSame(expected,r.getBody().getData());verify(portalService).listarNotificaciones(11);
}

@Test void UC181_PortalController_contarNotificacionesNoLeidas() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var usuarioCliente=externo();
var expected=3L;when(portalService.contarNotificacionesNoLeidas(11)).thenReturn(expected);var r=ctl.contarNotificacionesNoLeidas(usuarioCliente);assertEquals(200,r.getStatusCode().value());assertTrue(r.getBody().isSuccess());assertEquals(expected,r.getBody().getData());verify(portalService).contarNotificacionesNoLeidas(11);
}

@Test void UC182_PortalController_marcarNotificacionLeida() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var id=7;
var usuarioCliente=externo();
var r=ctl.marcarNotificacionLeida(id,usuarioCliente);assertEquals(200,r.getStatusCode().value());assertNull(r.getBody().getData());verify(portalService).marcarNotificacionLeida(11, id);
}

@Test void UC183_PortalController_marcarTodasNotificacionesLeidas() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var usuarioCliente=externo();
var r=ctl.marcarTodasNotificacionesLeidas(usuarioCliente);assertEquals(200,r.getStatusCode().value());assertNull(r.getBody().getData());verify(portalService).marcarTodasNotificacionesLeidas(11);
}

@Test void UC184_PortalController_listarDocumentosExpediente() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var expected=List.of(new DocumentoResponseDTO());when(documentoService.listar(EntidadDocumento.EXPEDIENTE,null,7,null,null)).thenReturn(expected);var r=ctl.listarDocumentosExpediente(7,externo());assertSame(expected,r.getBody().getData());var order=inOrder(portalService,documentoService);order.verify(portalService).buscarExpedientePorId(11,7);order.verify(documentoService).listar(EntidadDocumento.EXPEDIENTE,null,7,null,null);
}

@Test void UC185_PortalController_documentosAjenoRechazado() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
when(portalService.buscarExpedientePorId(11,7)).thenThrow(new BadRequestException("Ajeno"));assertThrows(BadRequestException.class,()->ctl.listarDocumentosExpediente(7,externo()));verifyNoInteractions(documentoService);
}

@Test void UC186_PortalController_subirComprobanteVoucher() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
var file=mock(MultipartFile.class);var expected=new DocumentoResponseDTO();when(documentoService.subirDocumentos(List.of(file),CategoriaDocumento.GENERAL,"Voucher de abono bancario registrado por cliente en portal B2B",EntidadDocumento.CLIENTE,11,null)).thenReturn(List.of(expected));var r=ctl.subirComprobanteVoucher(file,externo());assertEquals(201,r.getStatusCode().value());assertSame(expected,r.getBody().getData());verify(documentoService).subirDocumentos(List.of(file),CategoriaDocumento.GENERAL,"Voucher de abono bancario registrado por cliente en portal B2B",EntidadDocumento.CLIENTE,11,null);
}

@Test void UC187_PortalController_perfilInvalido186() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
assertThrows(BadRequestException.class,()->ctl.obtenerPerfil(null));verifyNoInteractions(portalService,documentoService);
}

@Test void UC188_PortalController_perfilInvalido187() throws Exception {
var portalService=mock(IPortalService.class);
var documentoService=mock(IDocumentoService.class);
var ctl=new PortalController(portalService,documentoService);
assertThrows(BadRequestException.class,()->ctl.obtenerPerfil(new UsuarioCliente()));verifyNoInteractions(portalService,documentoService);
}

@Test void UC189_DocumentoController_subir_True() throws Exception {
var documentoService=mock(IDocumentoService.class);
var ctl=new DocumentoController(documentoService);
var files=List.of(mock(MultipartFile.class));var expected=List.of(new DocumentoResponseDTO());when(documentoService.subirDocumentos(files,CategoriaDocumento.GENERAL,"QA",EntidadDocumento.CLIENTE,11,7)).thenReturn(expected);var r=ctl.subir(files,CategoriaDocumento.GENERAL,"QA",EntidadDocumento.CLIENTE,11,interno());assertEquals(201,r.getStatusCode().value());assertSame(expected,r.getBody().getData());verify(documentoService).subirDocumentos(files,CategoriaDocumento.GENERAL,"QA",EntidadDocumento.CLIENTE,11,7);
}

@Test void UC190_DocumentoController_listar_True() throws Exception {
var documentoService=mock(IDocumentoService.class);
var ctl=new DocumentoController(documentoService);
var usuario=interno();var expected=List.of(new DocumentoResponseDTO());when(documentoService.listarParaUsuario(usuario,EntidadDocumento.CLIENTE,null,null,11,null)).thenReturn(expected);var r=ctl.listar(EntidadDocumento.CLIENTE,null,null,11,null,usuario);assertEquals(200,r.getStatusCode().value());assertSame(expected,r.getBody().getData());verify(documentoService).listarParaUsuario(usuario,EntidadDocumento.CLIENTE,null,null,11,null);
}

@Test void UC191_DocumentoController_buscarPorId_True() throws Exception {
var documentoService=mock(IDocumentoService.class);
var ctl=new DocumentoController(documentoService);
var entity=new DocumentoAdjunto();var expected=new DocumentoResponseDTO();when(documentoService.obtenerEntidad(7)).thenReturn(entity);when(documentoService.buscarPorId(7)).thenReturn(expected);var r=ctl.buscarPorId(7,interno());assertSame(expected,r.getBody().getData());verify(documentoService).buscarPorId(7);
}

@Test void UC192_DocumentoController_descargar_True() throws Exception {
var documentoService=mock(IDocumentoService.class);
var ctl=new DocumentoController(documentoService);
var entity=new DocumentoAdjunto();entity.setNombreOriginal("prueba QA.pdf");entity.setFormatoMime("application/pdf");var file=new ByteArrayResource(new byte[]{1,2,3});when(documentoService.obtenerEntidad(7)).thenReturn(entity);when(documentoService.descargarArchivo(7)).thenReturn(file);var r=ctl.descargar(7,interno());assertEquals(200,r.getStatusCode().value());assertSame(file,r.getBody());assertEquals("application/pdf",r.getHeaders().getContentType().toString());assertTrue(r.getHeaders().getFirst("Content-Disposition").startsWith("attachment;"));assertTrue(r.getHeaders().getFirst("Content-Disposition").contains("prueba%20QA.pdf"));verify(documentoService).descargarArchivo(7);
}

@Test void UC193_DocumentoController_previsualizar_True() throws Exception {
var documentoService=mock(IDocumentoService.class);
var ctl=new DocumentoController(documentoService);
var entity=new DocumentoAdjunto();entity.setNombreOriginal("prueba QA.pdf");entity.setFormatoMime("application/pdf");var file=new ByteArrayResource(new byte[]{1,2,3});when(documentoService.obtenerEntidad(7)).thenReturn(entity);when(documentoService.descargarArchivo(7)).thenReturn(file);var r=ctl.previsualizar(7,interno());assertEquals(200,r.getStatusCode().value());assertSame(file,r.getBody());assertEquals("application/pdf",r.getHeaders().getContentType().toString());assertTrue(r.getHeaders().getFirst("Content-Disposition").startsWith("inline;"));assertTrue(r.getHeaders().getFirst("Content-Disposition").contains("prueba%20QA.pdf"));verify(documentoService).descargarArchivo(7);
}

@Test void UC194_DocumentoController_subir_False() throws Exception {
var documentoService=mock(IDocumentoService.class);
var ctl=new DocumentoController(documentoService);
var files=List.of(mock(MultipartFile.class));var expected=List.of(new DocumentoResponseDTO());when(documentoService.subirDocumentos(files,CategoriaDocumento.GENERAL,"QA",EntidadDocumento.CLIENTE,11,null)).thenReturn(expected);var r=ctl.subir(files,CategoriaDocumento.GENERAL,"QA",EntidadDocumento.CLIENTE,11,externo());assertEquals(201,r.getStatusCode().value());assertSame(expected,r.getBody().getData());var order=inOrder(documentoService);order.verify(documentoService).validarPropiedadEntidadCliente(11,EntidadDocumento.CLIENTE,11);order.verify(documentoService).subirDocumentos(files,CategoriaDocumento.GENERAL,"QA",EntidadDocumento.CLIENTE,11,null);
}

@Test void UC195_DocumentoController_listar_False() throws Exception {
var documentoService=mock(IDocumentoService.class);
var ctl=new DocumentoController(documentoService);
var expected=List.of(new DocumentoResponseDTO());when(documentoService.listar(EntidadDocumento.CLIENTE,null,null,11,null)).thenReturn(expected);var r=ctl.listar(EntidadDocumento.CLIENTE,null,null,11,null,externo());assertEquals(200,r.getStatusCode().value());assertSame(expected,r.getBody().getData());verify(documentoService).listar(EntidadDocumento.CLIENTE,null,null,11,null);verify(documentoService).validarPropiedadConsultaCliente(11,null,null,11);
}

@Test void UC196_DocumentoController_buscarPorId_False() throws Exception {
var documentoService=mock(IDocumentoService.class);
var ctl=new DocumentoController(documentoService);
var entity=new DocumentoAdjunto();var expected=new DocumentoResponseDTO();when(documentoService.obtenerEntidad(7)).thenReturn(entity);when(documentoService.buscarPorId(7)).thenReturn(expected);var r=ctl.buscarPorId(7,externo());assertSame(expected,r.getBody().getData());verify(documentoService).buscarPorId(7);verify(documentoService).validarPropiedadDocumentoCliente(11,entity);
}

@Test void UC197_DocumentoController_descargar_False() throws Exception {
var documentoService=mock(IDocumentoService.class);
var ctl=new DocumentoController(documentoService);
var entity=new DocumentoAdjunto();entity.setNombreOriginal("prueba QA.pdf");entity.setFormatoMime("application/pdf");var file=new ByteArrayResource(new byte[]{1,2,3});when(documentoService.obtenerEntidad(7)).thenReturn(entity);when(documentoService.descargarArchivo(7)).thenReturn(file);var r=ctl.descargar(7,externo());assertEquals(200,r.getStatusCode().value());assertSame(file,r.getBody());assertEquals("application/pdf",r.getHeaders().getContentType().toString());assertTrue(r.getHeaders().getFirst("Content-Disposition").startsWith("attachment;"));assertTrue(r.getHeaders().getFirst("Content-Disposition").contains("prueba%20QA.pdf"));verify(documentoService).validarPropiedadDocumentoCliente(11,entity);verify(documentoService).descargarArchivo(7);
}

@Test void UC198_DocumentoController_previsualizar_False() throws Exception {
var documentoService=mock(IDocumentoService.class);
var ctl=new DocumentoController(documentoService);
var entity=new DocumentoAdjunto();entity.setNombreOriginal("prueba QA.pdf");entity.setFormatoMime("application/pdf");var file=new ByteArrayResource(new byte[]{1,2,3});when(documentoService.obtenerEntidad(7)).thenReturn(entity);when(documentoService.descargarArchivo(7)).thenReturn(file);var r=ctl.previsualizar(7,externo());assertEquals(200,r.getStatusCode().value());assertSame(file,r.getBody());assertEquals("application/pdf",r.getHeaders().getContentType().toString());assertTrue(r.getHeaders().getFirst("Content-Disposition").startsWith("inline;"));assertTrue(r.getHeaders().getFirst("Content-Disposition").contains("prueba%20QA.pdf"));verify(documentoService).validarPropiedadDocumentoCliente(11,entity);verify(documentoService).descargarArchivo(7);
}

@Test void UC199_DocumentoController_eliminar() throws Exception {
var documentoService=mock(IDocumentoService.class);
var ctl=new DocumentoController(documentoService);
var r=ctl.eliminar(7,interno());assertEquals(200,r.getStatusCode().value());assertNull(r.getBody().getData());verify(documentoService).eliminar(7);
}

@Test void UC200_DocumentoController_subirSinPrincipal() throws Exception {
var documentoService=mock(IDocumentoService.class);
var ctl=new DocumentoController(documentoService);
assertThrows(AccessDeniedException.class,()->ctl.subir(List.of(),null,null,EntidadDocumento.GENERAL,null,null));verifyNoInteractions(documentoService);
}

@Test void UC201_DocumentoController_listarSinPrincipal() throws Exception {
var documentoService=mock(IDocumentoService.class);
var ctl=new DocumentoController(documentoService);
assertThrows(AccessDeniedException.class,()->ctl.listar(null,null,null,null,null,null));verifyNoInteractions(documentoService);
}

@Test void UC202_DocumentoController_buscarSinPrincipal() throws Exception {
var documentoService=mock(IDocumentoService.class);
var ctl=new DocumentoController(documentoService);
assertThrows(AccessDeniedException.class,()->ctl.buscarPorId(7,null));verify(documentoService).obtenerEntidad(7);verify(documentoService,never()).buscarPorId(anyInt());verifyNoMoreInteractions(documentoService);
}

@Test void UC203_DocumentoController_descargarSinPrincipal() throws Exception {
var documentoService=mock(IDocumentoService.class);
var ctl=new DocumentoController(documentoService);
assertThrows(AccessDeniedException.class,()->ctl.descargar(7,null));verify(documentoService,never()).descargarArchivo(anyInt());
}

@Test void UC204_DocumentoController_previsualizarSinPrincipal() throws Exception {
var documentoService=mock(IDocumentoService.class);
var ctl=new DocumentoController(documentoService);
assertThrows(AccessDeniedException.class,()->ctl.previsualizar(7,null));verify(documentoService,never()).descargarArchivo(anyInt());
}

@Test void UC205_DocumentoController_subidaAjenaRechazada() throws Exception {
var documentoService=mock(IDocumentoService.class);
var ctl=new DocumentoController(documentoService);
doThrow(new BadRequestException("Ajeno")).when(documentoService).validarPropiedadEntidadCliente(11,EntidadDocumento.CLIENTE,8);assertThrows(BadRequestException.class,()->ctl.subir(List.of(),null,null,EntidadDocumento.CLIENTE,8,externo()));verify(documentoService,never()).subirDocumentos(anyList(),any(),any(),any(),any(),any());
}

@Test void UC206_CatalogoMercanciaController_recursoInexistente() throws Exception {
var mercanciaService=mock(ICatalogoMercanciaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoMercanciaController(mercanciaService,reporteJasperService);
when(mercanciaService.buscarPorId(999)).thenThrow(new pe.sga.exception.ResourceNotFoundException("No existe"));assertThrows(pe.sga.exception.ResourceNotFoundException.class,()->ctl.buscarPorId(999));verify(mercanciaService).buscarPorId(999);
}

@Test void UC207_CatalogoPuertoController_recursoInexistente() throws Exception {
var puertoService=mock(ICatalogoPuertoService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoPuertoController(puertoService,reporteJasperService);
when(puertoService.buscarPorId(999)).thenThrow(new pe.sga.exception.ResourceNotFoundException("No existe"));assertThrows(pe.sga.exception.ResourceNotFoundException.class,()->ctl.buscarPorId(999));verify(puertoService).buscarPorId(999);
}

@Test void UC208_CatalogoTipoCargaController_recursoInexistente() throws Exception {
var tipoCargaService=mock(ICatalogoTipoCargaService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new CatalogoTipoCargaController(tipoCargaService,reporteJasperService);
when(tipoCargaService.buscarPorId(999)).thenThrow(new pe.sga.exception.ResourceNotFoundException("No existe"));assertThrows(pe.sga.exception.ResourceNotFoundException.class,()->ctl.buscarPorId(999));verify(tipoCargaService).buscarPorId(999);
}

@Test void UC209_ConductorController_recursoInexistente() throws Exception {
var conductorService=mock(IConductorService.class);
var ctl=new ConductorController(conductorService);
when(conductorService.buscarPorId(999)).thenThrow(new pe.sga.exception.ResourceNotFoundException("No existe"));assertThrows(pe.sga.exception.ResourceNotFoundException.class,()->ctl.buscarPorId(999));verify(conductorService).buscarPorId(999);
}

@Test void UC210_ControlAlmacenController_recursoInexistente() throws Exception {
var controlAlmacenService=mock(IControlAlmacenService.class);
var ctl=new ControlAlmacenController(controlAlmacenService);
when(controlAlmacenService.buscarPorId(999)).thenThrow(new pe.sga.exception.ResourceNotFoundException("No existe"));assertThrows(pe.sga.exception.ResourceNotFoundException.class,()->ctl.buscarPorId(999));verify(controlAlmacenService).buscarPorId(999);
}

@Test void UC211_CotizacionPagoController_recursoInexistente() throws Exception {
var cotizacionPagoService=mock(ICotizacionPagoService.class);
var ctl=new CotizacionPagoController(cotizacionPagoService);
when(cotizacionPagoService.buscarPorId(999)).thenThrow(new pe.sga.exception.ResourceNotFoundException("No existe"));assertThrows(pe.sga.exception.ResourceNotFoundException.class,()->ctl.buscarPorId(999));verify(cotizacionPagoService).buscarPorId(999);
}

@Test void UC212_DespachoTransporteController_recursoInexistente() throws Exception {
var despachoService=mock(IDespachoTransporteService.class);
var ctl=new DespachoTransporteController(despachoService);
when(despachoService.buscarPorId(999)).thenThrow(new pe.sga.exception.ResourceNotFoundException("No existe"));assertThrows(pe.sga.exception.ResourceNotFoundException.class,()->ctl.buscarPorId(999));verify(despachoService).buscarPorId(999);
}

@Test void UC213_ExpedienteController_recursoInexistente() throws Exception {
var expedienteService=mock(IExpedienteService.class);
var ctl=new ExpedienteController(expedienteService);
when(expedienteService.buscarPorId(999)).thenThrow(new pe.sga.exception.ResourceNotFoundException("No existe"));assertThrows(pe.sga.exception.ResourceNotFoundException.class,()->ctl.buscarPorId(999));verify(expedienteService).buscarPorId(999);
}

@Test void UC214_LiquidacionFinancieraController_recursoInexistente() throws Exception {
var liquidacionService=mock(ILiquidacionFinancieraService.class);
var ctl=new LiquidacionFinancieraController(liquidacionService);
when(liquidacionService.buscarPorId(999)).thenThrow(new pe.sga.exception.ResourceNotFoundException("No existe"));assertThrows(pe.sga.exception.ResourceNotFoundException.class,()->ctl.buscarPorId(999));verify(liquidacionService).buscarPorId(999);
}

@Test void UC215_RolController_recursoInexistente() throws Exception {
var rolService=mock(IRolService.class);
var ctl=new RolController(rolService);
when(rolService.buscarPorId(999)).thenThrow(new pe.sga.exception.ResourceNotFoundException("No existe"));assertThrows(pe.sga.exception.ResourceNotFoundException.class,()->ctl.buscarPorId(999));verify(rolService).buscarPorId(999);
}

@Test void UC216_SedeController_recursoInexistente() throws Exception {
var sedeService=mock(ISedeService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new SedeController(sedeService,reporteJasperService);
when(sedeService.buscarPorId(999)).thenThrow(new pe.sga.exception.ResourceNotFoundException("No existe"));assertThrows(pe.sga.exception.ResourceNotFoundException.class,()->ctl.buscarPorId(999));verify(sedeService).buscarPorId(999);
}

@Test void UC217_UnidadTransporteController_recursoInexistente() throws Exception {
var unidadService=mock(IUnidadTransporteService.class);
var ctl=new UnidadTransporteController(unidadService);
when(unidadService.buscarPorId(999)).thenThrow(new pe.sga.exception.ResourceNotFoundException("No existe"));assertThrows(pe.sga.exception.ResourceNotFoundException.class,()->ctl.buscarPorId(999));verify(unidadService).buscarPorId(999);
}

@Test void UC218_UsuarioClienteAdminController_recursoInexistente() throws Exception {
var usuarioClienteService=mock(IUsuarioClienteService.class);
var ctl=new UsuarioClienteAdminController(usuarioClienteService,mock(IReporteJasperService.class));
when(usuarioClienteService.buscarPorId(999)).thenThrow(new pe.sga.exception.ResourceNotFoundException("No existe"));assertThrows(pe.sga.exception.ResourceNotFoundException.class,()->ctl.buscarPorId(999));verify(usuarioClienteService).buscarPorId(999);
}

@Test void UC219_UsuarioController_recursoInexistente() throws Exception {
var usuarioService=mock(IUsuarioService.class);
var reporteJasperService=mock(IReporteJasperService.class);
var ctl=new UsuarioController(usuarioService,reporteJasperService);
when(usuarioService.buscarPorId(999)).thenThrow(new pe.sga.exception.ResourceNotFoundException("No existe"));assertThrows(pe.sga.exception.ResourceNotFoundException.class,()->ctl.buscarPorId(999));verify(usuarioService).buscarPorId(999);
}
}