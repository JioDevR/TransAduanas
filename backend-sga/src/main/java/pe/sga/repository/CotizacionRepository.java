package pe.sga.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.sga.entity.Cotizacion;
import pe.sga.entity.Usuario;
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

    @EntityGraph(attributePaths = {"cliente", "usuarioComercial", "mercancia", "tipoCarga", "puertoEnvio", "puertoDestino", "agenteRevisor"})
    List<Cotizacion> findByUsuarioComercial_IdUsuario(Integer idUsuario);

    @EntityGraph(attributePaths = {"cliente", "usuarioComercial", "mercancia", "tipoCarga", "puertoEnvio", "puertoDestino", "agenteRevisor"})
    List<Cotizacion> findByUsuarioComercialIsNullAndEstadoNot(EstadoCotizacion estado);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Cotizacion c SET c.usuarioComercial = :usuario WHERE c.idCotizacion = :idCotizacion AND c.usuarioComercial IS NULL AND c.estado <> pe.sga.entity.enums.EstadoCotizacion.ANULADA")
    int asignarSiSinAsignar(@Param("idCotizacion") Integer idCotizacion, @Param("usuario") Usuario usuario);
}
