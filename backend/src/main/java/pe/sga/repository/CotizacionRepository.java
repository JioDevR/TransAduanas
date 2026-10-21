package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.sga.entity.Cotizacion;
import pe.sga.entity.enums.EstadoCotizacion;

import java.util.List;
import java.util.Optional;

@Repository
public interface CotizacionRepository extends JpaRepository<Cotizacion, Integer> {

    Optional<Cotizacion> findByCodigoCotizacion(String codigoCotizacion);

    boolean existsByCodigoCotizacion(String codigoCotizacion);

    List<Cotizacion> findByEstado(EstadoCotizacion estado);

    List<Cotizacion> findByEstadoNot(EstadoCotizacion estado);

    List<Cotizacion> findByCliente_IdCliente(Integer idCliente);

    List<Cotizacion> findByUsuarioComercial_IdUsuario(Integer idUsuario);

    List<Cotizacion> findByAgenteRevisor_IdUsuario(Integer idUsuario);
}
