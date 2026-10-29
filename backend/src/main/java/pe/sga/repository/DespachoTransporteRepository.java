package pe.sga.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import pe.sga.entity.DespachoTransporte;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DespachoTransporteRepository extends JpaRepository<DespachoTransporte, Integer> {

    Optional<DespachoTransporte> findByCodigoTracking(String codigoTracking);

    Optional<DespachoTransporte> findByExpediente_IdExpediente(Integer idExpediente);

    @EntityGraph(attributePaths = {"conductor", "unidad"})
    List<DespachoTransporte> findByExpediente_IdExpedienteIn(Collection<Integer> idsExpediente);

    boolean existsByCodigoTracking(String codigoTracking);

    boolean existsByExpediente_IdExpediente(Integer idExpediente);

    List<DespachoTransporte> findByEstadoTransporte(String estadoTransporte);

    List<DespachoTransporte> findByEstadoTransporteNot(String estadoTransporte);

    List<DespachoTransporte> findByConductor_IdConductor(Integer idConductor);

    List<DespachoTransporte> findByUnidad_IdUnidad(Integer idUnidad);

    Optional<DespachoTransporte> findFirstByConductor_IdConductorAndEstadoTransporteInOrderByFechaProgramadaAsc(
            Integer idConductor, List<String> estados);

    Optional<DespachoTransporte> findFirstByConductor_Usuario_IdUsuarioAndEstadoTransporteInOrderByFechaProgramadaAsc(
            Integer idUsuario, List<String> estados);

    List<DespachoTransporte> findByConductor_Usuario_IdUsuarioOrderByFechaProgramadaDesc(Integer idUsuario);
}
