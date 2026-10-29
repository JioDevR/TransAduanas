package pe.sga.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.sga.entity.Cotizacion;
import pe.sga.entity.enums.EstadoCotizacion;

import java.util.List;
import java.util.Optional;

public interface CotizacionRepository extends JpaRepository<Cotizacion, Integer> {

    @Override
    @EntityGraph(attributePaths = {"cliente", "usuarioComercial", "mercancia", "tipoCarga", "puertoEnvio", "puertoDestino", "agenteRevisor"})
    List<Cotizacion> findAll();

    Optional<Cotizacion> findByCodigoCotizacion(String codigoCotizacion);

    boolean existsByCodigoCotizacion(String codigoCotizacion);

    @EntityGraph(attributePaths = {"cliente", "usuarioComercial", "mercancia", "tipoCarga", "puertoEnvio", "puertoDestino", "agenteRevisor"})
    List<Cotizacion> findByEstado(EstadoCotizacion estado);

    @EntityGraph(attributePaths = {"cliente", "usuarioComercial", "mercancia", "tipoCarga", "puertoEnvio", "puertoDestino", "agenteRevisor"})
    List<Cotizacion> findByEstadoNot(EstadoCotizacion estado);

    @EntityGraph(attributePaths = {"cliente", "usuarioComercial", "mercancia", "tipoCarga", "puertoEnvio", "puertoDestino", "agenteRevisor"})
    List<Cotizacion> findByCliente_IdCliente(Integer idCliente);

    List<Cotizacion> findByUsuarioComercial_IdUsuario(Integer idUsuario);

    List<Cotizacion> findByAgenteRevisor_IdUsuario(Integer idUsuario);
}
