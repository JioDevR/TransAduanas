package pe.sga.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.sga.entity.DespachoTransporte;
import pe.sga.entity.enums.EstadoTransporte;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DespachoTransporteRepository extends JpaRepository<DespachoTransporte, Integer> {

    @EntityGraph(attributePaths = {
            "expediente", "expediente.cliente", "expediente.cotizacion",
            "expediente.cotizacion.mercancia", "expediente.mercancia",
            "conductor", "unidad", "documentoRemision"
    })
    @Query("""
            SELECT d FROM DespachoTransporte d
            WHERE (:soloActivos = false OR d.estadoTransporte <> pe.sga.entity.enums.EstadoTransporte.CANCELADO)
              AND (:estado IS NULL OR d.estadoTransporte = :estado)
              AND (:idConductor IS NULL OR d.conductor.idConductor = :idConductor)
              AND (:idUnidad IS NULL OR d.unidad.idUnidad = :idUnidad)
            ORDER BY d.fechaProgramada DESC, d.idDespachoTransporte DESC
            """)
    List<DespachoTransporte> buscarConFiltros(
            @Param("soloActivos") boolean soloActivos,
            @Param("estado") EstadoTransporte estado,
            @Param("idConductor") Integer idConductor,
            @Param("idUnidad") Integer idUnidad);

    @EntityGraph(attributePaths = {
            "expediente", "expediente.cliente", "expediente.cotizacion",
            "expediente.cotizacion.mercancia", "expediente.mercancia",
            "conductor", "unidad", "documentoRemision"
    })
    @Query("SELECT d FROM DespachoTransporte d WHERE d.idDespachoTransporte = :id")
    Optional<DespachoTransporte> findByIdWithDetails(@Param("id") Integer id);

    @EntityGraph(attributePaths = {
            "expediente", "expediente.cliente", "expediente.cotizacion",
            "expediente.cotizacion.mercancia", "expediente.mercancia",
            "conductor", "unidad", "documentoRemision"
    })
    Optional<DespachoTransporte> findByCodigoTracking(String codigoTracking);

    @EntityGraph(attributePaths = {
            "expediente", "expediente.cliente", "expediente.cotizacion",
            "expediente.cotizacion.mercancia", "expediente.mercancia",
            "conductor", "unidad", "documentoRemision"
    })
    Optional<DespachoTransporte> findFirstByExpediente_IdExpedienteAndEstadoTransporteNotOrderByIdDespachoTransporteDesc(
            Integer idExpediente, EstadoTransporte estadoExcluido);

    @EntityGraph(attributePaths = {"conductor", "unidad"})
    List<DespachoTransporte> findByExpediente_IdExpedienteInAndEstadoTransporteNot(
            Collection<Integer> idsExpediente, EstadoTransporte estadoExcluido);

    boolean existsByCodigoTracking(String codigoTracking);

    boolean existsByExpediente_IdExpedienteAndEstadoTransporteNot(
            Integer idExpediente, EstadoTransporte estadoExcluido);

    boolean existsByUnidad_IdUnidadAndEstadoTransporteInAndIdDespachoTransporteNot(
            Integer idUnidad, Collection<EstadoTransporte> estados, Integer idDespachoTransporte);

    boolean existsByUnidad_IdUnidadAndEstadoTransporteIn(
            Integer idUnidad, Collection<EstadoTransporte> estados);

    boolean existsByConductor_IdConductorAndEstadoTransporteInAndIdDespachoTransporteNot(
            Integer idConductor, Collection<EstadoTransporte> estados, Integer idDespachoTransporte);

    boolean existsByConductor_IdConductorAndEstadoTransporteIn(
            Integer idConductor, Collection<EstadoTransporte> estados);

    @Query("SELECT COALESCE(MAX(d.idDespachoTransporte), 0) FROM DespachoTransporte d")
    Integer findMaxId();
}
