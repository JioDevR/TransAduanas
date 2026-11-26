package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sga.entity.CatalogoCategoriaDocumento;

import java.util.List;
import java.util.Optional;

public interface CatalogoCategoriaDocumentoRepository extends JpaRepository<CatalogoCategoriaDocumento, Integer> {
    Optional<CatalogoCategoriaDocumento> findByCodigoIgnoreCase(String codigo);
    List<CatalogoCategoriaDocumento> findByEstadoOrderByIdCategoriaDocumentoAsc(String estado);
}
