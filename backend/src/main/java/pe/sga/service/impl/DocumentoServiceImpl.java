package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import pe.sga.dto.response.DocumentoResponseDTO;
import pe.sga.entity.Cliente;
import pe.sga.entity.Cotizacion;
import pe.sga.entity.DocumentoAdjunto;
import pe.sga.entity.Expediente;
import pe.sga.entity.Usuario;
import pe.sga.entity.enums.CategoriaDocumento;
import pe.sga.entity.enums.EntidadDocumento;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.ClienteRepository;
import pe.sga.repository.CotizacionRepository;
import pe.sga.repository.ExpedienteRepository;
import pe.sga.repository.IDocumentoAdjuntoRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.IDocumentoService;
import pe.sga.service.IStorageService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentoServiceImpl implements IDocumentoService {

    private final IDocumentoAdjuntoRepository documentoRepository;
    private final IStorageService storageService;
    private final CotizacionRepository cotizacionRepository;
    private final ExpedienteRepository expedienteRepository;
    private final ClienteRepository clienteRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    public List<DocumentoResponseDTO> subirDocumentos(
            List<MultipartFile> files,
            CategoriaDocumento categoria,
            String descripcion,
            EntidadDocumento entidadTipo,
            Integer idEntidad,
            Integer idUsuario
    ) {
        Usuario usuario = usuarioRepository.findById(idUsuario != null ? idUsuario : 1)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + idUsuario));

        Cotizacion cotizacion = null;
        Expediente expediente = null;
        Cliente cliente = null;

        if (entidadTipo == null) {
            entidadTipo = EntidadDocumento.GENERAL;
        }

        if (idEntidad != null) {
            switch (entidadTipo) {
                case COTIZACION -> cotizacion = cotizacionRepository.findById(idEntidad)
                        .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con ID: " + idEntidad));
                case EXPEDIENTE -> expediente = expedienteRepository.findById(idEntidad)
                        .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idEntidad));
                case CLIENTE -> cliente = clienteRepository.findById(idEntidad)
                        .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + idEntidad));
                default -> {}
            }
        }

        List<DocumentoResponseDTO> resultados = new ArrayList<>();

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            String storedFilename = storageService.store(file);
            String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "documento");

            String extension = "";
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = originalFilename.substring(dotIndex + 1).toLowerCase();
            }

            String mimeType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

            DocumentoAdjunto doc = DocumentoAdjunto.builder()
                    .nombreOriginal(originalFilename)
                    .nombreAlmacenado(storedFilename)
                    .formatoMime(mimeType)
                    .extension(extension)
                    .tamanoBytes(file.getSize())
                    .rutaAlmacenamiento(storedFilename)
                    .categoria(categoria != null ? categoria : CategoriaDocumento.GENERAL)
                    .descripcion(descripcion)
                    .entidadTipo(entidadTipo)
                    .cotizacion(cotizacion)
                    .expediente(expediente)
                    .cliente(cliente)
                    .usuarioCreador(usuario)
                    .fechaCreacion(LocalDateTime.now())
                    .activo(true)
                    .build();

            DocumentoAdjunto guardado = documentoRepository.save(doc);
            resultados.add(toDTO(guardado));
        }

        return resultados;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoResponseDTO> listar(
            EntidadDocumento entidadTipo,
            Integer idCotizacion,
            Integer idExpediente,
            Integer idCliente,
            CategoriaDocumento categoria
    ) {
        List<DocumentoAdjunto> lista;

        if (idCotizacion != null) {
            lista = documentoRepository.findByCotizacionIdCotizacionAndActivoTrueOrderByFechaCreacionDesc(idCotizacion);
        } else if (idExpediente != null) {
            lista = documentoRepository.findByExpedienteIdExpedienteAndActivoTrueOrderByFechaCreacionDesc(idExpediente);
        } else if (idCliente != null) {
            lista = documentoRepository.findByClienteIdClienteAndActivoTrueOrderByFechaCreacionDesc(idCliente);
        } else if (entidadTipo != null) {
            lista = documentoRepository.findByEntidadTipoAndActivoTrueOrderByFechaCreacionDesc(entidadTipo);
        } else {
            lista = documentoRepository.findByActivoTrueOrderByFechaCreacionDesc();
        }

        if (categoria != null) {
            lista = lista.stream().filter(d -> d.getCategoria() == categoria).toList();
        }

        return lista.stream().map(this::toDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoResponseDTO buscarPorId(Integer id) {
        DocumentoAdjunto doc = obtenerEntidad(id);
        return toDTO(doc);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource descargarArchivo(Integer id) {
        DocumentoAdjunto doc = obtenerEntidad(id);
        return storageService.loadAsResource(doc.getNombreAlmacenado());
    }

    @Override
    @Transactional(readOnly = true)
    public DocumentoAdjunto obtenerEntidad(Integer id) {
        return documentoRepository.findByIdDocumentoAndActivoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public void eliminar(Integer id) {
        DocumentoAdjunto doc = obtenerEntidad(id);
        doc.setActivo(false);
        documentoRepository.save(doc);
        log.info("Documento anulado lógicamente con ID: {} (archivo físico preservado para trazabilidad aduanera)", id);
    }

    private DocumentoResponseDTO toDTO(DocumentoAdjunto d) {
        return DocumentoResponseDTO.builder()
                .idDocumento(d.getIdDocumento())
                .nombreOriginal(d.getNombreOriginal())
                .nombreAlmacenado(d.getNombreAlmacenado())
                .formatoMime(d.getFormatoMime())
                .extension(d.getExtension())
                .tamanoBytes(d.getTamanoBytes())
                .categoria(d.getCategoria())
                .descripcion(d.getDescripcion())
                .entidadTipo(d.getEntidadTipo())
                .idCotizacion(d.getCotizacion() != null ? d.getCotizacion().getIdCotizacion() : null)
                .codigoCotizacion(d.getCotizacion() != null ? d.getCotizacion().getCodigoCotizacion() : null)
                .idExpediente(d.getExpediente() != null ? d.getExpediente().getIdExpediente() : null)
                .codigoExpediente(d.getExpediente() != null ? d.getExpediente().getCodigoExpediente() : null)
                .idCliente(d.getCliente() != null ? d.getCliente().getIdCliente() : null)
                .clienteRazonSocial(d.getCliente() != null ? d.getCliente().getRazonSocial() : null)
                .idUsuarioCreador(d.getUsuarioCreador() != null ? d.getUsuarioCreador().getIdUsuario() : null)
                .usuarioCreadorNombre(d.getUsuarioCreador() != null ? d.getUsuarioCreador().getNombreCompleto() : null)
                .fechaCreacion(d.getFechaCreacion())
                .build();
    }
}
