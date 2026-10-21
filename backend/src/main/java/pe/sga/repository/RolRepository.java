package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.sga.entity.Rol;
import pe.sga.entity.enums.RolUsuario;

import java.util.List;
import java.util.Optional;

@Repository
public interface RolRepository extends JpaRepository<Rol, Integer> {

    Optional<Rol> findByNombreRol(RolUsuario nombreRol);

    boolean existsByNombreRol(RolUsuario nombreRol);

    List<Rol> findByEstadoActivoTrue();
}
