package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.sga.entity.CatalogoMercancia;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogoMercanciaRepository extends JpaRepository<CatalogoMercancia, Integer> {

    Optional<CatalogoMercancia> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<CatalogoMercancia> findByEstado(String estado);

    List<CatalogoMercancia> findByCategoriaIgnoreCase(String categoria);

    List<CatalogoMercancia> findByRequierePermiso(Boolean requierePermiso);
}
