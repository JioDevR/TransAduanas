package pe.sga.functional;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.beans.factory.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import pe.sga.repository.*;
import pe.sga.entity.*;
import pe.sga.entity.enums.*;
import pe.sga.dto.request.ClienteRequestDTO;
import java.net.URI;
import java.net.http.*;
import java.util.*;
import com.fasterxml.jackson.databind.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={
 "spring.config.import=", "spring.datasource.url=jdbc:h2:mem:qa_multinivel;MODE=MySQL;DB_CLOSE_DELAY=-1", 
 "spring.datasource.driver-class-name=org.h2.Driver", "spring.datasource.username=sa", "spring.datasource.password=",
 "spring.jpa.hibernate.ddl-auto=create-drop", "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
 "spring.jpa.show-sql=false", "spring.jpa.properties.hibernate.format_sql=false",
 "app.storage.upload-dir=./target/qa-files", "jwt.secret=QA_ONLY_2026_TEST_KEY_012345678901234567890123456789", "jwt.expiration-ms=3600000",
 "server.address=127.0.0.1", "logging.level.org.hibernate.SQL=OFF"})
abstract class FullStackTestBase {
 @Value("${local.server.port}") int port;
 @Autowired ClienteRepository clientes;
 @Autowired UsuarioRepository usuarios;
 @Autowired UsuarioClienteRepository usuariosPortal;
 @Autowired PasswordEncoder encoder;
 final ObjectMapper json=new ObjectMapper().findAndRegisterModules();
 final HttpClient http=HttpClient.newBuilder().connectTimeout(java.time.Duration.ofSeconds(10)).build();
 String unique(){return UUID.randomUUID().toString().replace("-","").substring(0,11);}
 ClienteRequestDTO dto(String doc){return ClienteRequestDTO.builder().numeroDocumento(doc).razonSocial("QA Empresa "+doc).correoContacto("qa@example.test").estadoCliente("PROSPECTO").estadoActivo(true).build();}
 Cliente empresa(){return clientes.saveAndFlush(Cliente.builder().numeroDocumento(unique()).razonSocial("QA Empresa").correoContacto("qa@example.test").estadoCliente("PROSPECTO").estadoActivo(true).build());}
 Usuario user(RolUsuario role){String n="qa"+unique();return usuarios.saveAndFlush(Usuario.builder().username(n).passwordHash(encoder.encode("QA_Test123!" )).nombres("QA").apellidos("Pruebas").correoInstitucional(n+"@example.test").rol(role).estadoActivo(true).build());}
 String login(Usuario u)throws Exception {var r=call("POST","/api/v1/auth/login",null,Map.of("username",u.getUsername(),"password","QA_Test123!"));assertEquals(200,r.statusCode(),r.body());return tree(r).path("data").path("token").asText();}
 HttpResponse<String> call(String method,String path,String token,Object body)throws Exception {
  var b=HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+port+path)).timeout(java.time.Duration.ofSeconds(20));
  if(token!=null)b.header("Authorization","Bearer "+token);
  if(body!=null)b.header("Content-Type","application/json");
  b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body instanceof String?(String)body:json.writeValueAsString(body)));
  return http.send(b.build(),HttpResponse.BodyHandlers.ofString());
 }
 JsonNode tree(HttpResponse<String> r)throws Exception{return json.readTree(r.body());}
 int create(String token,String doc)throws Exception{var r=call("POST","/api/v1/clientes",token,dto(doc));assertEquals(201,r.statusCode(),r.body());return tree(r).path("data").path("idCliente").asInt();}
}
