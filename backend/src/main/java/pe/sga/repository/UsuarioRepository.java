package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.sga.entity.Usuario;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByCorreoInstitucional(String correoInstitucional);

    boolean existsByUsername(String username);

    boolean existsByCorreoInstitucional(String correoInstitucional);

    List<Usuario> findByEstadoActivoTrue();

    List<Usuario> findByRol_IdRol(Integer idRol);

    List<Usuario> findBySede_IdSede(Integer idSede);
}
