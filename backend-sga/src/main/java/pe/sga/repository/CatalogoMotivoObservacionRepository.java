package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sga.entity.CatalogoMotivoObservacion;

import java.util.List;

public interface CatalogoMotivoObservacionRepository extends JpaRepository<CatalogoMotivoObservacion, Integer> {
    List<CatalogoMotivoObservacion> findByEstadoOrderByIdMotivoAsc(String estado);
    List<CatalogoMotivoObservacion> findByEstadoAndModuloIgnoreCaseOrderByIdMotivoAsc(String estado, String modulo);
}
