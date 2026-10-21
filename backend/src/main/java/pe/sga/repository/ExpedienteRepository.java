package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.sga.entity.Expediente;
import pe.sga.entity.enums.CanalSunat;
import pe.sga.entity.enums.EstadoExpediente;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpedienteRepository extends JpaRepository<Expediente, Integer> {

    Optional<Expediente> findByCodigoExpediente(String codigoExpediente);

    Optional<Expediente> findByCotizacion_IdCotizacion(Integer idCotizacion);

    Optional<Expediente> findByNumeroDam(String numeroDam);

    boolean existsByCodigoExpediente(String codigoExpediente);

    boolean existsByCotizacion_IdCotizacion(Integer idCotizacion);

    boolean existsByNumeroDam(String numeroDam);

    List<Expediente> findByEstadoExpediente(EstadoExpediente estado);

    List<Expediente> findByEstadoExpedienteNot(EstadoExpediente estado);

    List<Expediente> findByCanalSunat(CanalSunat canalSunat);

    List<Expediente> findByAgenteAduanero_IdUsuario(Integer idUsuario);

    List<Expediente> findByCliente_IdCliente(Integer idCliente);
}
