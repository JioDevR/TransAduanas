package pe.sga.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.sga.entity.AuditoriaEvento;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditoriaEventoRepository extends JpaRepository<AuditoriaEvento, Long> {

    @Query("""
            SELECT a FROM AuditoriaEvento a
            WHERE (:buscar IS NULL OR :buscar = '' OR
                   LOWER(a.usuarioActor) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                   LOWER(a.accion) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                   LOWER(COALESCE(a.detalle, '')) LIKE LOWER(CONCAT('%', :buscar, '%')) OR
                   LOWER(COALESCE(a.entidad, '')) LIKE LOWER(CONCAT('%', :buscar, '%')))
              AND (:modulo IS NULL OR :modulo = '' OR UPPER(a.modulo) = UPPER(:modulo))
              AND (:resultado IS NULL OR :resultado = '' OR UPPER(a.resultado) = UPPER(:resultado))
              AND (:desde IS NULL OR a.fecha >= :desde)
              AND (:hasta IS NULL OR a.fecha <= :hasta)
            ORDER BY a.fecha DESC, a.idEvento DESC
            """)
    Page<AuditoriaEvento> buscarConFiltros(
            @Param("buscar") String buscar,
            @Param("modulo") String modulo,
            @Param("resultado") String resultado,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
    );

    List<AuditoriaEvento> findByFechaGreaterThanEqualOrderByFechaDescIdEventoDesc(LocalDateTime desde);

    long countByFechaGreaterThanEqualAndResultado(LocalDateTime desde, String resultado);

    long countByFechaGreaterThanEqualAndAccionAndResultado(LocalDateTime desde, String accion, String resultado);
}
