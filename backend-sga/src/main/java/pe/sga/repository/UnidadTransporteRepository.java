package pe.sga.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.sga.entity.UnidadTransporte;
import pe.sga.entity.enums.EstadoOperativoFlota;

import java.util.List;
import java.util.Optional;

public interface UnidadTransporteRepository extends JpaRepository<UnidadTransporte, Integer> {

    Optional<UnidadTransporte> findByPlacaTracto(String placaTracto);

    boolean existsByPlacaTracto(String placaTracto);

    List<UnidadTransporte> findByEstadoActivo(Boolean estadoActivo);

    List<UnidadTransporte> findByEstadoOperativoAndEstadoActivo(EstadoOperativoFlota estadoOperativo, Boolean estadoActivo);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM UnidadTransporte u WHERE u.idUnidad = :id")
    Optional<UnidadTransporte> findByIdForUpdate(@Param("id") Integer id);
}
