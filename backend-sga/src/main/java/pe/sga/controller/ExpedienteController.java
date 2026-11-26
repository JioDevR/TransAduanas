package pe.sga.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import pe.sga.config.SecurityRoles;
import pe.sga.dto.request.ExpedienteActualizarDTO;
import pe.sga.dto.request.ExpedienteArriboDestinoDTO;
import pe.sga.dto.request.ExpedienteAsignarAgenteDTO;
import pe.sga.dto.request.ExpedienteCanalDTO;
import pe.sga.dto.request.ExpedienteCerrarDTO;
import pe.sga.dto.request.ExpedienteCrearDTO;
import pe.sga.dto.request.ExpedienteCuadrillaCitaDTO;
import pe.sga.dto.request.ExpedienteDiligenciaDTO;
import pe.sga.dto.request.ExpedienteEmbarqueExportacionDTO;
import pe.sga.dto.request.ExpedienteEventoRequestDTO;
import pe.sga.dto.request.ExpedienteInformeConformidadDTO;
import pe.sga.dto.request.ExpedienteIngresoOrigenDTO;
import pe.sga.dto.request.ExpedienteLevanteDTO;
import pe.sga.dto.request.ExpedienteRecepcionExteriorDTO;
import pe.sga.dto.request.ExpedienteRecojoExportacionDTO;
import pe.sga.dto.request.ExpedienteRetiroDirectoDTO;
import pe.sga.dto.request.ExpedienteSalidaOrigenDTO;
import pe.sga.dto.request.ExpedienteSolicitudTransporteDTO;
import pe.sga.dto.request.ExpedienteSunatSimuladoDTO;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.dto.response.ExpedienteResponseDTO;
import pe.sga.entity.Usuario;
import pe.sga.entity.enums.CanalSunat;
import pe.sga.entity.enums.EstadoExpediente;
import pe.sga.service.IExpedienteService;
import pe.sga.service.IReporteJasperService;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/expedientes")
@RequiredArgsConstructor
@PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_READ)
public class ExpedienteController {

    private final IExpedienteService expedienteService;
    private IReporteJasperService reporteJasperService;

    @Autowired(required = false)
    public void setReporteJasperService(IReporteJasperService reporteJasperService) {
        this.reporteJasperService = reporteJasperService;
    }

    @GetMapping
    public ResponseEntity<ApiResponseDTO<List<ExpedienteResponseDTO>>> listar(
            @RequestParam(required = false, defaultValue = "false") boolean soloActivos,
            @RequestParam(required = false) CanalSunat canal,
            @RequestParam(required = false) EstadoExpediente estado,
            @RequestParam(required = false) Integer idAgente,
            @RequestParam(required = false) Integer idCliente) {
        List<ExpedienteResponseDTO> lista;
        if (canal != null) {
            lista = expedienteService.listarPorCanal(canal);
        } else if (estado != null) {
            lista = expedienteService.listarPorEstado(estado);
        } else if (idAgente != null) {
            lista = expedienteService.listarPorAgente(idAgente);
        } else if (idCliente != null) {
            lista = expedienteService.listarPorCliente(idCliente);
        } else {
            lista = expedienteService.listarTodos(soloActivos);
        }
        return ResponseEntity.ok(ApiResponseDTO.ok("Lista de expedientes aduaneros recuperada", lista));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> buscarPorId(@PathVariable Integer id) {
        ExpedienteResponseDTO dto = expedienteService.buscarPorId(id);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @GetMapping("/codigo/{codigo}")
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> buscarPorCodigo(@PathVariable String codigo) {
        ExpedienteResponseDTO dto = expedienteService.buscarPorCodigo(codigo);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @GetMapping("/cotizacion/{idCotizacion}")
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> buscarPorCotizacion(@PathVariable Integer idCotizacion) {
        ExpedienteResponseDTO dto = expedienteService.buscarPorCotizacion(idCotizacion);
        return ResponseEntity.ok(ApiResponseDTO.ok(dto));
    }

    @PostMapping
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> crear(@Valid @RequestBody ExpedienteCrearDTO dto) {
        ExpedienteResponseDTO creado = expedienteService.crear(dto);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Expediente aduanero aperturado exitosamente", creado));
    }

    @PutMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> actualizar(
            @PathVariable Integer id,
            @Valid @RequestBody ExpedienteActualizarDTO dto) {
        ExpedienteResponseDTO actualizado = expedienteService.actualizar(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Expediente aduanero actualizado exitosamente", actualizado));
    }

    @PatchMapping("/{id}/canal")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> asignarCanal(
            @PathVariable Integer id,
            @Valid @RequestBody ExpedienteCanalDTO dto) {
        ExpedienteResponseDTO actualizado = expedienteService.asignarCanal(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Canal SUNAT y numeración de DAM registrados", actualizado));
    }

    @PatchMapping("/{id}/diligencia")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> registrarDiligencia(
            @PathVariable Integer id,
            @Valid @RequestBody ExpedienteDiligenciaDTO dto) {
        ExpedienteResponseDTO actualizado = expedienteService.registrarDiligencia(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Diligencia de inspección / aforo registrada exitosamente", actualizado));
    }

    @PatchMapping("/{id}/levante")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> autorizarLevante(
            @PathVariable Integer id,
            @Valid @RequestBody ExpedienteLevanteDTO dto) {
        ExpedienteResponseDTO actualizado = expedienteService.autorizarLevante(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Levante aduanero autorizado exitosamente", actualizado));
    }

    @PostMapping("/{id}/solicitar-recojo-exportacion")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> solicitarRecojoExportacion(
            @PathVariable Integer id,
            @Valid @RequestBody ExpedienteRecojoExportacionDTO dto) {
        ExpedienteResponseDTO actualizado = expedienteService.solicitarRecojoExportacion(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Solicitud de recojo en planta enviada al módulo de Transporte y Flota", actualizado));
    }

    @PostMapping("/{id}/confirmar-ingreso-origen")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> confirmarIngresoOrigen(
            @PathVariable Integer id,
            @Valid @RequestBody ExpedienteIngresoOrigenDTO dto) {
        ExpedienteResponseDTO actualizado = expedienteService.confirmarIngresoOrigen(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Ingreso a almacén/depósito temporal de origen confirmado", actualizado));
    }

    @PatchMapping("/{id}/confirmar-salida-origen")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> confirmarSalidaOrigen(
            @PathVariable Integer id,
            @Valid @RequestBody ExpedienteSalidaOrigenDTO dto) {
        ExpedienteResponseDTO actualizado = expedienteService.confirmarSalidaOrigen(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Salida de origen confirmada exitosamente. Carga en tránsito internacional.", actualizado));
    }

    @PatchMapping("/{id}/confirmar-arribo-destino")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> confirmarArriboDestino(
            @PathVariable Integer id,
            @Valid @RequestBody ExpedienteArriboDestinoDTO dto) {
        ExpedienteResponseDTO actualizado = expedienteService.confirmarArriboDestino(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Arribo a puerto/aeropuerto de destino confirmado exitosamente", actualizado));
    }

    @PatchMapping("/{id}/confirmar-embarque")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> confirmarEmbarque(
            @PathVariable Integer id,
            @Valid @RequestBody ExpedienteEmbarqueExportacionDTO dto) {
        ExpedienteResponseDTO actualizado = expedienteService.confirmarEmbarqueOrigen(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Confirmación de embarque internacional registrada", actualizado));
    }

    @PatchMapping("/{id}/confirmar-recepcion-exterior")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> confirmarRecepcionExterior(
            @PathVariable Integer id,
            @Valid @RequestBody ExpedienteRecepcionExteriorDTO dto) {
        ExpedienteResponseDTO actualizado = expedienteService.confirmarRecepcionExterior(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Recepción conforme en destino exterior registrada", actualizado));
    }

    @PostMapping("/{id}/solicitar-transporte")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> solicitarTransporte(
            @PathVariable Integer id,
            @Valid @RequestBody ExpedienteSolicitudTransporteDTO dto) {
        ExpedienteResponseDTO actualizado = expedienteService.solicitarTransporte(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Solicitud de despacho terrestre enviada a Flota y Transporte", actualizado));
    }

    @PostMapping("/{id}/retiro-directo")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> registrarRetiroDirecto(
            @PathVariable Integer id,
            @Valid @RequestBody ExpedienteRetiroDirectoDTO dto) {
        ExpedienteResponseDTO actualizado = expedienteService.registrarRetiroDirecto(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Acta de retiro directo de terminal registrada", actualizado));
    }

    @PatchMapping("/{id}/cerrar")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> cerrar(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) ExpedienteCerrarDTO dto) {
        ExpedienteResponseDTO actualizado = expedienteService.cerrar(id, dto);
        return ResponseEntity.ok(ApiResponseDTO.ok("Expediente aduanero cerrado formalmente con conformidad", actualizado));
    }

    @PatchMapping("/{id}/asignar-agente")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> asignarAgente(
            @PathVariable Integer id,
            @Valid @RequestBody ExpedienteAsignarAgenteDTO dto,
            @AuthenticationPrincipal Usuario usuarioActual) {
        String actor = usuarioActual != null ? usuarioActual.getNombreCompleto() : "Gestión Aduanera";
        ExpedienteResponseDTO actualizado = expedienteService.asignarAgente(id, dto, actor);
        return ResponseEntity.ok(ApiResponseDTO.ok("Agente aduanero asignado exitosamente al expediente", actualizado));
    }

    @PostMapping("/{id}/eventos")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> registrarEventoManual(
            @PathVariable Integer id,
            @Valid @RequestBody ExpedienteEventoRequestDTO dto,
            @AuthenticationPrincipal Usuario usuarioActual) {
        String actor = usuarioActual != null ? usuarioActual.getNombreCompleto() : "Agente Aduanero";
        ExpedienteResponseDTO actualizado = expedienteService.registrarEventoManual(id, dto, actor);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponseDTO.ok("Estado y marca de tiempo registrados en el expediente", actualizado));
    }

    @PatchMapping("/{id}/verificacion-origen")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> confirmarVerificacionOrigen(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal Usuario usuarioActual) {
        String actor = usuarioActual != null ? usuarioActual.getNombreCompleto() : "Agente Aduanero";
        String obs = body != null ? body.get("observaciones") : null;
        ExpedienteResponseDTO actualizado = expedienteService.confirmarVerificacionOrigen(id, obs, actor);
        return ResponseEntity.ok(ApiResponseDTO.ok("Verificación en origen confirmada", actualizado));
    }

    @PatchMapping("/{id}/canal-rojo/cuadrilla-cita")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> solicitarCuadrillaYCitaRojo(
            @PathVariable Integer id,
            @Valid @RequestBody ExpedienteCuadrillaCitaDTO dto,
            @AuthenticationPrincipal Usuario usuarioActual) {
        String actor = usuarioActual != null ? usuarioActual.getNombreCompleto() : "Agente Aduanero";
        ExpedienteResponseDTO actualizado = expedienteService.solicitarCuadrillaYCitaRojo(id, dto, actor);
        return ResponseEntity.ok(ApiResponseDTO.ok("Cuadrilla solicitada y cita de inspección física registrada", actualizado));
    }

    @PatchMapping("/{id}/canal-rojo/finalizar-revision")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> finalizarRevisionRojo(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) ExpedienteLevanteDTO dto,
            @AuthenticationPrincipal Usuario usuarioActual) {
        String actor = usuarioActual != null ? usuarioActual.getNombreCompleto() : "Agente Aduanero";
        ExpedienteResponseDTO actualizado = expedienteService.finalizarRevisionRojo(id, dto, actor);
        return ResponseEntity.ok(ApiResponseDTO.ok("Revisión física de Canal Rojo finalizada y levante autorizado", actualizado));
    }

    @PostMapping("/{id}/canal-naranja/enviar-sunat")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> enviarSunatSimulado(
            @PathVariable Integer id,
            @Valid @RequestBody(required = false) ExpedienteSunatSimuladoDTO dto,
            @AuthenticationPrincipal Usuario usuarioActual) {
        String actor = usuarioActual != null ? usuarioActual.getNombreCompleto() : "Agente Aduanero";
        ExpedienteResponseDTO actualizado = expedienteService.enviarDocumentosSunatSimulado(id, dto, actor);
        return ResponseEntity.ok(ApiResponseDTO.ok("Documentos transmitidos a SUNAT (simulado) exitosamente", actualizado));
    }

    @PatchMapping("/{id}/canal-verde/entregado-almacen")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> marcarEntregadoAlmacenVerde(
            @PathVariable Integer id,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal Usuario usuarioActual) {
        String actor = usuarioActual != null ? usuarioActual.getNombreCompleto() : "Agente Aduanero";
        String obs = body != null ? body.get("observaciones") : null;
        ExpedienteResponseDTO actualizado = expedienteService.marcarEntregadoAlmacenVerde(id, obs, actor);
        return ResponseEntity.ok(ApiResponseDTO.ok("Carga de Canal Verde marcada como entregada en almacén", actualizado));
    }

    @PostMapping("/{id}/informe-conformidad")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<ExpedienteResponseDTO>> emitirInformeConformidad(
            @PathVariable Integer id,
            @Valid @RequestBody ExpedienteInformeConformidadDTO dto,
            @AuthenticationPrincipal Usuario usuarioActual) {
        String actor = usuarioActual != null ? usuarioActual.getNombreCompleto() : "Agente Aduanero";
        ExpedienteResponseDTO actualizado = expedienteService.emitirInformeConformidadEntrega(id, dto, actor);
        return ResponseEntity.ok(ApiResponseDTO.ok("Informe de conformidad de entrega emitido exitosamente", actualizado));
    }

    @GetMapping("/{id}/informe-conformidad/pdf")
    public ResponseEntity<InputStreamResource> descargarInformeConformidadPdf(@PathVariable Integer id) {
        if (reporteJasperService == null) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        ExpedienteResponseDTO exp = expedienteService.buscarPorId(id);
        ByteArrayInputStream pdf = reporteJasperService.exportarInformeConformidadPdf(exp);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(ContentDisposition.inline()
                .filename("informe-conformidad-" + exp.getCodigoExpediente() + ".pdf")
                .build());
        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_PDF)
                .body(new InputStreamResource(pdf));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize(SecurityRoles.ACCESS_EXPEDIENTE_ALMACEN_WRITE)
    public ResponseEntity<ApiResponseDTO<Void>> eliminar(@PathVariable Integer id) {
        expedienteService.anular(id);
        return ResponseEntity.ok(ApiResponseDTO.ok("Expediente anulado exitosamente (borrado lógico por estado)", null));
    }
}

