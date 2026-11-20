package pe.sga.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;
import pe.sga.dto.portal.*;
import pe.sga.dto.portal.admin.*;
import pe.sga.dto.request.*;
import pe.sga.dto.response.*;
import pe.sga.entity.*;
import pe.sga.entity.enums.*;
import pe.sga.exception.BadRequestException;
import pe.sga.service.*;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Pruebas Unitarias de Controladores REST")
class AllControllersFunctionalTest {

    private Usuario interno() {
        var u = new Usuario();
        u.setIdUsuario(7);
        return u;
    }

    private UsuarioCliente externo() {
        var c = new Cliente();
        c.setIdCliente(11);
        var u = new UsuarioCliente();
        u.setCliente(c);
        u.setIdUsuarioCliente(13);
        return u;
    }

    @Test
    @DisplayName("HealthController y AuthController: estado UP, login y validación de principal en /me")
    void healthYAuthController_operacionesPrincipales() {
        var healthRes = new HealthController().checkHealth();
        assertEquals(200, healthRes.getStatusCode().value());
        assertEquals("UP", healthRes.getBody().getData().get("status"));

        var usuarioService = mock(IUsuarioService.class);
        var authCtl = new AuthController(usuarioService);

        var loginReq = new LoginRequestDTO();
        var authResp = new AuthResponseDTO();
        when(usuarioService.autenticar(loginReq)).thenReturn(authResp);
        assertSame(authResp, authCtl.login(loginReq).getBody().getData());

        var userResp = new UsuarioResponseDTO();
        when(usuarioService.buscarPorId(7)).thenReturn(userResp);
        assertSame(userResp, authCtl.me(interno()).getBody().getData());

        assertThrows(AccessDeniedException.class, () -> authCtl.me(null));
        assertThrows(AccessDeniedException.class, () -> authCtl.me(new Usuario()));
    }

    @Test
    @DisplayName("ClienteController: prioridad de filtros en listar, registro 201 y formalización de prospecto")
    void clienteController_filtrosYOperacionesComerciales() {
        var service = mock(IClienteService.class);
        var ctl = new ClienteController(service);

        ctl.listar(true, null, null, "  abc  ");
        verify(service).buscarPorTermino("  abc  ", true);

        ctl.listar(false, null, "PROSPECTO", null);
        verify(service).listarPorTipoProspecto(true, false);

        ctl.listar(true, null, "TODOS", null);
        verify(service).listarActivos();

        ctl.listar(false, null, null, "   ");
        verify(service).listarTodos();

        var req = new ClienteRequestDTO();
        var expected = new ClienteResponseDTO();
        when(service.crear(req)).thenReturn(expected);
        var created = ctl.crear(req);
        assertEquals(201, created.getStatusCode().value());
        assertSame(expected, created.getBody().getData());

        when(service.convertirEnCliente(7)).thenReturn(expected);
        assertSame(expected, ctl.convertirEnCliente(7).getBody().getData());
    }

    @Test
    @DisplayName("CotizacionController: listado filtrado por asesor, creación 201, asignación, revisión, estado y reporte PDF")
    void cotizacionController_flujoComercialYReportePdf() throws Exception {
        var cotizacionService = mock(ICotizacionService.class);
        var reporteService = mock(IReporteJasperService.class);
        var ctl = new CotizacionController(cotizacionService, reporteService);
        var user = interno();

        var expected = new CotizacionResponseDTO();
        when(cotizacionService.listar(true, EstadoCotizacion.APROBADA, 11, 7, user)).thenReturn(List.of(expected));
        var listRes = ctl.listar(true, EstadoCotizacion.APROBADA, 11, 7, user);
        assertEquals(200, listRes.getStatusCode().value());
        assertEquals(1, listRes.getBody().getData().size());

        var req = new CotizacionRequestDTO();
        when(cotizacionService.crear(req, user)).thenReturn(expected);
        var createRes = ctl.crear(req, user);
        assertEquals(201, createRes.getStatusCode().value());
        assertSame(expected, createRes.getBody().getData());

        var asignarDto = new CotizacionAsignarDTO();
        asignarDto.setIdUsuarioComercial(9);
        when(cotizacionService.asignarAsesor(5, 9, user)).thenReturn(expected);
        assertSame(expected, ctl.asignar(5, asignarDto, user).getBody().getData());

        var revDto = new CotizacionRevisionDTO();
        when(cotizacionService.revisarTecnicamente(5, revDto)).thenReturn(expected);
        assertSame(expected, ctl.revisarTecnicamente(5, revDto).getBody().getData());

        byte[] pdfBytes = "%PDF-COT".getBytes();
        when(reporteService.exportarReporteComercialPdf(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), 7, "IMPORTACION_DEFINITIVA", user))
                .thenReturn(new ByteArrayInputStream(pdfBytes));
        var pdfRes = ctl.exportarReporteComercialPdf(
                LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 30), 7, "IMPORTACION_DEFINITIVA", user);
        assertEquals(200, pdfRes.getStatusCode().value());
        assertEquals("application/pdf", pdfRes.getHeaders().getContentType().toString());
        assertArrayEquals(pdfBytes, pdfRes.getBody().getInputStream().readAllBytes());
    }

    @Test
    @DisplayName("CotizacionPagoController: registro de pago con principal, filtro de bandeja y validación financiera")
    void cotizacionPagoController_registroBandejaYValidacion() {
        var service = mock(ICotizacionPagoService.class);
        var ctl = new CotizacionPagoController(service);
        var user = interno();

        var regDto = new CotizacionPagoRegistrarDTO();
        var expected = new CotizacionPagoResponseDTO();
        when(service.registrarPago(regDto, 7)).thenReturn(expected);
        var regRes = ctl.registrarPago(15, regDto, user);
        assertEquals(201, regRes.getStatusCode().value());
        assertEquals(15, regDto.getIdCotizacion());
        assertSame(expected, regRes.getBody().getData());

        when(service.listarPorCotizacion(15, user)).thenReturn(List.of(expected));
        assertEquals(1, ctl.listarPorCotizacion(15, user).getBody().getData().size());

        ctl.listarAnticipos(null);
        verify(service).listarTodos();

        ctl.listarAnticipos(EstadoPagoCotizacion.PENDIENTE_REVISION);
        verify(service).listarPorEstado(EstadoPagoCotizacion.PENDIENTE_REVISION);

        var valDto = new CotizacionPagoValidarDTO();
        when(service.validarPago(20, valDto, 7)).thenReturn(expected);
        assertSame(expected, ctl.validarPago(20, valDto, user).getBody().getData());
    }

    @Test
    @DisplayName("ExpedienteController: prioridad de filtros en listar, apertura 201 e hitos operativos")
    void expedienteController_filtrosAperturaEHitos() {
        var service = mock(IExpedienteService.class);
        var ctl = new ExpedienteController(service);

        ctl.listar(true, CanalSunat.ROJO, EstadoExpediente.ASIGNADO, 7, 8);
        verify(service).listarPorCanal(CanalSunat.ROJO);

        ctl.listar(false, null, EstadoExpediente.EN_TRAMITE, 7, 8);
        verify(service).listarPorEstado(EstadoExpediente.EN_TRAMITE);

        ctl.listar(false, null, null, 7, 8);
        verify(service).listarPorAgente(7);

        ctl.listar(false, null, null, null, 8);
        verify(service).listarPorCliente(8);

        ctl.listar(true, null, null, null, null);
        verify(service).listarTodos(true);

        var crearDto = new ExpedienteCrearDTO();
        var expected = new ExpedienteResponseDTO();
        when(service.crear(crearDto)).thenReturn(expected);
        var createRes = ctl.crear(crearDto);
        assertEquals(201, createRes.getStatusCode().value());
        assertSame(expected, createRes.getBody().getData());

        var levanteDto = new ExpedienteLevanteDTO();
        when(service.autorizarLevante(7, levanteDto)).thenReturn(expected);
        assertSame(expected, ctl.autorizarLevante(7, levanteDto).getBody().getData());
    }

    @Test
    @DisplayName("ControlAlmacenController: filtro por depósito con trim, ingreso 201 y autorización de salida")
    void controlAlmacenController_filtrosIngresoYSalida() {
        var service = mock(IControlAlmacenService.class);
        var ctl = new ControlAlmacenController(service);

        ctl.listar(false, "  APM Terminals  ");
        verify(service).listarPorDeposito("APM Terminals");

        ctl.listar(true, "   ");
        verify(service).listarTodos(true);

        var req = new ControlAlmacenRequestDTO();
        var expected = new ControlAlmacenResponseDTO();
        when(service.registrarIngreso(req)).thenReturn(expected);
        var createRes = ctl.registrarIngreso(req);
        assertEquals(201, createRes.getStatusCode().value());
        assertSame(expected, createRes.getBody().getData());

        var salidaDto = new ControlAlmacenSalidaDTO();
        when(service.autorizarSalida(7, salidaDto)).thenReturn(expected);
        assertSame(expected, ctl.autorizarSalida(7, salidaDto).getBody().getData());
    }

    @Test
    @DisplayName("DespachoTransporteController, UnidadTransporteController y ConductorController: filtros, cabina e incidencias")
    void transporteControllers_despachosUnidadesYConductores() {
        var despachoService = mock(IDespachoTransporteService.class);
        var despachoCtl = new DespachoTransporteController(despachoService);

        despachoCtl.listar(true, "  EN_RUTA  ", 7, 8);
        verify(despachoService).listarPorEstado("EN_RUTA");

        despachoCtl.listar(false, null, 7, 8);
        verify(despachoService).listarPorConductor(7);

        despachoCtl.listar(false, null, null, 8);
        verify(despachoService).listarPorUnidad(8);

        assertNull(despachoCtl.obtenerMiDespachoActivo(null).getBody().getData());
        assertTrue(despachoCtl.listarMisDespachos(null).getBody().getData().isEmpty());

        var expectedDespacho = new DespachoTransporteResponseDTO();
        when(despachoService.obtenerDespachoActivoPorUsuario(7)).thenReturn(expectedDespacho);
        assertSame(expectedDespacho, despachoCtl.obtenerMiDespachoActivo(interno()).getBody().getData());

        despachoCtl.reportarIncidencia(7, Map.of("incidencia", "Neumático averiado"));
        verify(despachoService).reportarIncidencia(7, "Neumático averiado");

        despachoCtl.reportarIncidencia(7, Map.of());
        verify(despachoService).reportarIncidencia(7, "Incidencia no detallada");

        var unidadService = mock(IUnidadTransporteService.class);
        var unidadCtl = new UnidadTransporteController(unidadService);
        unidadCtl.listar(true, true);
        verify(unidadService).listarDisponibles();
        unidadCtl.listar(true, false);
        verify(unidadService).listarActivas();

        var conductorService = mock(IConductorService.class);
        var conductorCtl = new ConductorController(conductorService);
        conductorCtl.listar(true);
        verify(conductorService).listarActivos();
    }

    @Test
    @DisplayName("LiquidacionFinancieraController: filtro por estado, métricas, estimado, emisión con usuario y pago")
    void liquidacionFinancieraController_operacionesFinancieras() {
        var service = mock(ILiquidacionFinancieraService.class);
        var ctl = new LiquidacionFinancieraController(service);

        ctl.listar(false, "  PENDIENTE  ");
        verify(service).listarPorEstado("PENDIENTE");

        ctl.listar(true, null);
        verify(service).listarTodas(true);

        var metricas = new FinanzasMetricasResponseDTO();
        when(service.obtenerMetricas()).thenReturn(metricas);
        assertSame(metricas, ctl.obtenerMetricas().getBody().getData());

        var req = new LiquidacionFinancieraRequestDTO();
        var expected = new LiquidacionFinancieraResponseDTO();
        when(service.emitir(req, 7)).thenReturn(expected);
        var emitirRes = ctl.emitir(req, interno());
        assertEquals(201, emitirRes.getStatusCode().value());
        assertSame(expected, emitirRes.getBody().getData());
    }

    @Test
    @DisplayName("PortalController: autenticación B2B, validación de perfil, cotización, voucher y aislamiento de documentos")
    void portalController_flujoB2BYSeguridad() {
        var portalService = mock(IPortalService.class);
        var documentoService = mock(IDocumentoService.class);
        var ctl = new PortalController(portalService, documentoService);
        var clienteUser = externo();

        assertThrows(BadRequestException.class, () -> ctl.obtenerPerfil(null));
        assertThrows(BadRequestException.class, () -> ctl.obtenerPerfil(new UsuarioCliente()));

        var perfil = new PortalPerfilDTO();
        when(portalService.obtenerPerfil(clienteUser)).thenReturn(perfil);
        assertSame(perfil, ctl.obtenerPerfil(clienteUser).getBody().getData());

        var solDto = new PortalCotizacionSolicitudDTO();
        var cotDto = new PortalCotizacionDTO();
        when(portalService.solicitarCotizacion(11, solDto, 13)).thenReturn(cotDto);
        assertEquals(201, ctl.solicitarCotizacion(solDto, clienteUser).getStatusCode().value());

        var docExpected = List.of(new DocumentoResponseDTO());
        when(documentoService.listar(EntidadDocumento.EXPEDIENTE, null, 7, null, null)).thenReturn(docExpected);
        var docRes = ctl.listarDocumentosExpediente(7, clienteUser);
        assertSame(docExpected, docRes.getBody().getData());
        var order = inOrder(portalService, documentoService);
        order.verify(portalService).buscarExpedientePorId(11, 7);
        order.verify(documentoService).listar(EntidadDocumento.EXPEDIENTE, null, 7, null, null);
    }

    @Test
    @DisplayName("DocumentoController: control de acceso multi-rol en subida, listado, descarga, preview y eliminación")
    void documentoController_controlAccesoYDescarga() {
        var service = mock(IDocumentoService.class);
        var ctl = new DocumentoController(service);
        var userInterno = interno();
        var userPortal = externo();

        assertThrows(AccessDeniedException.class,
                () -> ctl.subir(List.of(), null, null, EntidadDocumento.GENERAL, null, null));
        assertThrows(AccessDeniedException.class,
                () -> ctl.listar(null, null, null, null, null, null));

        var files = List.of(mock(MultipartFile.class));
        var expectedDocs = List.of(new DocumentoResponseDTO());
        when(service.subirDocumentos(files, CategoriaDocumento.GENERAL, "QA", EntidadDocumento.COTIZACION, 55, 7))
                .thenReturn(expectedDocs);
        var subirRes = ctl.subir(files, CategoriaDocumento.GENERAL, "QA", EntidadDocumento.COTIZACION, 55, userInterno);
        assertEquals(201, subirRes.getStatusCode().value());
        verify(service).validarAccesoComercialCotizacion(userInterno, 55, true);

        when(service.listarParaUsuario(userInterno, EntidadDocumento.CLIENTE, null, null, 11, null))
                .thenReturn(expectedDocs);
        assertSame(expectedDocs, ctl.listar(EntidadDocumento.CLIENTE, null, null, 11, null, userInterno).getBody().getData());

        var entity = new DocumentoAdjunto();
        entity.setNombreOriginal("prueba QA.pdf");
        entity.setFormatoMime("application/pdf");
        var fileRes = new ByteArrayResource(new byte[]{1, 2, 3});
        when(service.obtenerEntidad(7)).thenReturn(entity);
        when(service.descargarArchivo(7)).thenReturn(fileRes);

        var downloadRes = ctl.descargar(7, userPortal);
        assertEquals(200, downloadRes.getStatusCode().value());
        assertTrue(downloadRes.getHeaders().getFirst("Content-Disposition").startsWith("attachment;"));
        assertTrue(downloadRes.getHeaders().getFirst("Content-Disposition").contains("prueba%20QA.pdf"));
        verify(service).validarPropiedadDocumentoCliente(11, entity);

        var previewRes = ctl.previsualizar(7, userInterno);
        assertTrue(previewRes.getHeaders().getFirst("Content-Disposition").startsWith("inline;"));

        var deleteRes = ctl.eliminar(7, userInterno);
        assertEquals(200, deleteRes.getStatusCode().value());
        verify(service).validarAccesoDocumentoComercial(userInterno, entity, true);
        verify(service).eliminar(7);
    }

    @Test
    @DisplayName("Catálogos, Sedes, Roles, Usuarios y AdminLogs: filtros, exportación PDF y descarga de logs")
    void catalogosYAdminControllers_filtrosReportesYLogs() throws Exception {
        var reporteService = mock(IReporteJasperService.class);
        byte[] pdfBytes = "%PDF-QA".getBytes();

        var mercanciaService = mock(ICatalogoMercanciaService.class);
        var mercanciaCtl = new CatalogoMercanciaController(mercanciaService, reporteService);
        mercanciaCtl.listar(true, "  GENERAL  ");
        verify(mercanciaService).listarPorCategoria("GENERAL");

        var puertoService = mock(ICatalogoPuertoService.class);
        var puertoCtl = new CatalogoPuertoController(puertoService, reporteService);
        puertoCtl.listar(true);
        verify(puertoService).listarActivos();

        var tipoCargaService = mock(ICatalogoTipoCargaService.class);
        var tipoCargaCtl = new CatalogoTipoCargaController(tipoCargaService, reporteService);
        tipoCargaCtl.listar(true);
        verify(tipoCargaService).listarActivos();

        var sedeService = mock(ISedeService.class);
        var sedeCtl = new SedeController(sedeService, reporteService);
        sedeCtl.listar(true);
        verify(sedeService).listarOperativas();

        var rolService = mock(IRolService.class);
        var rolCtl = new RolController(rolService);
        rolCtl.listar(true);
        verify(rolService).listarActivos();

        var usuarioService = mock(IUsuarioService.class);
        var usuarioCtl = new UsuarioController(usuarioService, reporteService);
        when(reporteService.exportarUsuariosPdf(true)).thenReturn(new ByteArrayInputStream(pdfBytes));
        var pdfUsuarios = usuarioCtl.exportarUsuariosPdf(true);
        assertEquals(200, pdfUsuarios.getStatusCode().value());
        assertTrue(pdfUsuarios.getHeaders().getFirst("Content-Disposition").contains("reporte-usuarios-activos.pdf"));

        var usuarioClienteService = mock(IUsuarioClienteService.class);
        var usuarioPortalAdminCtl = new UsuarioClienteAdminController(usuarioClienteService, reporteService);
        when(reporteService.exportarUsuariosPortalPdf(false)).thenReturn(new ByteArrayInputStream(pdfBytes));
        var pdfPortal = usuarioPortalAdminCtl.exportarUsuariosPortalPdf(false);
        assertEquals(200, pdfPortal.getStatusCode().value());
        assertTrue(pdfPortal.getHeaders().getFirst("Content-Disposition").contains("reporte-usuarios-portal-listado.pdf"));

        var adminLogsService = mock(IAdminLogsService.class);
        var logsCtl = new AdminLogsController(adminLogsService);
        var resourceWithoutName = mock(Resource.class);
        when(resourceWithoutName.getFilename()).thenReturn(null);
        when(adminLogsService.getLogFileResource("all")).thenReturn(resourceWithoutName);
        var logRes = logsCtl.downloadLog("all");
        assertEquals(200, logRes.getStatusCode().value());
        assertTrue(logRes.getHeaders().getFirst("Content-Disposition").contains("sga-backend.log"));
    }
}