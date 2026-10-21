package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pe.sga.entity.Sede;

import java.util.List;
import java.util.Optional;

@Repository
public interface SedeRepository extends JpaRepository<Sede, Integer> {

    Optional<Sede> findByCodigoSede(String codigoSede);

    boolean existsByCodigoSede(String codigoSede);

    List<Sede> findByEstado(String estado);

    default List<Sede> findByEstadoOperativo() {
        return findByEstado("OPERATIVO");
    }
}
