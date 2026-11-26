package pe.sga.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pe.sga.dto.response.ApiResponseDTO;
import pe.sga.entity.*;
import pe.sga.repository.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/catalogos")
@RequiredArgsConstructor
public class CatalogoGeneralController {

    private final CatalogoBancoRepository bancoRepository;
    private final CatalogoCuentaRecaudadoraRepository cuentaRecaudadoraRepository;
    private final CatalogoMetodoPagoRepository metodoPagoRepository;
    private final CatalogoCategoriaMercanciaRepository categoriaMercanciaRepository;
    private final CatalogoEntidadReguladoraRepository entidadReguladoraRepository;
    private final CatalogoMotivoObservacionRepository motivoObservacionRepository;
    private final CatalogoCategoriaDocumentoRepository categoriaDocumentoRepository;
    private final CatalogoDepartamentoRepository departamentoRepository;
    private final CatalogoProvinciaRepository provinciaRepository;

    @GetMapping("/bancos")
    public ResponseEntity<ApiResponseDTO<List<CatalogoBanco>>> listarBancos(
            @RequestParam(required = false) String uso) {
        List<CatalogoBanco> lista;
        if ("ORIGEN".equalsIgnoreCase(uso)) {
            lista = bancoRepository.findByEstadoAndAplicaOrigenTrueOrderByIdBancoAsc("ACTIVO");
        } else if ("DESTINO".equalsIgnoreCase(uso)) {
            lista = bancoRepository.findByEstadoAndAplicaDestinoTrueOrderByIdBancoAsc("ACTIVO");
        } else {
            lista = bancoRepository.findByEstadoOrderByIdBancoAsc("ACTIVO");
        }
        return ResponseEntity.ok(ApiResponseDTO.ok("Catálogo de bancos recuperado", lista));
    }

    @GetMapping("/cuentas-recaudadoras")
    public ResponseEntity<ApiResponseDTO<List<Map<String, Object>>>> listarCuentasRecaudadoras(
            @RequestParam(required = false) String uso,
            @RequestParam(required = false) String moneda) {
        List<CatalogoCuentaRecaudadora> base;
        if ("B2B".equalsIgnoreCase(uso)) {
            base = cuentaRecaudadoraRepository.findByEstadoAndAplicaPortalB2bTrueOrderByIdCuentaAsc("ACTIVO");
        } else if ("SGA".equalsIgnoreCase(uso)) {
            base = cuentaRecaudadoraRepository.findByEstadoAndAplicaSgaTrueOrderByIdCuentaAsc("ACTIVO");
        } else {
            base = cuentaRecaudadoraRepository.findByEstadoOrderByIdCuentaAsc("ACTIVO");
        }

        List<Map<String, Object>> lista = base.stream()
                .filter(c -> moneda == null || moneda.isBlank() || moneda.equalsIgnoreCase(c.getMoneda()))
                .map(c -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("idCuenta", c.getIdCuenta());
                    row.put("codigo", c.getCodigo());
                    row.put("nombre", c.getNombre());
                    row.put("moneda", c.getMoneda());
                    row.put("numeroCuenta", c.getNumeroCuenta());
                    row.put("cci", c.getCci());
                    row.put("bancoCodigo", c.getBanco() != null ? c.getBanco().getCodigo() : null);
                    row.put("banco", c.getBanco() != null ? c.getBanco().getNombre() : null);
                    row.put("aplicaPortalB2b", c.getAplicaPortalB2b());
                    row.put("aplicaSga", c.getAplicaSga());
                    return row;
                })
                .toList();
        return ResponseEntity.ok(ApiResponseDTO.ok("Cuentas recaudadoras recuperadas", lista));
    }

    @GetMapping("/metodos-pago")
    public ResponseEntity<ApiResponseDTO<List<CatalogoMetodoPago>>> listarMetodosPago(
            @RequestParam(required = false) String uso) {
        List<CatalogoMetodoPago> lista;
        if ("COTIZACION".equalsIgnoreCase(uso)) {
            lista = metodoPagoRepository.findByEstadoAndAplicaCotizacionTrueOrderByIdMetodoPagoAsc("ACTIVO");
        } else if ("LIQUIDACION".equalsIgnoreCase(uso)) {
            lista = metodoPagoRepository.findByEstadoAndAplicaLiquidacionTrueOrderByIdMetodoPagoAsc("ACTIVO");
        } else if ("B2B".equalsIgnoreCase(uso)) {
            lista = metodoPagoRepository.findByEstadoAndAplicaPortalB2bTrueOrderByIdMetodoPagoAsc("ACTIVO");
        } else {
            lista = metodoPagoRepository.findByEstadoOrderByIdMetodoPagoAsc("ACTIVO");
        }
        return ResponseEntity.ok(ApiResponseDTO.ok("Métodos de pago recuperados", lista));
    }

    @GetMapping("/categorias-mercancia")
    public ResponseEntity<ApiResponseDTO<List<CatalogoCategoriaMercancia>>> listarCategoriasMercancia() {
        return ResponseEntity.ok(ApiResponseDTO.ok(
                "Categorías de mercancía recuperadas",
                categoriaMercanciaRepository.findByEstadoOrderByNombreAsc("ACTIVO")
        ));
    }

    @GetMapping("/entidades-reguladoras")
    public ResponseEntity<ApiResponseDTO<List<CatalogoEntidadReguladora>>> listarEntidadesReguladoras() {
        return ResponseEntity.ok(ApiResponseDTO.ok(
                "Entidades reguladoras recuperadas",
                entidadReguladoraRepository.findByEstadoOrderBySiglaAsc("ACTIVO")
        ));
    }

    @GetMapping("/motivos-observacion")
    public ResponseEntity<ApiResponseDTO<List<CatalogoMotivoObservacion>>> listarMotivosObservacion(
            @RequestParam(required = false) String modulo) {
        List<CatalogoMotivoObservacion> lista = (modulo != null && !modulo.isBlank())
                ? motivoObservacionRepository.findByEstadoAndModuloIgnoreCaseOrderByIdMotivoAsc("ACTIVO", modulo.trim())
                : motivoObservacionRepository.findByEstadoOrderByIdMotivoAsc("ACTIVO");
        return ResponseEntity.ok(ApiResponseDTO.ok("Motivos de observación recuperados", lista));
    }

    @GetMapping("/categorias-documento")
    public ResponseEntity<ApiResponseDTO<List<CatalogoCategoriaDocumento>>> listarCategoriasDocumento() {
        return ResponseEntity.ok(ApiResponseDTO.ok(
                "Categorías de documento recuperadas",
                categoriaDocumentoRepository.findByEstadoOrderByIdCategoriaDocumentoAsc("ACTIVO")
        ));
    }

    @GetMapping("/ubigeo")
    public ResponseEntity<ApiResponseDTO<List<Map<String, Object>>>> listarUbigeo() {
        List<CatalogoDepartamento> deps = departamentoRepository.findByEstadoOrderByNombreAsc("ACTIVO");
        Map<Integer, List<String>> provsByDep = provinciaRepository.findByEstadoOrderByIdProvinciaAsc("ACTIVO")
                .stream()
                .filter(p -> p.getDepartamento() != null)
                .collect(Collectors.groupingBy(
                        p -> p.getDepartamento().getIdDepartamento(),
                        Collectors.mapping(CatalogoProvincia::getNombre, Collectors.toList())
                ));

        List<Map<String, Object>> resultado = deps.stream()
                .map(d -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("idDepartamento", d.getIdDepartamento());
                    item.put("departamento", d.getNombre());
                    item.put("provincias", provsByDep.getOrDefault(d.getIdDepartamento(), List.of()));
                    return item;
                })
                .toList();

        return ResponseEntity.ok(ApiResponseDTO.ok("Ubigeo del Perú recuperado", resultado));
    }
}
