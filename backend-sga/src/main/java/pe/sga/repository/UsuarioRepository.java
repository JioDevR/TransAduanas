package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sga.entity.Usuario;
import pe.sga.entity.enums.RolUsuario;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Integer> {

    Optional<Usuario> findByUsername(String username);

    Optional<Usuario> findByCorreoInstitucional(String correoInstitucional);

    boolean existsByUsername(String username);

    boolean existsByCorreoInstitucional(String correoInstitucional);

    List<Usuario> findByEstadoActivoTrue();

    List<Usuario> findByRol(RolUsuario rol);

    List<Usuario> findBySede_IdSede(Integer idSede);

    Optional<Usuario> findFirstByOrderByIdUsuarioAsc();

    Optional<Usuario> findFirstByRolInOrderByIdUsuarioAsc(Collection<RolUsuario> roles);
}
