package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.sga.entity.CatalogoPuerto;

import java.util.List;
import java.util.Optional;

@Repository
public interface CatalogoPuertoRepository extends JpaRepository<CatalogoPuerto, Integer> {

    Optional<CatalogoPuerto> findByCodigoPuerto(String codigoPuerto);

    boolean existsByCodigoPuerto(String codigoPuerto);

    List<CatalogoPuerto> findByEstado(String estado);
}
