package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sga.entity.CatalogoPuerto;

import java.util.List;
import java.util.Optional;

public interface CatalogoPuertoRepository extends JpaRepository<CatalogoPuerto, Integer> {

    Optional<CatalogoPuerto> findByCodigoPuerto(String codigoPuerto);

    boolean existsByCodigoPuerto(String codigoPuerto);

    List<CatalogoPuerto> findByEstado(String estado);
}
