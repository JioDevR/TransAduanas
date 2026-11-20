package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.sga.entity.UsuarioCliente;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface UsuarioClienteRepository extends JpaRepository<UsuarioCliente, Integer> {

    Optional<UsuarioCliente> findByUsername(String username);

    Optional<UsuarioCliente> findByCliente_IdCliente(Integer idCliente);

    @Query("SELECT DISTINCT uc.cliente.idCliente FROM UsuarioCliente uc WHERE uc.cliente.idCliente IN :idsClientes")
    Set<Integer> findClienteIdsWithUsuario(@Param("idsClientes") Collection<Integer> idsClientes);

    List<UsuarioCliente> findByCliente_NumeroDocumento(String ruc);

    Optional<UsuarioCliente> findByCliente_CorreoContacto(String correoContacto);

    default Optional<UsuarioCliente> findByCorreoElectronico(String correoElectronico) {
        return findByCliente_CorreoContacto(correoElectronico);
    }

    boolean existsByUsername(String username);

    @Query("SELECT uc FROM UsuarioCliente uc JOIN FETCH uc.cliente c ORDER BY uc.idUsuarioCliente DESC")
    List<UsuarioCliente> findAllWithCliente();

    @Query("SELECT uc FROM UsuarioCliente uc JOIN FETCH uc.cliente c WHERE uc.estadoActivo = true ORDER BY uc.idUsuarioCliente DESC")
    List<UsuarioCliente> findByEstadoActivoTrueWithCliente();

    @Query("SELECT uc FROM UsuarioCliente uc JOIN FETCH uc.cliente c WHERE uc.idUsuarioCliente = :id")
    Optional<UsuarioCliente> findByIdWithCliente(@Param("id") Integer id);
}
