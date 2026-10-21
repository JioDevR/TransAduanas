package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.sga.entity.Conductor;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConductorRepository extends JpaRepository<Conductor, Integer> {

    Optional<Conductor> findByNumeroLicencia(String numeroLicencia);

    Optional<Conductor> findByUsuario_IdUsuario(Integer idUsuario);

    boolean existsByNumeroLicencia(String numeroLicencia);

    boolean existsByUsuario_IdUsuario(Integer idUsuario);

    List<Conductor> findByEstadoActivo(Boolean estadoActivo);

    List<Conductor> findByEstadoOperativo(String estadoOperativo);
}
