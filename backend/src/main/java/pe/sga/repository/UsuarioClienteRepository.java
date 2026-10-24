package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sga.entity.UsuarioCliente;

import java.util.List;
import java.util.Optional;

public interface UsuarioClienteRepository extends JpaRepository<UsuarioCliente, Integer> {

    Optional<UsuarioCliente> findByUsername(String username);

    Optional<UsuarioCliente> findByCorreoElectronico(String correoElectronico);

    List<UsuarioCliente> findByCliente_NumeroDocumento(String ruc);

    List<UsuarioCliente> findByCliente_IdCliente(Integer idCliente);

    boolean existsByUsername(String username);

    boolean existsByCorreoElectronico(String correoElectronico);
}
