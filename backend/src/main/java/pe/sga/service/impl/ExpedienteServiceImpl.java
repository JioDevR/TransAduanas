package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.sga.dto.request.ExpedienteActualizarDTO;
import pe.sga.dto.request.ExpedienteCanalDTO;
import pe.sga.dto.request.ExpedienteCrearDTO;
import pe.sga.dto.request.ExpedienteLevanteDTO;
import pe.sga.dto.response.ExpedienteResponseDTO;
import pe.sga.entity.Cotizacion;
import pe.sga.entity.Expediente;
import pe.sga.entity.Usuario;
import pe.sga.entity.enums.CanalSunat;
import pe.sga.entity.enums.EstadoCotizacion;
import pe.sga.entity.enums.EstadoExpediente;
import pe.sga.entity.enums.EstadoPagoCotizacion;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.repository.CotizacionPagoRepository;
import pe.sga.repository.CotizacionRepository;
import pe.sga.repository.ExpedienteRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.IExpedienteService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpedienteServiceImpl implements IExpedienteService {

    private final ExpedienteRepository expedienteRepository;
    private final CotizacionRepository cotizacionRepository;
    private final UsuarioRepository usuarioRepository;
    private final CotizacionPagoRepository cotizacionPagoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<ExpedienteResponseDTO> listarTodos(Boolean soloActivos) {
        if (soloActivos != null && soloActivos) {
            return expedienteRepository.findByEstadoExpedienteNot(EstadoExpediente.ANULADO).stream()
                    .map(this::mapToResponse)
                    .toList();
        }
        return expedienteRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpedienteResponseDTO> listarPorCanal(CanalSunat canal) {
        return expedienteRepository.findByCanalSunat(canal).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpedienteResponseDTO> listarPorEstado(EstadoExpediente estado) {
        return expedienteRepository.findByEstadoExpediente(estado).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpedienteResponseDTO> listarPorAgente(Integer idAgenteAduanero) {
        return expedienteRepository.findByAgenteAduanero_IdUsuario(idAgenteAduanero).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExpedienteResponseDTO> listarPorCliente(Integer idCliente) {
        return expedienteRepository.findByCliente_IdCliente(idCliente).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ExpedienteResponseDTO buscarPorId(Integer idExpediente) {
        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));
        return mapToResponse(expediente);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpedienteResponseDTO buscarPorCodigo(String codigoExpediente) {
        Expediente expediente = expedienteRepository.findByCodigoExpediente(codigoExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con código: " + codigoExpediente));
        return mapToResponse(expediente);
    }

    @Override
    @Transactional(readOnly = true)
    public ExpedienteResponseDTO buscarPorCotizacion(Integer idCotizacion) {
        Expediente expediente = expedienteRepository.findByCotizacion_IdCotizacion(idCotizacion)
                .orElseThrow(() -> new ResourceNotFoundException("No existe expediente para la cotización ID: " + idCotizacion));
        return mapToResponse(expediente);
    }

    @Override
    @Transactional
    public ExpedienteResponseDTO crear(ExpedienteCrearDTO dto) {
        Cotizacion cotizacion = cotizacionRepository.findById(dto.getIdCotizacion())
                .orElseThrow(() -> new ResourceNotFoundException("Cotización no encontrada con ID: " + dto.getIdCotizacion()));

        if (cotizacion.getEstado() != EstadoCotizacion.APROBADA) {
            throw new BadRequestException("Solo se pueden aperturar expedientes para cotizaciones con estado APROBADA. Estado actual: " + cotizacion.getEstado());
        }

        // COMPUERTA FINANCIERA: Debe existir un pago o adelanto validado y aprobado por Finanzas
        boolean tienePagoAprobado = "ADELANTO_APROBADO".equals(cotizacion.getEstadoPago())
                || "PAGADO_TOTAL".equals(cotizacion.getEstadoPago())
                || cotizacionPagoRepository.existsByCotizacion_IdCotizacionAndEstadoPago(cotizacion.getIdCotizacion(), EstadoPagoCotizacion.APROBADO);

        if (!tienePagoAprobado) {
            throw new BadRequestException("No es posible aperturar el expediente aduanero. Se requiere un pago o adelanto validado y aprobado por el área de Finanzas.");
        }

        if (expedienteRepository.existsByCotizacion_IdCotizacion(dto.getIdCotizacion())) {
            throw new BadRequestException("Ya existe un expediente aduanero aperturado para la cotización ID: " + dto.getIdCotizacion());
        }

        Usuario agente = usuarioRepository.findById(dto.getIdAgenteAduanero())
                .orElseThrow(() -> new ResourceNotFoundException("Agente aduanero no encontrado con ID: " + dto.getIdAgenteAduanero()));

        String codigoExpediente = generarCodigoExpediente();

        Expediente expediente = Expediente.builder()
                .codigoExpediente(codigoExpediente)
                .cotizacion(cotizacion)
                .cliente(cotizacion.getCliente())
                .agenteAduanero(agente)
                .fechaAsignacion(LocalDateTime.now())
                .contactoExteriorNombre(dto.getContactoExteriorNombre())
                .contactoExteriorTelefono(dto.getContactoExteriorTelefono())
                .contactoExteriorCorreo(dto.getContactoExteriorCorreo())
                .fechaZarpeEtd(dto.getFechaZarpeEtd())
                .fechaLlegadaEta(dto.getFechaLlegadaEta())
                .numeroContenedor(dto.getNumeroContenedor())
                .precintoAduanero(dto.getPrecintoAduanero())
                .naveViaje(dto.getNaveViaje())
                .canalSunat(CanalSunat.PENDIENTE)
                .notificadoAgente(false)
                .estadoExpediente(EstadoExpediente.ASIGNADO)
                .fechaApertura(LocalDateTime.now())
                .observaciones(dto.getObservaciones())
                .build();

        return mapToResponse(expedienteRepository.save(expediente));
    }

    @Override
    @Transactional
    public ExpedienteResponseDTO actualizar(Integer idExpediente, ExpedienteActualizarDTO dto) {
        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));

        if (expediente.getEstadoExpediente() == EstadoExpediente.ANULADO || expediente.getEstadoExpediente() == EstadoExpediente.CERRADO) {
            throw new BadRequestException("No se puede editar un expediente en estado: " + expediente.getEstadoExpediente());
        }

        if (dto.getContactoExteriorNombre() != null) expediente.setContactoExteriorNombre(dto.getContactoExteriorNombre());
        if (dto.getContactoExteriorTelefono() != null) expediente.setContactoExteriorTelefono(dto.getContactoExteriorTelefono());
        if (dto.getContactoExteriorCorreo() != null) expediente.setContactoExteriorCorreo(dto.getContactoExteriorCorreo());
        if (dto.getFechaZarpeEtd() != null) expediente.setFechaZarpeEtd(dto.getFechaZarpeEtd());
        if (dto.getFechaLlegadaEta() != null) expediente.setFechaLlegadaEta(dto.getFechaLlegadaEta());
        if (dto.getFechaArriboAduana() != null) expediente.setFechaArriboAduana(dto.getFechaArriboAduana());
        if (dto.getNumeroContenedor() != null) expediente.setNumeroContenedor(dto.getNumeroContenedor());
        if (dto.getPrecintoAduanero() != null) expediente.setPrecintoAduanero(dto.getPrecintoAduanero());
        if (dto.getNaveViaje() != null) expediente.setNaveViaje(dto.getNaveViaje());
        if (dto.getObservaciones() != null) expediente.setObservaciones(dto.getObservaciones());

        return mapToResponse(expedienteRepository.save(expediente));
    }

    @Override
    @Transactional
    public ExpedienteResponseDTO asignarCanal(Integer idExpediente, ExpedienteCanalDTO dto) {
        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));

        String dam = dto.getNumeroDam().trim();
        if (expediente.getNumeroDam() == null || !expediente.getNumeroDam().equalsIgnoreCase(dam)) {
            if (expedienteRepository.existsByNumeroDam(dam)) {
                throw new BadRequestException("Ya existe un expediente con el número de DAM: " + dam);
            }
            expediente.setNumeroDam(dam);
        }

        expediente.setCanalSunat(dto.getCanalSunat());
        expediente.setFechaAsignacionCanal(dto.getFechaAsignacionCanal() != null ? dto.getFechaAsignacionCanal() : LocalDateTime.now());

        if (dto.getDetalleInspeccion() != null) {
            expediente.setDetalleInspeccion(dto.getDetalleInspeccion());
        }

        // Regla de Negocio: Canales Naranja y Rojo activan notificación automática al agente
        if (dto.getCanalSunat() == CanalSunat.NARANJA || dto.getCanalSunat() == CanalSunat.ROJO) {
            expediente.setNotificadoAgente(true);
            expediente.setFechaNotificacion(LocalDateTime.now());
            expediente.setEstadoExpediente(EstadoExpediente.INSPECCION);
        } else if (dto.getCanalSunat() == CanalSunat.VERDE) {
            expediente.setNotificadoAgente(false);
            expediente.setEstadoExpediente(EstadoExpediente.EN_TRAMITE);
        }

        return mapToResponse(expedienteRepository.save(expediente));
    }

    @Override
    @Transactional
    public ExpedienteResponseDTO autorizarLevante(Integer idExpediente, ExpedienteLevanteDTO dto) {
        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));

        if (expediente.getNumeroDam() == null || expediente.getCanalSunat() == CanalSunat.PENDIENTE) {
            throw new BadRequestException("No se puede autorizar levante sin antes haber asignado DAM y canal aduanero");
        }

        expediente.setFechaLevanteAutorizacion(dto.getFechaLevanteAutorizacion() != null ? dto.getFechaLevanteAutorizacion() : LocalDateTime.now());
        if (dto.getDetalleInspeccion() != null) {
            expediente.setDetalleInspeccion(dto.getDetalleInspeccion());
        }
        if (dto.getObservaciones() != null) {
            expediente.setObservaciones(dto.getObservaciones());
        }
        expediente.setEstadoExpediente(EstadoExpediente.LEVANTE_AUTORIZADO);

        return mapToResponse(expedienteRepository.save(expediente));
    }

    @Override
    @Transactional
    public void anular(Integer idExpediente) {
        Expediente expediente = expedienteRepository.findById(idExpediente)
                .orElseThrow(() -> new ResourceNotFoundException("Expediente no encontrado con ID: " + idExpediente));
        // Borrado lógico estándar
        expediente.setEstadoExpediente(EstadoExpediente.ANULADO);
        expediente.setFechaCierre(LocalDateTime.now());
        expedienteRepository.save(expediente);
    }

    private String generarCodigoExpediente() {
        int year = LocalDate.now().getYear();
        long count = expedienteRepository.count() + 1;
        String codigo;
        do {
            codigo = String.format("EXP-%d-%04d", year, count);
            count++;
        } while (expedienteRepository.existsByCodigoExpediente(codigo));
        return codigo;
    }

    private ExpedienteResponseDTO mapToResponse(Expediente e) {
        return ExpedienteResponseDTO.builder()
                .idExpediente(e.getIdExpediente())
                .codigoExpediente(e.getCodigoExpediente())
                .idCotizacion(e.getCotizacion() != null ? e.getCotizacion().getIdCotizacion() : null)
                .codigoCotizacion(e.getCotizacion() != null ? e.getCotizacion().getCodigoCotizacion() : null)
                .regimenAduanero(e.getCotizacion() != null ? e.getCotizacion().getRegimenAduanero() : null)
                .idCliente(e.getCliente() != null ? e.getCliente().getIdCliente() : null)
                .clienteRazonSocial(e.getCliente() != null ? e.getCliente().getRazonSocial() : null)
                .clienteNumeroDocumento(e.getCliente() != null ? e.getCliente().getNumeroDocumento() : null)
                .idAgenteAduanero(e.getAgenteAduanero() != null ? e.getAgenteAduanero().getIdUsuario() : null)
                .agenteAduaneroNombre(e.getAgenteAduanero() != null ? e.getAgenteAduanero().getNombreCompleto() : null)
                .fechaAsignacion(e.getFechaAsignacion())
                .contactoExteriorNombre(e.getContactoExteriorNombre())
                .contactoExteriorTelefono(e.getContactoExteriorTelefono())
                .contactoExteriorCorreo(e.getContactoExteriorCorreo())
                .fechaZarpeEtd(e.getFechaZarpeEtd())
                .fechaLlegadaEta(e.getFechaLlegadaEta())
                .fechaArriboAduana(e.getFechaArriboAduana())
                .numeroDam(e.getNumeroDam())
                .canalSunat(e.getCanalSunat())
                .fechaAsignacionCanal(e.getFechaAsignacionCanal())
                .notificadoAgente(e.getNotificadoAgente())
                .fechaNotificacion(e.getFechaNotificacion())
                .detalleInspeccion(e.getDetalleInspeccion())
                .fechaLevanteAutorizacion(e.getFechaLevanteAutorizacion())
                .numeroContenedor(e.getNumeroContenedor())
                .precintoAduanero(e.getPrecintoAduanero())
                .naveViaje(e.getNaveViaje())
                .estadoExpediente(e.getEstadoExpediente())
                .fechaApertura(e.getFechaApertura())
                .fechaCierre(e.getFechaCierre())
                .observaciones(e.getObservaciones())
                .build();
    }
}
