package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.sga.entity.ComprobanteFactura;

import java.util.List;
import java.util.Optional;

public interface ComprobanteFacturaRepository extends JpaRepository<ComprobanteFactura, Integer> {

    List<ComprobanteFactura> findAllByOrderByFechaEmisionDesc();

    List<ComprobanteFactura> findByCliente_IdClienteOrderByFechaEmisionDesc(Integer idCliente);

    Optional<ComprobanteFactura> findFirstByPagoCotizacion_IdPagoAndEstadoOrderByIdComprobanteDesc(
            Integer idPago, String estado);

    Optional<ComprobanteFactura> findFirstByLiquidacion_IdLiquidacionAndEstadoOrderByIdComprobanteDesc(
            Integer idLiquidacion, String estado);

    boolean existsByNumeroCompleto(String numeroCompleto);

    @Query("SELECT COALESCE(MAX(c.correlativo), 0) FROM ComprobanteFactura c WHERE c.serie = :serie")
    Integer findMaxCorrelativoBySerie(@Param("serie") String serie);
}
