package pe.sga.functional;
import org.junit.jupiter.api.Test;
import pe.sga.entity.*;
import pe.sga.entity.enums.*;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class AcceptanceBusinessTest extends FullStackTestBase {
 UsuarioCliente cuenta(Cliente c){return usuariosPortal.saveAndFlush(UsuarioCliente.builder().cliente(c).username("portal"+unique()).passwordHash(encoder.encode("QA_Portal123!")).estadoActivo(true).build());}
 String portalLogin(UsuarioCliente uc)throws Exception{var r=call("POST","/api/v1/portal/auth/login",null,Map.of("username",uc.getUsername(),"password","QA_Portal123!"));assertEquals(200,r.statusCode(),r.body());return tree(r).path("data").path("token").asText();}
 @Test void AC01_dadoProspectoCuandoFormalizaEntoncesEsCliente()throws Exception{var token=login(user(RolUsuario.AGENTE_COMERCIAL));int id=create(token,unique());var before=call("GET","/api/v1/clientes/"+id,token,null);assertTrue(tree(before).path("data").path("esProspecto").asBoolean());assertEquals(200,call("PATCH","/api/v1/clientes/"+id+"/convertir-cliente",token,null).statusCode());var after=call("GET","/api/v1/clientes/"+id,token,null);assertEquals("CLIENTE",tree(after).path("data").path("estadoCliente").asText());assertFalse(tree(after).path("data").path("esProspecto").asBoolean());assertEquals("CLIENTE",clientes.findById(id).orElseThrow().getEstadoCliente());}
 @Test void AC02_dadoClienteInactivoCuandoReactivaEntoncesVisible()throws Exception{var token=login(user(RolUsuario.AGENTE_COMERCIAL));int id=create(token,unique());assertEquals(200,call("DELETE","/api/v1/clientes/"+id,token,null).statusCode());assertFalse(clientes.findById(id).orElseThrow().getEstadoActivo());assertEquals(200,call("PATCH","/api/v1/clientes/"+id+"/reactivar",token,null).statusCode());var r=call("GET","/api/v1/clientes?soloActivos=true",token,null);assertEquals(200,r.statusCode());boolean found=false;for(var n:tree(r).path("data"))if(n.path("idCliente").asInt()==id)found=true;assertTrue(found);assertTrue(clientes.findById(id).orElseThrow().getEstadoActivo());}
 @Test void AC03_dadoPortalCuandoConsultaEntoncesPerfilPropio()throws Exception{var c=empresa();var uc=cuenta(c);String token=portalLogin(uc);var r=call("GET","/api/v1/portal/perfil",token,null);assertEquals(200,r.statusCode(),r.body());assertEquals(c.getIdCliente().intValue(),tree(r).path("data").path("idCliente").asInt());assertEquals(c.getNumeroDocumento(),tree(r).path("data").path("ruc").asText());assertEquals(403,call("GET","/api/v1/usuarios",token,null).statusCode());}
 @Test void AC04_dadoDocumentoCuandoOtroClienteDescargaEntoncesDenegado()throws Exception{
  var c=empresa();String owner=portalLogin(cuenta(c));String other=portalLogin(cuenta(empresa()));String boundary="QA_BOUNDARY_2026";
  String body="--"+boundary+"\r\nContent-Disposition: form-data; name=\"files\"; filename=\"qa-evidencia.txt\"\r\nContent-Type: text/plain\r\n\r\nEVIDENCIA_QA_2026\r\n--"+boundary+"--\r\n";
  var request=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+"/api/v1/documentos/upload?categoria=GENERAL&entidadTipo=CLIENTE&idEntidad="+c.getIdCliente())).header("Authorization","Bearer "+owner).header("Content-Type","multipart/form-data; boundary="+boundary).POST(HttpRequest.BodyPublishers.ofString(body)).build();
  var upload=http.send(request,HttpResponse.BodyHandlers.ofString());assertEquals(201,upload.statusCode(),upload.body());int id=tree(upload).path("data").get(0).path("idDocumento").asInt();assertTrue(id>0);
  var download=call("GET","/api/v1/documentos/"+id+"/descargar",owner,null);assertEquals(200,download.statusCode(),download.body());assertEquals("EVIDENCIA_QA_2026",download.body());assertTrue(download.headers().firstValue("Content-Disposition").orElse("").contains("qa-evidencia.txt"));var denied=call("GET","/api/v1/documentos/"+id+"/descargar",other,null);assertEquals(403,denied.statusCode(),denied.body());assertFalse(denied.body().contains("EVIDENCIA_QA_2026"));
 }
}
