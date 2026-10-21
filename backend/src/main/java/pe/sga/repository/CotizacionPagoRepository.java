package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.sga.entity.CotizacionPago;
import pe.sga.entity.enums.EstadoPagoCotizacion;

import java.util.List;
import java.util.Optional;

public interface CotizacionPagoRepository extends JpaRepository<CotizacionPago, Integer> {

    List<CotizacionPago> findByCotizacion_IdCotizacionOrderByFechaRegistroDesc(Integer idCotizacion);

    List<CotizacionPago> findByEstadoPagoOrderByFechaRegistroDesc(EstadoPagoCotizacion estadoPago);

    boolean existsByCotizacion_IdCotizacionAndEstadoPago(Integer idCotizacion, EstadoPagoCotizacion estadoPago);

    long countByEstadoPago(EstadoPagoCotizacion estadoPago);

    @Query("SELECT p FROM CotizacionPago p WHERE p.codigoPago LIKE :prefix% ORDER BY p.idPago DESC LIMIT 1")
    Optional<CotizacionPago> findUltimoPagoPorPrefijo(@Param("prefix") String prefix);
}
