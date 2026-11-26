package pe.sga.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.sga.entity.Conductor;
import pe.sga.entity.enums.EstadoOperativoConductor;

import java.util.List;
import java.util.Optional;

public interface ConductorRepository extends JpaRepository<Conductor, Integer> {

    Optional<Conductor> findByNumeroLicencia(String numeroLicencia);

    boolean existsByNumeroLicencia(String numeroLicencia);

    boolean existsByNumeroDocumento(String numeroDocumento);

    boolean existsByNumeroDocumentoAndIdConductorNot(String numeroDocumento, Integer idConductor);

    List<Conductor> findByEstadoActivo(Boolean estadoActivo);

    List<Conductor> findByEstadoOperativoAndEstadoActivo(EstadoOperativoConductor estadoOperativo, Boolean estadoActivo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Conductor c WHERE c.idConductor = :id")
    Optional<Conductor> findByIdForUpdate(@Param("id") Integer id);
}
