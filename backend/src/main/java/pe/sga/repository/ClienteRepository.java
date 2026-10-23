package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sga.entity.Cliente;

import java.util.List;
import java.util.Optional;

public interface ClienteRepository extends JpaRepository<Cliente, Integer> {

    Optional<Cliente> findByNumeroDocumento(String numeroDocumento);

    boolean existsByNumeroDocumento(String numeroDocumento);

    List<Cliente> findByEstadoActivo(Boolean estadoActivo);

    List<Cliente> findByEsProspecto(Boolean esProspecto);

    List<Cliente> findByEsProspectoAndEstadoActivo(Boolean esProspecto, Boolean estadoActivo);

    List<Cliente> findByRazonSocialContainingIgnoreCase(String razonSocial);

    List<Cliente> findByRazonSocialContainingIgnoreCaseAndEstadoActivo(String razonSocial, Boolean estadoActivo);
}
