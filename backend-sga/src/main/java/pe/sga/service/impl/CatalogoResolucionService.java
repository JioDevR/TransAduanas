package pe.sga.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pe.sga.entity.*;
import pe.sga.repository.*;

@Service
@RequiredArgsConstructor
public class CatalogoResolucionService {

    private final CatalogoBancoRepository bancoRepository;
    private final CatalogoCuentaRecaudadoraRepository cuentaRecaudadoraRepository;
    private final CatalogoMetodoPagoRepository metodoPagoRepository;
    private final CatalogoCategoriaMercanciaRepository categoriaMercanciaRepository;
    private final CatalogoEntidadReguladoraRepository entidadReguladoraRepository;
    private final CatalogoCategoriaDocumentoRepository categoriaDocumentoRepository;
    private final CatalogoProvinciaRepository provinciaRepository;

    public void enriquecerMercancia(CatalogoMercancia mercancia) {
        if (mercancia == null) return;
        if (mercancia.getCategoria() != null && !mercancia.getCategoria().isBlank()) {
            String cat = mercancia.getCategoria().trim();
            categoriaMercanciaRepository.findByCodigoIgnoreCase(cat)
                    .or(() -> categoriaMercanciaRepository.findByNombreIgnoreCase(cat))
                    .ifPresent(mercancia::setCategoriaEntity);
        }
        if (mercancia.getEntidadReguladora() != null && !mercancia.getEntidadReguladora().isBlank()) {
            entidadReguladoraRepository.findBySiglaIgnoreCase(mercancia.getEntidadReguladora().trim())
                    .ifPresent(mercancia::setEntidadReguladoraEntity);
        } else {
            mercancia.setEntidadReguladoraEntity(null);
        }
    }

    public void enriquecerCotizacion(Cotizacion cotizacion) {
        if (cotizacion == null) return;
        if (cotizacion.getOrigenDepartamento() != null && cotizacion.getOrigenProvincia() != null) {
            provinciaRepository.findByDepartamento_NombreIgnoreCaseAndNombreIgnoreCase(
                    cotizacion.getOrigenDepartamento().trim(),
                    cotizacion.getOrigenProvincia().trim()
            ).ifPresent(cotizacion::setProvinciaOrigenEntity);
        }
        if (cotizacion.getDestinoDepartamento() != null && cotizacion.getDestinoProvincia() != null) {
            provinciaRepository.findByDepartamento_NombreIgnoreCaseAndNombreIgnoreCase(
                    cotizacion.getDestinoDepartamento().trim(),
                    cotizacion.getDestinoProvincia().trim()
            ).ifPresent(cotizacion::setProvinciaDestinoEntity);
        }
    }

    public void enriquecerPagoCotizacion(CotizacionPago pago) {
        if (pago == null) return;
        if (pago.getMetodoPago() != null && !pago.getMetodoPago().isBlank()) {
            metodoPagoRepository.findByCodigoIgnoreCase(pago.getMetodoPago().trim())
                    .ifPresent(pago::setMetodoPagoEntity);
        }
        if (pago.getBancoOrigen() != null && !pago.getBancoOrigen().isBlank()) {
            String bo = pago.getBancoOrigen().trim();
            bancoRepository.findByCodigoIgnoreCase(bo)
                    .or(() -> bancoRepository.findByNombreIgnoreCase(bo))
                    .ifPresent(pago::setBancoOrigenEntity);
        }
        if (pago.getBancoDestino() != null && !pago.getBancoDestino().isBlank()) {
            String bd = pago.getBancoDestino().trim();
            cuentaRecaudadoraRepository.findByCodigoIgnoreCase(bd)
                    .or(() -> cuentaRecaudadoraRepository.findByNombreIgnoreCase(bd))
                    .ifPresent(cta -> {
                        pago.setCuentaRecaudadora(cta);
                        pago.setBancoDestinoEntity(cta.getBanco());
                    });
            if (pago.getBancoDestinoEntity() == null) {
                bancoRepository.findByCodigoIgnoreCase(bd)
                        .or(() -> bancoRepository.findByNombreIgnoreCase(bd))
                        .ifPresent(pago::setBancoDestinoEntity);
            }
        }
    }

    public void enriquecerLiquidacion(LiquidacionFinanciera lf) {
        if (lf == null) return;
        if (lf.getMetodoPago() != null && !lf.getMetodoPago().isBlank()) {
            metodoPagoRepository.findByCodigoIgnoreCase(lf.getMetodoPago().trim())
                    .ifPresent(lf::setMetodoPagoEntity);
        }
        if (lf.getBancoDestino() != null && !lf.getBancoDestino().isBlank()) {
            String bd = lf.getBancoDestino().trim();
            bancoRepository.findByCodigoIgnoreCase(bd)
                    .or(() -> bancoRepository.findByNombreIgnoreCase(bd))
                    .ifPresent(lf::setBancoDestinoEntity);
        }
    }

    public void enriquecerDocumento(DocumentoAdjunto doc) {
        if (doc == null || doc.getCategoria() == null) return;
        categoriaDocumentoRepository.findByCodigoIgnoreCase(doc.getCategoria().name())
                .ifPresent(doc::setCategoriaDocumentoEntity);
    }
}
