package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.security.access.AccessDeniedException;
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
import pe.sga.entity.enums.RolUsuario;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.ClienteRepository;
import pe.sga.repository.CotizacionPagoRepository;
import pe.sga.repository.CotizacionRepository;
import pe.sga.repository.ExpedienteRepository;
import pe.sga.repository.IDocumentoAdjuntoRepository;
import pe.sga.repository.LiquidacionFinancieraRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.IDocumentoService;
import pe.sga.service.IStorageService;

import java.nio.file.Paths;
import java.time.LocalDate;
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
    private final CotizacionPagoRepository cotizacionPagoRepository;
    private final LiquidacionFinancieraRepository liquidacionFinancieraRepository;

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
        Usuario usuario = null;
        if (idUsuario != null && usuarioRepository.existsById(idUsuario)) {
            usuario = usuarioRepository.findById(idUsuario).get();
        } else {
            usuario = usuarioRepository.findAll().stream().findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("No se encontró ningún usuario del sistema registrado"));
        }

        Cotizacion cotizacion = null;
        Expediente expediente = null;
        Cliente cliente = null;

        if (entidadTipo == null) {
            entidadTipo = EntidadDocumento.GENERAL;
        }

        if (idEntidad != null) {
            switch (entidadTipo) {
                case COTIZACION -> {
                    cotizacion = cotizacionRepository.findById(idEntidad)
                            .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con ID: " + idEntidad));
                    cliente = cotizacion.getCliente();
                }
                case EXPEDIENTE -> {
                    expediente = expedienteRepository.findById(idEntidad)
                            .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idEntidad));
                    cliente = expediente.getCliente();
                }
                case CLIENTE -> cliente = clienteRepository.findById(idEntidad)
                        .orElseThrow(() -> new ResourceNotFoundException("Cliente no encontrado con ID: " + idEntidad));
                default -> {}
            }
        }

        List<DocumentoResponseDTO> resultados = new ArrayList<>();
        String subpath = resolverSubdirectorio(entidadTipo, cotizacion, expediente, cliente, categoria, descripcion);

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            String storedRelativePath = storageService.store(file, subpath);
            String storedFilename = Paths.get(storedRelativePath).getFileName().toString();
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
                    .rutaAlmacenamiento(storedRelativePath)
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
        return obtenerDocumentosFiltrados(entidadTipo, idCotizacion, idExpediente, idCliente, categoria)
                .stream()
                .map(this::toDTO)
                .toList();
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
        String ruta = (doc.getRutaAlmacenamiento() != null && !doc.getRutaAlmacenamiento().isBlank())
                ? doc.getRutaAlmacenamiento()
                : doc.getNombreAlmacenado();
        return storageService.loadAsResource(ruta);
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

    @Override
    @Transactional(readOnly = true)
    public void validarPropiedadEntidadCliente(Integer idClienteAutenticado, EntidadDocumento entidadTipo, Integer idEntidad) {
        if (idClienteAutenticado == null || idEntidad == null) {
            throw new AccessDeniedException("No cuenta con permisos para adjuntar documentos a esta entidad");
        }
        EntidadDocumento tipo = entidadTipo != null ? entidadTipo : EntidadDocumento.GENERAL;
        switch (tipo) {
            case COTIZACION -> {
                Cotizacion cot = cotizacionRepository.findById(idEntidad)
                        .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con ID: " + idEntidad));
                if (cot.getCliente() == null || !idClienteAutenticado.equals(cot.getCliente().getIdCliente())) {
                    throw new AccessDeniedException("La cotización no pertenece a su empresa");
                }
            }
            case EXPEDIENTE -> {
                Expediente exp = expedienteRepository.findById(idEntidad)
                        .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idEntidad));
                if (exp.getCliente() == null || !idClienteAutenticado.equals(exp.getCliente().getIdCliente())) {
                    throw new AccessDeniedException("El expediente no pertenece a su empresa");
                }
            }
            case CLIENTE -> {
                if (!idClienteAutenticado.equals(idEntidad)) {
                    throw new AccessDeniedException("No tiene permisos para operar sobre documentos de otra empresa");
                }
            }
            default -> throw new AccessDeniedException("Tipo de entidad no permitido para clientes del portal");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validarPropiedadConsultaCliente(Integer idClienteAutenticado, Integer idCotizacion, Integer idExpediente, Integer idCliente) {
        if (idClienteAutenticado == null) {
            throw new AccessDeniedException("Usuario de portal no válido");
        }
        if (idCotizacion == null && idExpediente == null) {
            throw new AccessDeniedException("Debe especificar una cotización o expediente propio para consultar documentos");
        }
        if (idCliente != null && !idClienteAutenticado.equals(idCliente)) {
            throw new AccessDeniedException("No puede consultar documentos de otro cliente");
        }
        if (idCotizacion != null) {
            Cotizacion cot = cotizacionRepository.findById(idCotizacion)
                    .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con ID: " + idCotizacion));
            if (cot.getCliente() == null || !idClienteAutenticado.equals(cot.getCliente().getIdCliente())) {
                throw new AccessDeniedException("La cotización solicitada no pertenece a su empresa");
            }
        }
        if (idExpediente != null) {
            Expediente exp = expedienteRepository.findById(idExpediente)
                    .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));
            if (exp.getCliente() == null || !idClienteAutenticado.equals(exp.getCliente().getIdCliente())) {
                throw new AccessDeniedException("El expediente solicitado no pertenece a su empresa");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validarPropiedadDocumentoCliente(Integer idClienteAutenticado, DocumentoAdjunto doc) {
        if (idClienteAutenticado == null || doc == null) {
            throw new AccessDeniedException("No tiene permisos para acceder a este documento");
        }
        if (doc.getCliente() != null && idClienteAutenticado.equals(doc.getCliente().getIdCliente())) {
            return;
        }
        if (doc.getCotizacion() != null && doc.getCotizacion().getCliente() != null
                && idClienteAutenticado.equals(doc.getCotizacion().getCliente().getIdCliente())) {
            return;
        }
        if (doc.getExpediente() != null && doc.getExpediente().getCliente() != null
                && idClienteAutenticado.equals(doc.getExpediente().getCliente().getIdCliente())) {
            return;
        }
        // Verificar si es comprobante de un pago de cotización o liquidación del cliente
        boolean esComprobantePagoCliente = cotizacionPagoRepository
                .findByCotizacion_Cliente_IdClienteOrderByFechaRegistroDesc(idClienteAutenticado)
                .stream()
                .anyMatch(p -> p.getDocumentoComprobante() != null
                        && doc.getIdDocumento().equals(p.getDocumentoComprobante().getIdDocumento()));
        if (esComprobantePagoCliente) {
            return;
        }

        boolean esDocumentoLiquidacionCliente = liquidacionFinancieraRepository
                .findByExpediente_Cliente_IdCliente(idClienteAutenticado)
                .stream()
                .anyMatch(l -> (l.getDocumentoVoucher() != null && doc.getIdDocumento().equals(l.getDocumentoVoucher().getIdDocumento()))
                        || (l.getDocumentoFactura() != null && doc.getIdDocumento().equals(l.getDocumentoFactura().getIdDocumento())));
        if (esDocumentoLiquidacionCliente) {
            return;
        }

        throw new AccessDeniedException("Acceso denegado: el documento solicitado no pertenece a su empresa");
    }

    @Override
    @Transactional(readOnly = true)
    public void validarAccesoComercialCotizacion(Usuario usuario, Integer idCotizacion, boolean permitirSinAsignar) {
        if (usuario == null || usuario.getRol() != RolUsuario.AGENTE_COMERCIAL || idCotizacion == null) {
            return;
        }
        Cotizacion cot = cotizacionRepository.findById(idCotizacion)
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con ID: " + idCotizacion));
        if (!puedeAccederCotizacionComercial(usuario, cot, permitirSinAsignar)) {
            throw new AccessDeniedException("Acceso denegado: no tiene permisos sobre los documentos de esta cotización");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void validarAccesoDocumentoComercial(Usuario usuario, DocumentoAdjunto doc, boolean permitirSinAsignar) {
        if (usuario == null || usuario.getRol() != RolUsuario.AGENTE_COMERCIAL || doc == null) {
            return;
        }
        if (doc.getCotizacion() != null && !puedeAccederCotizacionComercial(usuario, doc.getCotizacion(), permitirSinAsignar)) {
            throw new AccessDeniedException("Acceso denegado: el documento pertenece a una cotización de otro asesor comercial");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<DocumentoResponseDTO> listarParaUsuario(
            Usuario usuario,
            EntidadDocumento entidadTipo,
            Integer idCotizacion,
            Integer idExpediente,
            Integer idCliente,
            CategoriaDocumento categoria
    ) {
        if (usuario != null && usuario.getRol() == RolUsuario.AGENTE_COMERCIAL) {
            if (idCotizacion != null) {
                validarAccesoComercialCotizacion(usuario, idCotizacion, true);
            }
            List<DocumentoAdjunto> base = obtenerDocumentosFiltrados(entidadTipo, idCotizacion, idExpediente, idCliente, categoria);
            return base.stream()
                    .filter(d -> d.getCotizacion() == null || puedeAccederCotizacionComercial(usuario, d.getCotizacion(), true))
                    .map(this::toDTO)
                    .toList();
        }
        return listar(entidadTipo, idCotizacion, idExpediente, idCliente, categoria);
    }

    private boolean puedeAccederCotizacionComercial(Usuario usuario, Cotizacion cot, boolean permitirSinAsignar) {
        if (cot == null) return true;
        if (cot.getUsuarioComercial() == null) {
            return permitirSinAsignar;
        }
        return cot.getUsuarioComercial().getIdUsuario().equals(usuario.getIdUsuario());
    }

    private List<DocumentoAdjunto> obtenerDocumentosFiltrados(
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
        return lista;
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

    private String resolverSubdirectorio(
            EntidadDocumento entidadTipo,
            Cotizacion cotizacion,
            Expediente expediente,
            Cliente cliente,
            CategoriaDocumento categoria,
            String descripcion
    ) {
        LocalDate hoy = LocalDate.now();
        String anio = String.valueOf(hoy.getYear());
        String mes = String.format("%02d", hoy.getMonthValue());
        String catFolder = categoria != null ? categoria.name().toLowerCase() : "general";

        if (expediente != null) {
            String expAnio = expediente.getFechaApertura() != null
                    ? String.valueOf(expediente.getFechaApertura().getYear())
                    : anio;
            String expCod = (expediente.getCodigoExpediente() != null && !expediente.getCodigoExpediente().isBlank())
                    ? sanitizarNombreDirectorio(expediente.getCodigoExpediente())
                    : "EXP-" + expediente.getIdExpediente();
            return "expedientes/" + expAnio + "/" + expCod + "/" + catFolder;
        }

        if (cotizacion != null) {
            String cotAnio = cotizacion.getFechaEmision() != null
                    ? String.valueOf(cotizacion.getFechaEmision().getYear())
                    : anio;
            String cotCod = (cotizacion.getCodigoCotizacion() != null && !cotizacion.getCodigoCotizacion().isBlank())
                    ? sanitizarNombreDirectorio(cotizacion.getCodigoCotizacion())
                    : "COT-" + cotizacion.getIdCotizacion();
            return "cotizaciones/" + cotAnio + "/" + cotCod;
        }

        if (entidadTipo == EntidadDocumento.CLIENTE || cliente != null) {
            String cliCod = (cliente != null && cliente.getNumeroDocumento() != null && !cliente.getNumeroDocumento().isBlank())
                    ? sanitizarNombreDirectorio(cliente.getNumeroDocumento())
                    : (cliente != null ? "CLI-" + cliente.getIdCliente() : "clientes-varios");

            boolean esPago = (descripcion != null && (descripcion.toLowerCase().contains("voucher")
                    || descripcion.toLowerCase().contains("pago")
                    || descripcion.toLowerCase().contains("abono")))
                    || (categoria == CategoriaDocumento.GENERAL && descripcion != null && descripcion.toLowerCase().contains("abono"));
            if (esPago) {
                return "pagos/" + anio + "/" + mes + "/" + cliCod;
            }
            return "clientes/" + cliCod + "/" + catFolder;
        }

        return "general/" + anio + "/" + mes;
    }

    private String sanitizarNombreDirectorio(String input) {
        if (input == null) return "general";
        return input.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
