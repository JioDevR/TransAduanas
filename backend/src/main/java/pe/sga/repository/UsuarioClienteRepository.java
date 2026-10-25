package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.sga.entity.UsuarioCliente;

import java.util.List;
import java.util.Optional;

public interface UsuarioClienteRepository extends JpaRepository<UsuarioCliente, Integer> {

    Optional<UsuarioCliente> findByUsername(String username);

    Optional<UsuarioCliente> findByCliente_IdCliente(Integer idCliente);

    List<UsuarioCliente> findByCliente_NumeroDocumento(String ruc);

    Optional<UsuarioCliente> findByCliente_CorreoContacto(String correoContacto);

    default Optional<UsuarioCliente> findByCorreoElectronico(String correoElectronico) {
        return findByCliente_CorreoContacto(correoElectronico);
    }

    boolean existsByUsername(String username);

    default boolean existsByCorreoElectronico(String correoElectronico) {
        return findByCliente_CorreoContacto(correoElectronico).isPresent();
    }

    @Query("SELECT uc FROM UsuarioCliente uc JOIN FETCH uc.cliente c ORDER BY uc.idUsuarioCliente DESC")
    List<UsuarioCliente> findAllWithCliente();

    @Query("SELECT uc FROM UsuarioCliente uc JOIN FETCH uc.cliente c WHERE uc.estadoActivo = true ORDER BY uc.idUsuarioCliente DESC")
    List<UsuarioCliente> findByEstadoActivoTrueWithCliente();

    @Query("SELECT uc FROM UsuarioCliente uc JOIN FETCH uc.cliente c WHERE uc.idUsuarioCliente = :id")
    Optional<UsuarioCliente> findByIdWithCliente(@Param("id") Integer id);

    @Query("SELECT uc FROM UsuarioCliente uc JOIN FETCH uc.cliente c " +
           "WHERE (:idCliente IS NULL OR c.idCliente = :idCliente) " +
           "AND (:soloActivos = false OR uc.estadoActivo = true) " +
           "AND (:termino IS NULL OR :termino = '' OR (" +
           "   LOWER(uc.username) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "   LOWER(c.contactoRepresentante) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "   LOWER(c.correoContacto) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "   LOWER(c.razonSocial) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "   c.numeroDocumento LIKE CONCAT('%', :termino, '%') " +
           ")) " +
           "ORDER BY uc.idUsuarioCliente DESC")
    List<UsuarioCliente> buscarConFiltros(
            @Param("idCliente") Integer idCliente,
            @Param("soloActivos") boolean soloActivos,
            @Param("termino") String termino);
}
