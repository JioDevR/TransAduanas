package pe.sga.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.DocumentoResponseDTO;
import pe.sga.entity.DocumentoAdjunto;
import pe.sga.entity.Usuario;
import pe.sga.entity.enums.CategoriaDocumento;
import pe.sga.entity.enums.EntidadDocumento;
import pe.sga.service.IDocumentoService;

import org.springframework.security.access.prepost.PreAuthorize;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1/documentos")
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class DocumentoController {

    private final IDocumentoService documentoService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponseDTO<List<DocumentoResponseDTO>>> subir(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "categoria", required = false) CategoriaDocumento categoria,
            @RequestParam(value = "descripcion", required = false) String descripcion,
            @RequestParam(value = "entidadTipo", required = false, defaultValue = "GENERAL") EntidadDocumento entidadTipo,
            @RequestParam(value = "idEntidad", required = false) Integer idEntidad,
            @AuthenticationPrincipal Usuario usuarioAutenticado
    ) {
        Integer idUsuario = (usuarioAutenticado != null && usuarioAutenticado.getIdUsuario() != null)
                ? usuarioAutenticado.getIdUsuario()
                : 1;

        List<DocumentoResponseDTO> guardados = documentoService.subirDocumentos(
                files, categoria, descripcion, entidadTipo, idEntidad, idUsuario
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Documentos almacenados exitosamente", guardados));
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<DocumentoResponseDTO>>> listar(
            @RequestParam(required = false) EntidadDocumento entidadTipo,
            @RequestParam(required = false) Integer idCotizacion,
            @RequestParam(required = false) Integer idExpediente,
            @RequestParam(required = false) Integer idCliente,
            @RequestParam(required = false) CategoriaDocumento categoria
    ) {
        List<DocumentoResponseDTO> lista = documentoService.listar(
                entidadTipo, idCotizacion, idExpediente, idCliente, categoria
        );
        return ResponseEntity.ok(ApiResponseDTO.ok("Listado de documentos recuperado", lista));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<DocumentoResponseDTO>> buscarPorId(@PathVariable Integer id) {
        DocumentoResponseDTO dto = documentoService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @GetMapping("/{id}/descargar")
    public ResponseEntity<Resource> descargar(@PathVariable Integer id) {
        DocumentoAdjunto entidad = documentoService.obtenerEntidad(id);
        Resource resource = documentoService.descargarArchivo(id);

        String encodedFilename = URLEncoder.encode(entidad.getNombreOriginal(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(entidad.getFormatoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + entidad.getNombreOriginal() + "\"; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> previsualizar(@PathVariable Integer id) {
        DocumentoAdjunto entidad = documentoService.obtenerEntidad(id);
        Resource resource = documentoService.descargarArchivo(id);

        String encodedFilename = URLEncoder.encode(entidad.getNombreOriginal(), StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(entidad.getFormatoMime()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + entidad.getNombreOriginal() + "\"; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<Void>> eliminar(@PathVariable Integer id) {
        documentoService.eliminar(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Documento eliminado correctamente", null));
    }
}
