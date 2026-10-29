package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sga.entity.Cliente;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);

    boolean existsByNumeroDocumento(String numeroDocumento);

    List<Cliente> findByEstadoActivo(Boolean estadoActivo);

    List<Cliente> findByEstadoCliente(String estadoCliente);

    List<Cliente> findByEstadoClienteAndEstadoActivo(String estadoCliente, Boolean estadoActivo);

    default List<Cliente> findByEsProspecto(Boolean esProspecto) {
        return findByEstadoCliente(Boolean.TRUE.equals(esProspecto) ? "PROSPECTO" : "CLIENTE");
    }

    default List<Cliente> findByEsProspectoAndEstadoActivo(Boolean esProspecto, Boolean estadoActivo) {
        return findByEstadoClienteAndEstadoActivo(Boolean.TRUE.equals(esProspecto) ? "PROSPECTO" : "CLIENTE", estadoActivo);
    }

    List<Cliente> findByRazonSocialContainingIgnoreCase(String razonSocial);

    List<Cliente> findByRazonSocialContainingIgnoreCaseAndEstadoActivo(String razonSocial, Boolean estadoActivo);
}
