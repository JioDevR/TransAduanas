package pe.sga.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pe.sga.entity.DocumentoAdjunto;
import pe.sga.entity.enums.EntidadDocumento;

import java.util.List;
import java.util.Optional;

public interface IDocumentoAdjuntoRepository extends JpaRepository<DocumentoAdjunto, Integer> {

    List<DocumentoAdjunto> findByActivoTrueOrderByFechaCreacionDesc();

    List<DocumentoAdjunto> findByCotizacionIdCotizacionAndActivoTrueOrderByFechaCreacionDesc(Integer idCotizacion);

    List<DocumentoAdjunto> findByExpedienteIdExpedienteAndActivoTrueOrderByFechaCreacionDesc(Integer idExpediente);

    List<DocumentoAdjunto> findByClienteIdClienteAndActivoTrueOrderByFechaCreacionDesc(Integer idCliente);

    List<DocumentoAdjunto> findByEntidadTipoAndActivoTrueOrderByFechaCreacionDesc(EntidadDocumento entidadTipo);

    Optional<DocumentoAdjunto> findByIdDocumentoAndActivoTrue(Integer idDocumento);

    Optional<DocumentoAdjunto> findByNombreAlmacenadoAndActivoTrue(String nombreAlmacenado);
}
