package pe.sga.functional;

import org.junit.jupiter.api.Test;
import pe.sga.support.PruebaDocente;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestWatcher;
import org.springframework.beans.factory.annotation.Autowired;
import pe.sga.entity.*;
import pe.sga.entity.enums.*;
import pe.sga.dto.portal.PortalCotizacionSolicitudDTO;
import pe.sga.dto.request.*;
import pe.sga.repository.*;
import pe.sga.service.*;
import pe.sga.exception.BadRequestException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

/** Servicios y repositorios reales; H2 aislada, sin mocks ni datos productivos. */
@TestMethodOrder(MethodOrderer.MethodName.class)
class BusinessFunctionalCasesTest extends FullStackTestBase {
 private String caso;
 @BeforeEach void presentarCaso(TestInfo info) {
  String nombre = info.getTestMethod().orElseThrow().getName();
  caso = nombre.substring(0, 4);
  evidencia("INICIO", nombre);
  evidencia("ENTORNO", "H2 temporal; datos ficticios; servicios y repositorios reales. No modifica MySQL ni demuestra la interfaz web.");
 }
 @RegisterExtension final TestWatcher resultado = new TestWatcher() {
  @Override public void testSuccessful(ExtensionContext context) {
   evidencia("RESULTADO", "APROBADO: terminaron todas las verificaciones del caso.");
  }
  @Override public void testFailed(ExtensionContext context, Throwable causa) {
   evidencia("RESULTADO", "NO APROBADO: " + causa.getClass().getSimpleName() + ": " + causa.getMessage());
  }
  @Override public void testAborted(ExtensionContext context, Throwable causa) {
   evidencia("RESULTADO", "INTERRUMPIDO: " + causa.getMessage());
  }
 };
 private void evidencia(String etapa, Object detalle) {
  System.out.printf("[%s] %s | %s%n", caso, etapa, detalle);
 }
 @Autowired IPortalService portal;
 @Autowired IClienteService clienteService;
 @Autowired IDocumentoAdjuntoRepository documentoRepo;
 @Autowired IExpedienteService expedientes;
 @Autowired ICotizacionPagoService pagos;
 @Autowired ILiquidacionFinancieraService liquidaciones;
 @Autowired CotizacionRepository cotRepo;
 @Autowired CatalogoMercanciaRepository mercanciaRepo;
 @Autowired CotizacionPagoRepository pagoRepo;
 @Autowired ExpedienteRepository expRepo;
 @Autowired LiquidacionFinancieraRepository liqRepo;
 Cotizacion cotizacion(Cliente c){
  var dto=PortalCotizacionSolicitudDTO.builder().regimenAduanero("IMPORTACION_DEFINITIVA").valorFob(new BigDecimal("1000.00")).build();
  int id=portal.solicitarCotizacion(c.getIdCliente(),dto,null).getIdCotizacion();
  var cot=cotRepo.findById(id).orElseThrow();cot.setEstado(EstadoCotizacion.APROBADA);cot.setTotalCotizado(new BigDecimal("100.00"));return cotRepo.saveAndFlush(cot);
 }
 Expediente expediente(Cliente c){var cot=cotizacion(c);cot.setEstadoPago("ADELANTO_APROBADO");cotRepo.saveAndFlush(cot);var dto=ExpedienteCrearDTO.builder().idCotizacion(cot.getIdCotizacion()).idAgenteAduanero(user(RolUsuario.AGENTE_ADUANERO).getIdUsuario()).build();return expRepo.findById(expedientes.crear(dto).getIdExpediente()).orElseThrow();}
 String cuentaPortal(Cliente c)throws Exception{
  String n="portal"+unique();usuariosPortal.saveAndFlush(UsuarioCliente.builder().cliente(c).username(n).passwordHash(encoder.encode("QA_Portal123!")).estadoActivo(true).build());
  var r=call("POST","/api/v1/portal/auth/login",null,Map.of("username",n,"password","QA_Portal123!"));assertEquals(200,r.statusCode(),r.body());return tree(r).path("data").path("token").asText();
 }
 void catalogo(String path,String idKey,String token)throws Exception{
  evidencia("ACCION", "Consultar listado y detalle de " + path);
  var r=call("GET",path,token,null);assertEquals(200,r.statusCode(),r.body());var data=tree(r).path("data");assertTrue(data.isArray());assertFalse(data.isEmpty());var first=data.get(0);assertTrue(first.path(idKey).asInt()>0,first.toString());
  var detail=call("GET",path+"/"+first.path(idKey).asInt(),token,null);assertEquals(200,detail.statusCode(),detail.body());assertEquals(first,tree(detail).path("data"));
  evidencia("VERIFICADO", "Listado HTTP " + r.statusCode() + "; registros=" + data.size() + "; primer registro=" + first);
  evidencia("VERIFICADO", "Detalle HTTP " + detail.statusCode() + "; contenido identico al listado=" + tree(detail).path("data"));
  var inexistente=call("GET",path+"/2147483647",token,null);
  assertEquals(404,inexistente.statusCode());
  evidencia("RECHAZO ESPERADO", "ID 2147483647; HTTP " + inexistente.statusCode() + "; respuesta=" + inexistente.body());
 }
 @Test @Tag("sistema") @PruebaDocente(codigo="PS01", tipo="Sistema del backend", herramientas="JUnit Jupiter 6 + Java HttpClient + Spring Boot Test + H2; Maven Surefire", objetivo="HTTP 200 en listado y detalle coincidente; identificador inexistente devuelve 404.")
 void CP03_puertosListaDetalleReal()throws Exception{catalogo("/api/v1/catalogos/puertos","idPuerto",null);}
 @Test @Tag("integracion") void CP04_validarAnticipoPersisteYRecalcula(){
  var cot=cotizacion(empresa());var pago=pagoRepo.saveAndFlush(CotizacionPago.builder().cotizacion(cot).codigoPago("QA-"+unique()).montoAbonado(new BigDecimal("25.00")).montoEquivalentePen(new BigDecimal("93.75")).metodoPago("TRANSFERENCIA").bancoDestino("QA BANCO").numeroOperacion(unique()).fechaOperacion(LocalDate.now()).build());
  evidencia("DATOS", "Cotizacion ID=" + cot.getIdCotizacion() + "; total USD=" + cot.getTotalCotizado() + "; pago ID=" + pago.getIdPago() + "; abono USD=" + pago.getMontoAbonado());
  int contador=user(RolUsuario.CONTADOR).getIdUsuario();
  pagos.validarPago(pago.getIdPago(),CotizacionPagoValidarDTO.builder().accion("APROBAR").build(),contador);
  assertEquals(EstadoPagoCotizacion.APROBADO,pagoRepo.findById(pago.getIdPago()).orElseThrow().getEstadoPago());
  var after=cotRepo.findById(cot.getIdCotizacion()).orElseThrow();assertEquals(0,new BigDecimal("25.00").compareTo(after.getTotalAbonado()));assertEquals(0,new BigDecimal("75.00").compareTo(after.getSaldoPendiente()));assertEquals("ADELANTO_APROBADO",after.getEstadoPago());
  evidencia("VERIFICADO", "Pago=" + pagoRepo.findById(pago.getIdPago()).orElseThrow().getEstadoPago() + "; total abonado=" + after.getTotalAbonado() + "; saldo=" + after.getSaldoPendiente() + "; estado cotizacion=" + after.getEstadoPago());
  pagos.validarPago(pago.getIdPago(),CotizacionPagoValidarDTO.builder().accion("OBSERVAR").observacionFinanzas("Comprobante por corregir").build(),contador);
  assertEquals(EstadoPagoCotizacion.OBSERVADO,pagoRepo.findById(pago.getIdPago()).orElseThrow().getEstadoPago());assertEquals(0,BigDecimal.ZERO.compareTo(cotRepo.findById(cot.getIdCotizacion()).orElseThrow().getTotalAbonado()));
  evidencia("VERIFICADO", "Al observar: pago=" + pagoRepo.findById(pago.getIdPago()).orElseThrow().getEstadoPago() + "; total aprobado=" + cotRepo.findById(cot.getIdCotizacion()).orElseThrow().getTotalAbonado());
 }
 @Test @Tag("sistema") void CP06_tiposCargaListaDetalleReal()throws Exception{catalogo("/api/v1/catalogos/tipos-carga","idTipoCarga",null);}
 @Test @Tag("integracion") @PruebaDocente(codigo="PI03", tipo="Integracion", herramientas="JUnit Jupiter 6 + Spring Boot Test + Spring Data JPA + H2; Maven Surefire", objetivo="Sin pago no crea expediente; con adelanto aprobado lo persiste y vincula; una segunda apertura se rechaza.")
 void CP07_aperturaExigePagoYPersiste(){
  var cot=cotizacion(empresa());var dto=ExpedienteCrearDTO.builder().idCotizacion(cot.getIdCotizacion()).idAgenteAduanero(user(RolUsuario.AGENTE_ADUANERO).getIdUsuario()).build();
  evidencia("DATOS", "Cotizacion ID=" + cot.getIdCotizacion() + "; estado de pago=" + cot.getEstadoPago());
  var error=assertThrows(BadRequestException.class,()->expedientes.crear(dto));
  assertTrue(error.getMessage().contains("pago"));assertFalse(expRepo.existsByCotizacion_IdCotizacion(cot.getIdCotizacion()));
  evidencia("RECHAZO ESPERADO", error.getMessage() + "; expediente existente=" + expRepo.existsByCotizacion_IdCotizacion(cot.getIdCotizacion()));
  cot.setEstadoPago("ADELANTO_APROBADO");cotRepo.saveAndFlush(cot);var result=expedientes.crear(dto);assertTrue(expRepo.existsById(result.getIdExpediente()));assertEquals(cot.getIdCotizacion(),expRepo.findById(result.getIdExpediente()).orElseThrow().getCotizacion().getIdCotizacion());var duplicado=assertThrows(BadRequestException.class,()->expedientes.crear(dto));
  evidencia("VERIFICADO", "Con ADELANTO_APROBADO se guardo expediente ID=" + result.getIdExpediente() + "; cotizacion asociada=" + expRepo.findById(result.getIdExpediente()).orElseThrow().getCotizacion().getIdCotizacion());
  evidencia("RECHAZO ESPERADO", "Segunda apertura: " + duplicado.getMessage());
 }
 @Test @Tag("sistema") void CP09_sedesListaDetalleReal()throws Exception{catalogo("/api/v1/sedes","idSede",login(user(RolUsuario.ADMINISTRADOR_TI)));}
 @Test @Tag("sistema") void CP10_consultaListadoYDetalleExpediente()throws Exception{
  var c=empresa();var exp=expediente(c);String token=cuentaPortal(c);
  var detail=call("GET","/api/v1/portal/expedientes/"+exp.getIdExpediente(),token,null);assertEquals(200,detail.statusCode(),detail.body());assertEquals(exp.getCodigoExpediente(),tree(detail).path("data").path("codigoExpediente").asText());
  var list=call("GET","/api/v1/portal/expedientes",token,null);assertEquals(200,list.statusCode(),list.body());assertEquals(1,tree(list).path("data").size());assertEquals(exp.getIdExpediente().intValue(),tree(list).path("data").get(0).path("idExpediente").asInt());
  evidencia("DATOS", "Cliente ID=" + c.getIdCliente() + "; expediente preparado ID=" + exp.getIdExpediente() + "; codigo=" + exp.getCodigoExpediente());
  evidencia("VERIFICADO", "Detalle HTTP " + detail.statusCode() + "; respuesta=" + detail.body());
  evidencia("VERIFICADO", "Listado HTTP " + list.statusCode() + "; respuesta=" + list.body());
 }
 @Test @Tag("sistema") @PruebaDocente(codigo="PS02", tipo="Sistema del backend", herramientas="JUnit Jupiter 6 + Java HttpClient + Spring Boot Test + H2; Maven Surefire", objetivo="Solicitud FOB 1000 devuelve 201 y persiste; FOB negativo devuelve 400 sin agregar filas.")
 void CP11_solicitudPortalCreaRegistroYRechazaInvalido()throws Exception{
  var c=empresa();String token=cuentaPortal(c);long before=cotRepo.count();
  var r=call("POST","/api/v1/portal/cotizaciones/solicitar",token,Map.of("regimenAduanero","IMPORTACION_DEFINITIVA","valorFob",1000,"validezDias",15));assertEquals(201,r.statusCode(),r.body());int id=tree(r).path("data").path("idCotizacion").asInt();var stored=cotRepo.findById(id).orElseThrow();assertEquals(c.getIdCliente(),stored.getCliente().getIdCliente());assertEquals(0,new BigDecimal("1000.00").compareTo(stored.getValorFob()));assertEquals(before+1,cotRepo.count());
  evidencia("DATOS", "Cliente ID=" + c.getIdCliente() + "; FOB solicitado=1000; filas antes=" + before);
  evidencia("VERIFICADO", "Solicitud HTTP " + r.statusCode() + "; cotizacion ID=" + id + "; cliente guardado=" + stored.getCliente().getIdCliente() + "; FOB guardado=" + stored.getValorFob() + "; filas despues=" + cotRepo.count());
  var bad=call("POST","/api/v1/portal/cotizaciones/solicitar",token,Map.of("regimenAduanero","IMPORTACION_DEFINITIVA","valorFob",-1));assertEquals(400,bad.statusCode(),bad.body());assertEquals(before+1,cotRepo.count());
  evidencia("RECHAZO ESPERADO", "FOB=-1; HTTP " + bad.statusCode() + "; respuesta=" + bad.body() + "; filas finales=" + cotRepo.count() + "; no se agrego otra cotizacion.");
 }
 @Test @Tag("integracion") void CP12_aceptarCotizacionPersisteSaldoYObservacion(){
  var c=empresa();var cot=cotizacion(c);portal.aceptarCotizacion(c.getIdCliente(),cot.getIdCotizacion());
  var stored=cotRepo.findById(cot.getIdCotizacion()).orElseThrow();assertEquals("SIN_PAGO",stored.getEstadoPago());assertEquals(0,new BigDecimal("100.00").compareTo(stored.getSaldoPendiente()));assertTrue(stored.getObservaciones().contains("Aceptada formalmente por el cliente"));
  evidencia("VERIFICADO", "Cotizacion ID=" + stored.getIdCotizacion() + "; estado pago=" + stored.getEstadoPago() + "; saldo USD=" + stored.getSaldoPendiente() + "; observaciones=" + stored.getObservaciones());
  var repetida=assertThrows(BadRequestException.class,()->portal.aceptarCotizacion(c.getIdCliente(),cot.getIdCotizacion()));
  evidencia("RECHAZO ESPERADO", "Segunda aceptacion: " + repetida.getMessage());
 }
 @Test @Tag("integracion") @PruebaDocente(codigo="PI04", tipo="Integracion", herramientas="JUnit Jupiter 6 + Spring Boot Test + Spring Data JPA + H2; Maven Surefire", objetivo="Verifica subtotal PEN 550, IGV 99, total PEN 1649, USD 439.73 y recupera por expediente la liquidacion guardada en H2.")
 void CP13_liquidacionEmitePersisteYConsulta(){
  var exp=expediente(empresa());var dto=LiquidacionFinancieraRequestDTO.builder().idExpediente(exp.getIdExpediente()).totalTributosSunat(new BigDecimal("1000.00")).honorariosAgencia(new BigDecimal("200.00")).fleteTransporteLocal(new BigDecimal("350.00")).tipoCambio(new BigDecimal("3.75")).build();
  var result=liquidaciones.emitir(dto,user(RolUsuario.CONTADOR).getIdUsuario());var stored=liqRepo.findById(result.getIdLiquidacion()).orElseThrow();assertEquals(0,new BigDecimal("550.00").compareTo(stored.getSubtotalImponible()));assertEquals(0,new BigDecimal("99.00").compareTo(stored.getIgvMonto()));assertEquals(0,new BigDecimal("1649.00").compareTo(stored.getTotalGeneralPen()));assertEquals(0,new BigDecimal("439.73").compareTo(stored.getTotalGeneralUsd()));assertEquals(result.getIdLiquidacion(),liquidaciones.buscarPorExpediente(exp.getIdExpediente()).getIdLiquidacion());
  evidencia("DATOS", "Expediente ID=" + exp.getIdExpediente() + "; tributos=1000; honorarios=200; transporte=350; TC=" + stored.getTipoCambio());
  evidencia("VERIFICADO", "Liquidacion ID=" + stored.getIdLiquidacion() + "; subtotal PEN=" + stored.getSubtotalImponible() + "; IGV=" + stored.getIgvMonto() + "; total PEN=" + stored.getTotalGeneralPen() + "; total USD=" + stored.getTotalGeneralUsd());
  evidencia("VERIFICADO", "Consulta por expediente devuelve ID=" + liquidaciones.buscarPorExpediente(exp.getIdExpediente()).getIdLiquidacion());
 }
 @Test @Tag("sistema") void CP15_mercanciasListaDetalleCodigoReal()throws Exception{
  mercanciaRepo.saveAndFlush(CatalogoMercancia.builder().codigo("QA-"+unique()).nombre("Mercancia QA").categoria("GENERAL").build());
  String path="/api/v1/catalogos/mercancias";catalogo(path,"idMercancia",null);var list=tree(call("GET",path,null,null)).path("data");var first=list.get(0);String code=first.path("codigo").asText();assertFalse(code.isBlank(),first.toString());var r=call("GET",path+"/codigo/"+code,null,null);assertEquals(200,r.statusCode(),r.body());assertEquals(first,tree(r).path("data"));
  evidencia("VERIFICADO", "Busqueda por codigo=" + code + "; HTTP " + r.statusCode() + "; registro=" + tree(r).path("data"));
 }

 @Test @Tag("integracion") @PruebaDocente(codigo="PI01", tipo="Integracion", herramientas="JUnit Jupiter 6 + Spring Boot Test + Spring Data JPA + H2; Maven Surefire", objetivo="El servicio convierte PROSPECTO a CLIENTE; esProspecto=false y el nuevo estado se consulta en H2.")
 void CP01_formalizacionProspectoACliente(){
  var created=clienteService.crear(dto(unique()));assertTrue(created.getEsProspecto());
  evidencia("ANTES", "Cliente ID=" + created.getIdCliente() + "; documento=" + created.getNumeroDocumento() + "; estado=" + created.getEstadoCliente() + "; esProspecto=" + created.getEsProspecto());
  var formal=clienteService.convertirEnCliente(created.getIdCliente());assertEquals("CLIENTE",formal.getEstadoCliente());assertFalse(formal.getEsProspecto());assertEquals("CLIENTE",clientes.findById(created.getIdCliente()).orElseThrow().getEstadoCliente());
  evidencia("VERIFICADO", "Tras convertir: estado respuesta=" + formal.getEstadoCliente() + "; esProspecto=" + formal.getEsProspecto() + "; estado leido de la base=" + clientes.findById(created.getIdCliente()).orElseThrow().getEstadoCliente());
 }
 @Test @Tag("integracion") @PruebaDocente(codigo="PI02", tipo="Integracion", herramientas="JUnit Jupiter 6 + Spring Boot Test + Spring Data JPA + H2; Maven Surefire", objetivo="Documento y correo se leen de H2; un duplicado genera rechazo y el conteo permanece sin otra fila.")
 void CP02_registroClienteYDuplicado(){
  var input=dto(unique());long before=clientes.count();var created=clienteService.crear(input);var stored=clientes.findById(created.getIdCliente()).orElseThrow();assertEquals(input.getNumeroDocumento(),stored.getNumeroDocumento());assertEquals(input.getCorreoContacto(),stored.getCorreoContacto());
  long trasRegistro=clientes.count();assertEquals(before+1,trasRegistro);
  evidencia("DATOS", "Documento=" + input.getNumeroDocumento() + "; correo=" + input.getCorreoContacto() + "; filas antes=" + before);
  evidencia("REGISTRO VERIFICADO", "ID=" + stored.getIdCliente() + "; documento guardado=" + stored.getNumeroDocumento() + "; correo guardado=" + stored.getCorreoContacto() + "; filas despues=" + trasRegistro);
  var rechazo=assertThrows(BadRequestException.class,()->clienteService.crear(input));assertEquals(trasRegistro,clientes.count());
  evidencia("RECHAZO ESPERADO", "Mismo documento=" + input.getNumeroDocumento() + "; motivo=" + rechazo.getMessage());
  evidencia("VERIFICADO", "Filas tras duplicado=" + clientes.count() + "; filas tras registro=" + trasRegistro + "; no se agrego otra fila.");
 }
 @Test @Tag("integracion") void CP05_busquedaClientesPorRazonSocial(){
  var input=dto(unique());input.setRazonSocial("Empresa Busqueda "+unique());var created=clienteService.crear(input);
  var found=clienteService.buscarPorTermino(input.getRazonSocial().toLowerCase(),true);assertEquals(1,found.size());assertEquals(created.getIdCliente(),found.getFirst().getIdCliente());
  evidencia("DATOS", "Razon social registrada=" + input.getRazonSocial() + "; termino buscado=" + input.getRazonSocial().toLowerCase());
  evidencia("VERIFICADO", "Coincidencias=" + found.size() + "; ID esperado=" + created.getIdCliente() + "; ID encontrado=" + found.getFirst().getIdCliente());
 }
 @Test @Tag("integracion") void CP08_actualizacionClientePersiste(){
  var input=dto(unique());var created=clienteService.crear(input);evidencia("ANTES", "Cliente ID=" + created.getIdCliente() + "; razon social=" + created.getRazonSocial());input.setRazonSocial("Empresa Actualizada "+unique());clienteService.actualizar(created.getIdCliente(),input);
  assertEquals(input.getRazonSocial(),clientes.findById(created.getIdCliente()).orElseThrow().getRazonSocial());assertEquals(input.getRazonSocial(),clienteService.buscarPorId(created.getIdCliente()).getRazonSocial());
  evidencia("VERIFICADO", "Razon social solicitada=" + input.getRazonSocial() + "; guardada=" + clientes.findById(created.getIdCliente()).orElseThrow().getRazonSocial() + "; consultada=" + clienteService.buscarPorId(created.getIdCliente()).getRazonSocial());
 }
 @Test @Tag("sistema") @PruebaDocente(codigo="PS03", tipo="Sistema del backend", herramientas="JUnit Jupiter 6 + Java HttpClient + Spring Boot Test + H2; Maven Surefire", objetivo="Carga multipart 201, registro persistido y descarga 200 con nombre y contenido exactos.")
 void CP14_cargaYDescargaConservaContenido()throws Exception{
  var c=empresa();String token=cuentaPortal(c);String boundary="QA_BOUNDARY_"+unique();String content="ARCHIVO_FUNCIONAL_"+unique();
  String body="--"+boundary+"\r\nContent-Disposition: form-data; name=\"files\"; filename=\"funcional.txt\"\r\nContent-Type: text/plain\r\n\r\n"+content+"\r\n--"+boundary+"--\r\n";
  var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/api/v1/documentos/upload?categoria=GENERAL&entidadTipo=CLIENTE&idEntidad="+c.getIdCliente())).header("Authorization","Bearer "+token).header("Content-Type","multipart/form-data; boundary="+boundary).POST(java.net.http.HttpRequest.BodyPublishers.ofString(body)).build();
  var uploaded=http.send(request,java.net.http.HttpResponse.BodyHandlers.ofString());assertEquals(201,uploaded.statusCode(),uploaded.body());int id=tree(uploaded).path("data").get(0).path("idDocumento").asInt();assertTrue(documentoRepo.existsById(id));
  evidencia("DATOS", "Cliente ID=" + c.getIdCliente() + "; archivo=funcional.txt; contenido original=" + content);
  evidencia("VERIFICADO", "Carga HTTP " + uploaded.statusCode() + "; documento ID=" + id + "; registro existe=" + documentoRepo.existsById(id));
  var downloaded=call("GET","/api/v1/documentos/"+id+"/descargar",token,null);assertEquals(200,downloaded.statusCode(),downloaded.body());assertEquals(content,downloaded.body());assertTrue(downloaded.headers().firstValue("Content-Disposition").orElse("").contains("funcional.txt"));
  evidencia("VERIFICADO", "Descarga HTTP " + downloaded.statusCode() + "; contenido recibido=" + downloaded.body() + "; cabecera=" + downloaded.headers().firstValue("Content-Disposition").orElse(""));
  evidencia("VERIFICADO", "Contenido recibido identico al original.");
 }
}
