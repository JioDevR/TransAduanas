package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sga.entity.CatalogoEntidadReguladora;

import java.util.List;
import java.util.Optional;

public interface CatalogoEntidadReguladoraRepository extends JpaRepository<CatalogoEntidadReguladora, Integer> {
    Optional<CatalogoEntidadReguladora> findBySiglaIgnoreCase(String sigla);
    List<CatalogoEntidadReguladora> findByEstadoOrderBySiglaAsc(String estado);
}
