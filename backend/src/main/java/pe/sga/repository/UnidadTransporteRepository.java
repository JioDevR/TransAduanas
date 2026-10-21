package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.sga.entity.UnidadTransporte;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnidadTransporteRepository extends JpaRepository<UnidadTransporte, Integer> {

    Optional<UnidadTransporte> findByPlacaTracto(String placaTracto);

    boolean existsByPlacaTracto(String placaTracto);

    List<UnidadTransporte> findByEstadoActivo(Boolean estadoActivo);

    List<UnidadTransporte> findByEstadoOperativoAndEstadoActivo(String estadoOperativo, Boolean estadoActivo);
}
