package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sga.entity.CatalogoCuentaRecaudadora;

import java.util.List;
import java.util.Optional;

public interface CatalogoCuentaRecaudadoraRepository extends JpaRepository<CatalogoCuentaRecaudadora, Integer> {
    Optional<CatalogoCuentaRecaudadora> findByCodigoIgnoreCase(String codigo);
    Optional<CatalogoCuentaRecaudadora> findByNombreIgnoreCase(String nombre);
    List<CatalogoCuentaRecaudadora> findByEstadoOrderByIdCuentaAsc(String estado);
    List<CatalogoCuentaRecaudadora> findByEstadoAndAplicaPortalB2bTrueOrderByIdCuentaAsc(String estado);
    List<CatalogoCuentaRecaudadora> findByEstadoAndAplicaSgaTrueOrderByIdCuentaAsc(String estado);
}
