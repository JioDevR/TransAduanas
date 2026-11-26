package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sga.entity.CatalogoBanco;

import java.util.List;
import java.util.Optional;

public interface CatalogoBancoRepository extends JpaRepository<CatalogoBanco, Integer> {
    Optional<CatalogoBanco> findByCodigoIgnoreCase(String codigo);
    Optional<CatalogoBanco> findByNombreIgnoreCase(String nombre);
    List<CatalogoBanco> findByEstadoOrderByIdBancoAsc(String estado);
    List<CatalogoBanco> findByEstadoAndAplicaOrigenTrueOrderByIdBancoAsc(String estado);
    List<CatalogoBanco> findByEstadoAndAplicaDestinoTrueOrderByIdBancoAsc(String estado);
}
