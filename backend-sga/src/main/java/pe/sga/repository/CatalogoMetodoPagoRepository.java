package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sga.entity.CatalogoMetodoPago;

import java.util.List;
import java.util.Optional;

public interface CatalogoMetodoPagoRepository extends JpaRepository<CatalogoMetodoPago, Integer> {
    Optional<CatalogoMetodoPago> findByCodigoIgnoreCase(String codigo);
    List<CatalogoMetodoPago> findByEstadoOrderByIdMetodoPagoAsc(String estado);
    List<CatalogoMetodoPago> findByEstadoAndAplicaCotizacionTrueOrderByIdMetodoPagoAsc(String estado);
    List<CatalogoMetodoPago> findByEstadoAndAplicaLiquidacionTrueOrderByIdMetodoPagoAsc(String estado);
    List<CatalogoMetodoPago> findByEstadoAndAplicaPortalB2bTrueOrderByIdMetodoPagoAsc(String estado);
}
