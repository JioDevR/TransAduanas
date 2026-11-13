package pe.sga.functional;

import org.junit.jupiter.api.Test;
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
class BusinessFunctionalCasesTest extends FullStackTestBase {
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
  var r=call("GET",path,token,null);assertEquals(200,r.statusCode(),r.body());var data=tree(r).path("data");assertTrue(data.isArray());assertFalse(data.isEmpty());var first=data.get(0);assertTrue(first.path(idKey).asInt()>0,first.toString());
  var detail=call("GET",path+"/"+first.path(idKey).asInt(),token,null);assertEquals(200,detail.statusCode(),detail.body());assertEquals(first,tree(detail).path("data"));
  assertEquals(404,call("GET",path+"/2147483647",token,null).statusCode());
 }
 @Test void CP03_puertosListaDetalleReal()throws Exception{catalogo("/api/v1/catalogos/puertos","idPuerto",null);}
 @Test void CP04_validarAnticipoPersisteYRecalcula(){
  var cot=cotizacion(empresa());var pago=pagoRepo.saveAndFlush(CotizacionPago.builder().cotizacion(cot).codigoPago("QA-"+unique()).montoAbonado(new BigDecimal("25.00")).montoEquivalentePen(new BigDecimal("93.75")).metodoPago("TRANSFERENCIA").bancoDestino("QA BANCO").numeroOperacion(unique()).fechaOperacion(LocalDate.now()).build());
  int contador=user(RolUsuario.CONTADOR).getIdUsuario();
  pagos.validarPago(pago.getIdPago(),CotizacionPagoValidarDTO.builder().accion("APROBAR").build(),contador);
  assertEquals(EstadoPagoCotizacion.APROBADO,pagoRepo.findById(pago.getIdPago()).orElseThrow().getEstadoPago());
  var after=cotRepo.findById(cot.getIdCotizacion()).orElseThrow();assertEquals(0,new BigDecimal("25.00").compareTo(after.getTotalAbonado()));assertEquals(0,new BigDecimal("75.00").compareTo(after.getSaldoPendiente()));assertEquals("ADELANTO_APROBADO",after.getEstadoPago());
  pagos.validarPago(pago.getIdPago(),CotizacionPagoValidarDTO.builder().accion("OBSERVAR").observacionFinanzas("Comprobante por corregir").build(),contador);
  assertEquals(EstadoPagoCotizacion.OBSERVADO,pagoRepo.findById(pago.getIdPago()).orElseThrow().getEstadoPago());assertEquals(0,BigDecimal.ZERO.compareTo(cotRepo.findById(cot.getIdCotizacion()).orElseThrow().getTotalAbonado()));
 }
 @Test void CP06_tiposCargaListaDetalleReal()throws Exception{catalogo("/api/v1/catalogos/tipos-carga","idTipoCarga",null);}
 @Test void CP07_aperturaExigePagoYPersiste(){
  var cot=cotizacion(empresa());var dto=ExpedienteCrearDTO.builder().idCotizacion(cot.getIdCotizacion()).idAgenteAduanero(user(RolUsuario.AGENTE_ADUANERO).getIdUsuario()).build();
  var error=assertThrows(BadRequestException.class,()->expedientes.crear(dto));assertTrue(error.getMessage().contains("pago"));assertFalse(expRepo.existsByCotizacion_IdCotizacion(cot.getIdCotizacion()));
  cot.setEstadoPago("ADELANTO_APROBADO");cotRepo.saveAndFlush(cot);var result=expedientes.crear(dto);assertTrue(expRepo.existsById(result.getIdExpediente()));assertEquals(cot.getIdCotizacion(),expRepo.findById(result.getIdExpediente()).orElseThrow().getCotizacion().getIdCotizacion());assertThrows(BadRequestException.class,()->expedientes.crear(dto));
 }
 @Test void CP09_sedesListaDetalleReal()throws Exception{catalogo("/api/v1/sedes","idSede",login(user(RolUsuario.ADMINISTRADOR_TI)));}
 @Test void CP10_consultaListadoYDetalleExpediente()throws Exception{
  var c=empresa();var exp=expediente(c);String token=cuentaPortal(c);
  var detail=call("GET","/api/v1/portal/expedientes/"+exp.getIdExpediente(),token,null);assertEquals(200,detail.statusCode(),detail.body());assertEquals(exp.getCodigoExpediente(),tree(detail).path("data").path("codigoExpediente").asText());
  var list=call("GET","/api/v1/portal/expedientes",token,null);assertEquals(200,list.statusCode(),list.body());assertEquals(1,tree(list).path("data").size());assertEquals(exp.getIdExpediente().intValue(),tree(list).path("data").get(0).path("idExpediente").asInt());
 }
 @Test void CP11_solicitudPortalCreaRegistroYRechazaInvalido()throws Exception{
  var c=empresa();String token=cuentaPortal(c);long before=cotRepo.count();
  var r=call("POST","/api/v1/portal/cotizaciones/solicitar",token,Map.of("regimenAduanero","IMPORTACION_DEFINITIVA","valorFob",1000,"validezDias",15));assertEquals(201,r.statusCode(),r.body());int id=tree(r).path("data").path("idCotizacion").asInt();var stored=cotRepo.findById(id).orElseThrow();assertEquals(c.getIdCliente(),stored.getCliente().getIdCliente());assertEquals(0,new BigDecimal("1000.00").compareTo(stored.getValorFob()));assertEquals(before+1,cotRepo.count());
  var bad=call("POST","/api/v1/portal/cotizaciones/solicitar",token,Map.of("regimenAduanero","IMPORTACION_DEFINITIVA","valorFob",-1));assertEquals(400,bad.statusCode(),bad.body());assertEquals(before+1,cotRepo.count());
 }
 @Test void CP12_aceptarCotizacionPersisteSaldoYObservacion(){
  var c=empresa();var cot=cotizacion(c);portal.aceptarCotizacion(c.getIdCliente(),cot.getIdCotizacion());
  var stored=cotRepo.findById(cot.getIdCotizacion()).orElseThrow();assertEquals("SIN_PAGO",stored.getEstadoPago());assertEquals(0,new BigDecimal("100.00").compareTo(stored.getSaldoPendiente()));assertTrue(stored.getObservaciones().contains("Aceptada formalmente por el cliente"));
  assertThrows(BadRequestException.class,()->portal.aceptarCotizacion(c.getIdCliente(),cot.getIdCotizacion()));
 }
 @Test void CP13_liquidacionEmitePersisteYConsulta(){
  var exp=expediente(empresa());var dto=LiquidacionFinancieraRequestDTO.builder().idExpediente(exp.getIdExpediente()).totalTributosSunat(new BigDecimal("1000.00")).honorariosAgencia(new BigDecimal("200.00")).fleteTransporteLocal(new BigDecimal("350.00")).tipoCambio(new BigDecimal("3.75")).build();
  var result=liquidaciones.emitir(dto,user(RolUsuario.CONTADOR).getIdUsuario());var stored=liqRepo.findById(result.getIdLiquidacion()).orElseThrow();assertEquals(0,new BigDecimal("550.00").compareTo(stored.getSubtotalImponible()));assertEquals(0,new BigDecimal("99.00").compareTo(stored.getIgvMonto()));assertEquals(0,new BigDecimal("1649.00").compareTo(stored.getTotalGeneralPen()));assertEquals(0,new BigDecimal("439.73").compareTo(stored.getTotalGeneralUsd()));assertEquals(result.getIdLiquidacion(),liquidaciones.buscarPorExpediente(exp.getIdExpediente()).getIdLiquidacion());
 }
 @Test void CP15_mercanciasListaDetalleCodigoReal()throws Exception{
  mercanciaRepo.saveAndFlush(CatalogoMercancia.builder().codigo("QA-"+unique()).nombre("Mercancia QA").categoria("GENERAL").build());
  String path="/api/v1/catalogos/mercancias";catalogo(path,"idMercancia",null);var list=tree(call("GET",path,null,null)).path("data");var first=list.get(0);String code=first.path("codigo").asText();assertFalse(code.isBlank(),first.toString());var r=call("GET",path+"/codigo/"+code,null,null);assertEquals(200,r.statusCode(),r.body());assertEquals(first,tree(r).path("data"));
 }

 @Test void CP01_formalizacionProspectoACliente(){
  var created=clienteService.crear(dto(unique()));assertTrue(created.getEsProspecto());
  var formal=clienteService.convertirEnCliente(created.getIdCliente());assertEquals("CLIENTE",formal.getEstadoCliente());assertFalse(formal.getEsProspecto());assertEquals("CLIENTE",clientes.findById(created.getIdCliente()).orElseThrow().getEstadoCliente());
 }
 @Test void CP02_registroClienteYDuplicado(){
  var input=dto(unique());long before=clientes.count();var created=clienteService.crear(input);var stored=clientes.findById(created.getIdCliente()).orElseThrow();assertEquals(input.getNumeroDocumento(),stored.getNumeroDocumento());assertEquals(input.getCorreoContacto(),stored.getCorreoContacto());
  assertThrows(BadRequestException.class,()->clienteService.crear(input));assertEquals(before+1,clientes.count());
 }
 @Test void CP05_busquedaClientesPorRazonSocial(){
  var input=dto(unique());input.setRazonSocial("Empresa Busqueda "+unique());var created=clienteService.crear(input);
  var found=clienteService.buscarPorTermino(input.getRazonSocial().toLowerCase(),true);assertEquals(1,found.size());assertEquals(created.getIdCliente(),found.getFirst().getIdCliente());
 }
 @Test void CP08_actualizacionClientePersiste(){
  var input=dto(unique());var created=clienteService.crear(input);input.setRazonSocial("Empresa Actualizada "+unique());clienteService.actualizar(created.getIdCliente(),input);
  assertEquals(input.getRazonSocial(),clientes.findById(created.getIdCliente()).orElseThrow().getRazonSocial());assertEquals(input.getRazonSocial(),clienteService.buscarPorId(created.getIdCliente()).getRazonSocial());
 }
 @Test void CP14_cargaYDescargaConservaContenido()throws Exception{
  var c=empresa();String token=cuentaPortal(c);String boundary="QA_BOUNDARY_"+unique();String content="ARCHIVO_FUNCIONAL_"+unique();
  String body="--"+boundary+"\r\nContent-Disposition: form-data; name=\"files\"; filename=\"funcional.txt\"\r\nContent-Type: text/plain\r\n\r\n"+content+"\r\n--"+boundary+"--\r\n";
  var request=java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://127.0.0.1:"+port+"/api/v1/documentos/upload?categoria=GENERAL&entidadTipo=CLIENTE&idEntidad="+c.getIdCliente())).header("Authorization","Bearer "+token).header("Content-Type","multipart/form-data; boundary="+boundary).POST(java.net.http.HttpRequest.BodyPublishers.ofString(body)).build();
  var uploaded=http.send(request,java.net.http.HttpResponse.BodyHandlers.ofString());assertEquals(201,uploaded.statusCode(),uploaded.body());int id=tree(uploaded).path("data").get(0).path("idDocumento").asInt();assertTrue(documentoRepo.existsById(id));
  var downloaded=call("GET","/api/v1/documentos/"+id+"/descargar",token,null);assertEquals(200,downloaded.statusCode(),downloaded.body());assertEquals(content,downloaded.body());assertTrue(downloaded.headers().firstValue("Content-Disposition").orElse("").contains("funcional.txt"));
 }
}
