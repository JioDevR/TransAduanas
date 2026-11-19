package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.sga.entity.NotificacionCliente;

import java.util.List;
import java.util.Optional;

public interface NotificacionClienteRepository extends JpaRepository<NotificacionCliente, Integer> {

    List<NotificacionCliente> findByCliente_IdClienteOrderByFechaCreacionDesc(Integer idCliente);

    long countByCliente_IdClienteAndLeidoFalse(Integer idCliente);

    Optional<NotificacionCliente> findByCliente_IdClienteAndIdNotificacion(Integer idCliente, Integer idNotificacion);

    @Modifying
    @Query("UPDATE NotificacionCliente n SET n.leido = true WHERE n.cliente.idCliente = :idCliente AND n.leido = false")
    int marcarTodasLeidasPorCliente(@Param("idCliente") Integer idCliente);
}
