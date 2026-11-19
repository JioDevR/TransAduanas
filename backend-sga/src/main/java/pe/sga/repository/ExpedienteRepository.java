package pe.sga.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.sga.entity.Expediente;
import pe.sga.entity.enums.CanalSunat;
import pe.sga.entity.enums.EstadoExpediente;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ExpedienteRepository extends JpaRepository<Expediente, Integer> {

    @Override
    @EntityGraph(attributePaths = {"cotizacion", "cotizacion.mercancia", "cotizacion.tipoCarga", "cotizacion.puertoEnvio", "cotizacion.puertoDestino", "cotizacion.usuarioComercial", "cliente", "agenteAduanero", "mercancia", "tipoCarga"})
    List<Expediente> findAll();

    Optional<Expediente> findByCodigoExpediente(String codigoExpediente);

    Optional<Expediente> findByCotizacion_IdCotizacion(Integer idCotizacion);

    List<Expediente> findByCotizacion_IdCotizacionIn(Collection<Integer> idsCotizacion);

    Optional<Expediente> findByNumeroDam(String numeroDam);

    boolean existsByCodigoExpediente(String codigoExpediente);

    boolean existsByCotizacion_IdCotizacion(Integer idCotizacion);

    boolean existsByNumeroDam(String numeroDam);

    @EntityGraph(attributePaths = {"cotizacion", "cotizacion.mercancia", "cotizacion.tipoCarga", "cotizacion.puertoEnvio", "cotizacion.puertoDestino", "cotizacion.usuarioComercial", "cliente", "agenteAduanero", "mercancia", "tipoCarga"})
    List<Expediente> findByEstadoExpediente(EstadoExpediente estado);

    @EntityGraph(attributePaths = {"cotizacion", "cotizacion.mercancia", "cotizacion.tipoCarga", "cotizacion.puertoEnvio", "cotizacion.puertoDestino", "cotizacion.usuarioComercial", "cliente", "agenteAduanero", "mercancia", "tipoCarga"})
    List<Expediente> findByEstadoExpedienteNot(EstadoExpediente estado);

    @EntityGraph(attributePaths = {"cotizacion", "cotizacion.mercancia", "cotizacion.tipoCarga", "cotizacion.puertoEnvio", "cotizacion.puertoDestino", "cotizacion.usuarioComercial", "cliente", "agenteAduanero", "mercancia", "tipoCarga"})
    List<Expediente> findByCanalSunat(CanalSunat canalSunat);

    @EntityGraph(attributePaths = {"cotizacion", "cotizacion.mercancia", "cotizacion.tipoCarga", "cotizacion.puertoEnvio", "cotizacion.puertoDestino", "cotizacion.usuarioComercial", "cliente", "agenteAduanero", "mercancia", "tipoCarga"})
    List<Expediente> findByAgenteAduanero_IdUsuario(Integer idUsuario);

    @EntityGraph(attributePaths = {"cotizacion", "cotizacion.mercancia", "cotizacion.tipoCarga", "cotizacion.puertoEnvio", "cotizacion.puertoDestino", "cotizacion.usuarioComercial", "cliente", "agenteAduanero", "mercancia", "tipoCarga"})
    List<Expediente> findByCliente_IdCliente(Integer idCliente);
}
