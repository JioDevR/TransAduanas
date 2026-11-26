package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sga.entity.CatalogoDepartamento;

import java.util.List;
import java.util.Optional;

public interface CatalogoDepartamentoRepository extends JpaRepository<CatalogoDepartamento, Integer> {
    Optional<CatalogoDepartamento> findByNombreIgnoreCase(String nombre);
    List<CatalogoDepartamento> findByEstadoOrderByNombreAsc(String estado);
}
