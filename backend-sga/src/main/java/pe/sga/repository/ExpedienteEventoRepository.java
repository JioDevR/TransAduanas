package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.sga.entity.ExpedienteEvento;

import java.util.List;

@Repository
public interface ExpedienteEventoRepository extends JpaRepository<ExpedienteEvento, Integer> {

    List<ExpedienteEvento> findByExpediente_IdExpedienteOrderByFechaEventoAscIdEventoAsc(Integer idExpediente);

    List<ExpedienteEvento> findByExpediente_IdExpedienteInOrderByFechaEventoAscIdEventoAsc(List<Integer> idsExpediente);
}
