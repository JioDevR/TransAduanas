package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sga.entity.CatalogoProvincia;

import java.util.List;
import java.util.Optional;

public interface CatalogoProvinciaRepository extends JpaRepository<CatalogoProvincia, Integer> {
    List<CatalogoProvincia> findByEstadoOrderByIdProvinciaAsc(String estado);
    Optional<CatalogoProvincia> findByDepartamento_NombreIgnoreCaseAndNombreIgnoreCase(String departamento, String provincia);
}
