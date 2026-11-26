package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sga.entity.CatalogoCategoriaMercancia;

import java.util.List;
import java.util.Optional;

public interface CatalogoCategoriaMercanciaRepository extends JpaRepository<CatalogoCategoriaMercancia, Integer> {
    Optional<CatalogoCategoriaMercancia> findByCodigoIgnoreCase(String codigo);
    Optional<CatalogoCategoriaMercancia> findByNombreIgnoreCase(String nombre);
    List<CatalogoCategoriaMercancia> findByEstadoOrderByNombreAsc(String estado);
}
