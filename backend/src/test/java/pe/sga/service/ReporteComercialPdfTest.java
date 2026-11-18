package pe.sga.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import pe.sga.dto.response.CotizacionResponseDTO;
import pe.sga.entity.Cliente;
import pe.sga.entity.Usuario;
import pe.sga.entity.enums.EstadoCotizacion;
import pe.sga.entity.enums.RolUsuario;
import pe.sga.repository.CatalogoMercanciaRepository;
import pe.sga.repository.CatalogoPuertoRepository;
import pe.sga.repository.CatalogoTipoCargaRepository;
import pe.sga.repository.ClienteRepository;
import pe.sga.repository.SedeRepository;
import pe.sga.repository.UsuarioClienteRepository;
import pe.sga.repository.UsuarioRepository;
import pe.sga.service.impl.ReporteJasperServiceImpl;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteComercialPdfTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private UsuarioClienteRepository usuarioClienteRepository;
    @Mock private SedeRepository sedeRepository;
    @Mock private CatalogoPuertoRepository puertoRepository;
    @Mock private CatalogoTipoCargaRepository tipoCargaRepository;
    @Mock private CatalogoMercanciaRepository mercanciaRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private ICotizacionService cotizacionService;

    @Test
    void exportarReporteComercialPdf_generaPdfValidoConMetricas() {
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");

        SpringTemplateEngine templateEngine = new SpringTemplateEngine();
        templateEngine.setTemplateResolver(resolver);

        ReporteJasperServiceImpl service = new ReporteJasperServiceImpl(
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

        Usuario usuarioActual = Usuario.builder()
                .idUsuario(1)
                .username("jefe.comercial")
                .nombres("Ana")
                .apellidos("Torres")
                .rol(RolUsuario.GERENTE_GENERAL)
                .build();

        CotizacionResponseDTO c1 = CotizacionResponseDTO.builder()
                .idCotizacion(10)
                .codigoCotizacion("COT-2026-0010")
                .clienteRazonSocial("Importadora Pacífico S.A.C.")
                .clienteNumeroDocumento("20512345678")
                .usuarioComercialNombre("Carlos Mendoza")
                .regimenAduanero("IMPORTACION_DEFINITIVA")
                .fechaEmision(LocalDate.of(2026, 11, 10))
                .totalCotizado(new BigDecimal("4500.00"))
                .totalAbonado(new BigDecimal("2000.00"))
                .saldoPendiente(new BigDecimal("2500.00"))
                .estado(EstadoCotizacion.APROBADA)
                .build();

        CotizacionResponseDTO c2 = CotizacionResponseDTO.builder()
                .idCotizacion(11)
                .codigoCotizacion("COT-2026-0011")
                .clienteRazonSocial("AgroExport Sur E.I.R.L.")
                .clienteNumeroDocumento("20698765432")
                .usuarioComercialNombre("Carlos Mendoza")
                .regimenAduanero("EXPORTACION_DEFINITIVA")
                .fechaEmision(LocalDate.of(2026, 11, 15))
                .totalCotizado(new BigDecimal("3200.00"))
                .totalAbonado(BigDecimal.ZERO)
                .saldoPendiente(new BigDecimal("3200.00"))
                .estado(EstadoCotizacion.EN_REVISION)
                .build();

        when(cotizacionService.listar(false, null, null, null, usuarioActual)).thenReturn(List.of(c1, c2));
        when(cotizacionService.listarSinAsignar()).thenReturn(List.of());
        when(clienteRepository.findAll()).thenReturn(List.of(
                Cliente.builder().idCliente(1).estadoCliente("CLIENTE").build(),
                Cliente.builder().idCliente(2).estadoCliente("PROSPECTO").build()
        ));

        ByteArrayInputStream pdfStream = service.exportarReporteComercialPdf(
                LocalDate.of(2026, 11, 1),
                LocalDate.of(2026, 11, 30),
                null,
                null,
                usuarioActual
        );

        byte[] bytes = pdfStream.readAllBytes();
        String header = new String(bytes, 0, Math.min(bytes.length, 5), StandardCharsets.US_ASCII);
        assertTrue(bytes.length > 500, "El PDF generado debe tener contenido");
        assertTrue("%PDF-".equals(header), "El archivo generado debe iniciar con cabecera %PDF-");
    }
}
