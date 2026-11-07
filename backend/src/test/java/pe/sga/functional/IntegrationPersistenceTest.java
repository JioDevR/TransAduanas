package pe.sga.functional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.sga.service.IClienteService;
import pe.sga.exception.*;
import static org.junit.jupiter.api.Assertions.*;
class IntegrationPersistenceTest extends FullStackTestBase {
 @Autowired IClienteService service;
 @Autowired PlatformTransactionManager txManager;
 @Test void IN01_crearPersisteDTO(){var d=dto(unique());var r=service.crear(d);var stored=clientes.findById(r.getIdCliente()).orElseThrow();assertEquals(d.getNumeroDocumento(),stored.getNumeroDocumento());assertEquals(d.getCorreoContacto(),stored.getCorreoContacto());}
 @Test void IN02_actualizarPersiste(){var r=service.crear(dto(unique()));var d=dto(r.getNumeroDocumento());d.setRazonSocial("QA Actualizada");service.actualizar(r.getIdCliente(),d);assertEquals("QA Actualizada",clientes.findById(r.getIdCliente()).orElseThrow().getRazonSocial());}
 @Test void IN03_documentoDuplicadoNoInserta(){var d=dto(unique());service.crear(d);long before=clientes.count();assertThrows(BadRequestException.class,()->service.crear(d));assertEquals(before,clientes.count());}
 @Test void IN04_bajaLogicaConservaFila(){var r=service.crear(dto(unique()));service.eliminar(r.getIdCliente());assertFalse(clientes.findById(r.getIdCliente()).orElseThrow().getEstadoActivo());assertTrue(service.listarActivos().stream().noneMatch(x->x.getIdCliente().equals(r.getIdCliente())));}
 @Test void IN05_reactivarRecuperaListado(){var r=service.crear(dto(unique()));service.eliminar(r.getIdCliente());service.reactivar(r.getIdCliente());assertTrue(clientes.findById(r.getIdCliente()).orElseThrow().getEstadoActivo());assertTrue(service.listarActivos().stream().anyMatch(x->x.getIdCliente().equals(r.getIdCliente())));}
 @Test void IN06_formalizarActualizaEstado(){var r=service.crear(dto(unique()));service.convertirEnCliente(r.getIdCliente());assertEquals("CLIENTE",clientes.findById(r.getIdCliente()).orElseThrow().getEstadoCliente());assertFalse(service.buscarPorId(r.getIdCliente()).getEsProspecto());}
 @Test void IN07_rollbackNoPersiste(){String doc=unique();var tx=new TransactionTemplate(txManager);tx.executeWithoutResult(status->{service.crear(dto(doc));assertTrue(clientes.existsByNumeroDocumento(doc));status.setRollbackOnly();});assertFalse(clientes.existsByNumeroDocumento(doc));}
 @Test void IN08_filtroConsultaSQL(){var r=service.crear(dto(unique()));String text="QA Busqueda "+unique();var d=dto(r.getNumeroDocumento());d.setRazonSocial(text);service.actualizar(r.getIdCliente(),d);var found=service.buscarPorTermino(text.toLowerCase(),true);assertEquals(1,found.size());assertEquals(r.getIdCliente(),found.getFirst().getIdCliente());}
}
