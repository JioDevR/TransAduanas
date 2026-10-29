package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sga.entity.LiquidacionFinanciera;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LiquidacionFinancieraRepository extends JpaRepository<LiquidacionFinanciera, Integer> {

    Optional<LiquidacionFinanciera> findByCodigoLiquidacion(String codigoLiquidacion);

    Optional<LiquidacionFinanciera> findByExpediente_IdExpediente(Integer idExpediente);

    boolean existsByCodigoLiquidacion(String codigoLiquidacion);

    boolean existsByExpediente_IdExpediente(Integer idExpediente);

    List<LiquidacionFinanciera> findByEstadoPago(String estadoPago);

    List<LiquidacionFinanciera> findByEstadoPagoIn(Collection<String> estadosPago);

    List<LiquidacionFinanciera> findByEstadoPagoNot(String estadoPago);

    List<LiquidacionFinanciera> findByExpediente_Cliente_IdCliente(Integer idCliente);
}
