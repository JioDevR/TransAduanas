package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.sga.entity.CatalogoTipoCarga;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogoTipoCargaRepository extends JpaRepository<CatalogoTipoCarga, Integer> {

    Optional<CatalogoTipoCarga> findByCodigo(String codigo);

    boolean existsByCodigo(String codigo);

    List<CatalogoTipoCarga> findByEstado(String estado);
}
