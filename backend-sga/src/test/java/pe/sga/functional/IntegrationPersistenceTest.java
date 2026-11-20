package pe.sga.functional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import pe.sga.service.IClienteService;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Pruebas de Integración y Persistencia JPA")
class IntegrationPersistenceTest extends FullStackTestBase {

    @Autowired
    private IClienteService service;

    @Autowired
    private PlatformTransactionManager txManager;

    @Test
    @DisplayName("Persistencia: rollback transaccional revierte inserción en base de datos")
    void rollbackTransaccional_noPersisteRegistro() {
        String doc = unique();
        TransactionTemplate tx = new TransactionTemplate(txManager);

        tx.executeWithoutResult(status -> {
            service.crear(dto(doc));
            assertTrue(clientes.existsByNumeroDocumento(doc));
            status.setRollbackOnly();
        });

        assertFalse(clientes.existsByNumeroDocumento(doc));
    }

    @Test
    @DisplayName("Persistencia: filtro SQL case-insensitive por término recupera cliente persistido")
    void filtroConsultaSQL_busquedaCaseInsensitiveRecuperaEntidad() {
        var creado = service.crear(dto(unique()));
        String razonUnica = "QA Busqueda " + unique();

        var updateDto = dto(creado.getNumeroDocumento());
        updateDto.setRazonSocial(razonUnica);
        service.actualizar(creado.getIdCliente(), updateDto);

        var encontrados = service.buscarPorTermino(razonUnica.toLowerCase(), true);
        assertEquals(1, encontrados.size());
        assertEquals(creado.getIdCliente(), encontrados.getFirst().getIdCliente());
    }
}
