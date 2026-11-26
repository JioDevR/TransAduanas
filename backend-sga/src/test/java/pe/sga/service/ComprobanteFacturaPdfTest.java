package pe.sga.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import pe.sga.dto.response.ComprobanteFacturaResponseDTO;
import pe.sga.repository.CatalogoMercanciaRepository;
import pe.sga.repository.CatalogoPuertoRepository;
import pe.sga.repository.CatalogoTipoCargaRepository;
import pe.sga.repository.ClienteRepository;
import pe.sga.repository.SedeRepository;
import pe.sga.repository.UsuarioClienteRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.impl.ReporteJasperServiceImpl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class ComprobanteFacturaPdfTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioClienteRepository usuarioClienteRepository;
    @Mock private SedeRepository sedeRepository;
    @Mock private CatalogoPuertoRepository puertoRepository;
    @Mock private CatalogoTipoCargaRepository tipoCargaRepository;
    @Mock private CatalogoMercanciaRepository mercanciaRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private ICotizacionService cotizacionService;

    private ReporteJasperServiceImpl buildService() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        return new ReporteJasperServiceImpl(
                usuarioRepository,
                usuarioClienteRepository,
                sedeRepository,
                puertoRepository,
                tipoCargaRepository,
                mercanciaRepository,
                clienteRepository,
                cotizacionService,
                templateEngine
        );
    }

    @Test
    void exportarComprobanteFacturaPdf_abonoAprobado_generaPdfValidoConDesglose() throws IOException {
        ReporteJasperServiceImpl service = buildService();
        ComprobanteFacturaResponseDTO factura = ComprobanteFacturaResponseDTO.builder()
                .idComprobante(1)
                .tipoComprobante("FACTURA")
                .serie("F001")
                .correlativo(1)
                .numeroCompleto("F001-000001")
                .origenTipo("ABONO_COTIZACION")
                .clienteRazonSocial("Importadora Pacífico S.A.C.")
                .clienteNumeroDocumento("20555666777")
                .clienteDireccion("Av. Argentina 1200, Callao")
                .codigoReferencia("COT-2026-0012 / PAG-COT-2026-0030")
                .concepto("Anticipo por servicios logísticos y aduaneros")
                .moneda("USD")
                .tipoCambio(new BigDecimal("3.7500"))
                .inafecto(BigDecimal.ZERO)
                .subtotalGravado(new BigDecimal("1000.00"))
                .igvMonto(new BigDecimal("180.00"))
                .totalComprobante(new BigDecimal("1180.00"))
                .totalEquivalentePen(new BigDecimal("4425.00"))
                .bancoDestino("BCP Dólares")
                .numeroOperacion("OP-778899")
                .estado("EMITIDA")
                .fechaEmision(LocalDateTime.of(2026, 11, 24, 21, 45))
                .build();

        ByteArrayInputStream stream = service.exportarComprobanteFacturaPdf(factura);
        byte[] bytes = stream.readAllBytes();
        String header = new String(bytes, 0, Math.min(5, bytes.length), StandardCharsets.US_ASCII);
        assertTrue(header.startsWith("%PDF-"));

        try (PDDocument doc = PDDocument.load(bytes)) {
            String text = new PDFTextStripper().getText(doc);
            assertTrue(text.contains("F001-000001"));
            assertTrue(text.contains("20555666777"));
            assertTrue(text.contains("1,180.00"));
            assertTrue(text.contains("OP-778899"));
        }
    }
}
