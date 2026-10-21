package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.sga.entity.ControlAlmacen;

import java.util.List;
import java.util.Optional;

@Repository
public interface ControlAlmacenRepository extends JpaRepository<ControlAlmacen, Integer> {

    Optional<ControlAlmacen> findByExpediente_IdExpediente(Integer idExpediente);

    boolean existsByExpediente_IdExpediente(Integer idExpediente);

    List<ControlAlmacen> findByEstadoAlmacen(String estadoAlmacen);

    List<ControlAlmacen> findByEstadoAlmacenNot(String estadoAlmacen);

    List<ControlAlmacen> findByDepositoTemporalIgnoreCase(String depositoTemporal);
}
