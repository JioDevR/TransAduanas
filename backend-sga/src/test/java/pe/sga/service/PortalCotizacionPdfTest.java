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
import pe.sga.dto.portal.PortalCotizacionDTO;
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
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class PortalCotizacionPdfTest {

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

    private String extractPdfText(byte[] bytes) throws IOException {
        try (PDDocument doc = PDDocument.load(bytes)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    @Test
    void exportarCotizacionPortalPdf_liquidadaConTransporte_generaPdfValidoSinGlifosFaltantes() throws IOException {
        ReporteJasperServiceImpl service = buildService();
        PortalCotizacionDTO cotizacion = PortalCotizacionDTO.builder()
                .idCotizacion(42)
                .codigoCotizacion("COT-2026-0042")
                .razonSocialCliente("LG Electronics Perú S.A.")
                .rucCliente("20511894520")
                .regimenAduanero("IMPORTACION_DEFINITIVA")
                .nombreMercancia("Electrodomésticos & Pantallas <LED>")
                .codigoMercancia("8528.72.00")
                .nombreTipoCarga("Contenedor FCL 40")
                .nombrePuertoEnvio("Shanghai (CNSHA)")
                .nombrePuertoDestino("Callao (PECLL)")
                .servicioTransporte(true)
                .origenDepartamento("CALLAO")
                .origenProvincia("Callao")
                .destinoDepartamento("LIMA")
                .destinoProvincia("Ate")
                .valorFob(new BigDecimal("45000.00"))
                .montoFlete(new BigDecimal("3200.00"))
                .montoSeguroEstimado(new BigDecimal("450.00"))
                .montoHonorarios(new BigDecimal("650.00"))
                .costoTransporteInterno(new BigDecimal("420.00"))
                .montoTributos(new BigDecimal("8750.00"))
                .totalCotizado(new BigDecimal("13470.00"))
                .totalAbonado(new BigDecimal("5000.00"))
                .saldoPendiente(new BigDecimal("8470.00"))
                .validezDias(30)
                .fechaEmision(LocalDate.of(2026, 11, 24))
                .idUsuarioComercial(7)
                .usuarioComercialNombre("Carlos Mendoza")
                .estado("APROBADA")
                .estadoPago("ADELANTO_APROBADO")
                .aceptadaPorCliente(true)
                .motivoObservacion("Observación histórica que no debe mostrarse cuando está aprobada")
                .observaciones("Incluye coordinación de volante en puerto.")
                .build();

        ByteArrayInputStream pdfStream = service.exportarCotizacionPortalPdf(cotizacion);
        byte[] bytes = pdfStream.readAllBytes();
        String header = new String(bytes, 0, Math.min(bytes.length, 5), StandardCharsets.US_ASCII);
        String text = extractPdfText(bytes);

        assertTrue(bytes.length > 1000, "La proforma PDF debe contener cuerpo válido");
        assertTrue("%PDF-".equals(header), "El archivo generado debe iniciar con cabecera %PDF-");
        assertFalse(text.contains("#"), "No deben existir glifos faltantes (#) en el texto PDF");
        assertTrue(text.contains("Shanghai (CNSHA) -> Callao (PECLL)"));
        assertTrue(text.contains("Callao -> Ate"));
        assertTrue(text.contains("$13,470.00"));
        assertTrue(text.contains("Electrodomésticos & Pantallas <LED>"));
        assertFalse(text.contains("Observación histórica que no debe mostrarse"));
    }

    @Test
    void exportarCotizacionPortalPdf_pendienteLiquidacionConCamposNulos_generaPdfValido() throws IOException {
        ReporteJasperServiceImpl service = buildService();
        PortalCotizacionDTO cotizacion = PortalCotizacionDTO.builder()
                .idCotizacion(99)
                .codigoCotizacion("COT-2026-0099")
                .regimenAduanero("EXPORTACION_DEFINITIVA")
                .estado("PENDIENTE")
                .estadoPago("SIN_PAGO")
                .valorFob(new BigDecimal("12000.00"))
                .totalCotizado(BigDecimal.ZERO)
                .build();

        ByteArrayInputStream pdfStream = service.exportarCotizacionPortalPdf(cotizacion);
        byte[] bytes = pdfStream.readAllBytes();
        String header = new String(bytes, 0, Math.min(bytes.length, 5), StandardCharsets.US_ASCII);
        String text = extractPdfText(bytes);

        assertTrue(bytes.length > 1000, "La proforma PDF pendiente debe generarse sin errores");
        assertTrue("%PDF-".equals(header));
        assertTrue(text.contains("Inafecto ($0.00)"));
        assertTrue(text.contains("Por asesor"));
        assertTrue(text.contains("En liquidación ($0.00)"));
        assertFalse(text.contains("Flete Terrestre Local (Flota SGA)"));
    }

    @Test
    void exportarCotizacionPortalPdf_sanitizaCaracteresDeControlInvalidosParaXml() throws IOException {
        ReporteJasperServiceImpl service = buildService();
        PortalCotizacionDTO cotizacion = PortalCotizacionDTO.builder()
                .idCotizacion(105)
                .codigoCotizacion("COT-2026-0105")
                .nombreMercancia("Carga\u0000 especial\u0007")
                .estado("OBSERVADA")
                .motivoObservacion("Falta\u000B detalle\u001F técnico")
                .observaciones("Nota\u0001 limpia")
                .build();

        ByteArrayInputStream pdfStream = service.exportarCotizacionPortalPdf(cotizacion);
        byte[] bytes = pdfStream.readAllBytes();
        String text = extractPdfText(bytes);

        assertTrue(text.contains("Carga especial"));
        assertTrue(text.contains("Falta detalle técnico"));
        assertTrue(text.contains("Nota limpia"));
    }

    @Test
    void exportarCotizacionPortalPdf_nula_lanzaIllegalArgumentException() {
        ReporteJasperServiceImpl service = buildService();
        assertThrows(IllegalArgumentException.class, () -> service.exportarCotizacionPortalPdf(null));
    }
}
