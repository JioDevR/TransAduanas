package pe.sga.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import pe.sga.dto.response.DocumentoResponseDTO;
import pe.sga.entity.DocumentoAdjunto;
import pe.sga.entity.enums.CategoriaDocumento;
import pe.sga.entity.enums.EntidadDocumento;

import java.util.List;

public interface IDocumentoService {

    List<DocumentoResponseDTO> subirDocumentos(
            List<MultipartFile> files,
            CategoriaDocumento categoria,
            String descripcion,
            EntidadDocumento entidadTipo,
            Integer idEntidad,
            Integer idUsuario
    );

    List<DocumentoResponseDTO> listar(
            EntidadDocumento entidadTipo,
            Integer idCotizacion,
            Integer idExpediente,
            Integer idCliente,
            CategoriaDocumento categoria
    );

    DocumentoResponseDTO buscarPorId(Integer id);

    Resource descargarArchivo(Integer id);

    DocumentoAdjunto obtenerEntidad(Integer id);

    void eliminar(Integer id);
}
