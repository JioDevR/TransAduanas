package pe.sga.controller;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import pe.sga.dto.portal.PortalCotizacionDTO;
import pe.sga.entity.Cliente;
import pe.sga.entity.UsuarioCliente;
import pe.sga.exception.BadRequestException;
import pe.sga.exception.ResourceNotFoundException;
import pe.sga.service.IDocumentoService;
import pe.sga.service.IPortalService;
import pe.sga.service.IReporteJasperService;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PortalControllerPdfTest {

    private UsuarioCliente usuarioCliente() {
        return UsuarioCliente.builder()
                .idUsuarioCliente(13)
                .cliente(Cliente.builder().idCliente(11).build())
                .build();
    }

    @Test
    void descargarCotizacionPdf_inlinePorDefecto_retornaPdfConDisposicionInlineYNoStore() {
        IPortalService portalService = mock(IPortalService.class);
        IDocumentoService documentoService = mock(IDocumentoService.class);
        IReporteJasperService reporteService = mock(IReporteJasperService.class);
        PortalController controller = new PortalController(portalService, documentoService, reporteService);

        PortalCotizacionDTO dto = PortalCotizacionDTO.builder()
                .idCotizacion(7)
                .codigoCotizacion("COT-2026-0007")
                .build();
        byte[] pdfBytes = "%PDF-1.4 sample".getBytes(StandardCharsets.US_ASCII);

        when(portalService.buscarCotizacionPorId(11, 7)).thenReturn(dto);
        when(reporteService.exportarCotizacionPortalPdf(dto)).thenReturn(new ByteArrayInputStream(pdfBytes));

        ResponseEntity<InputStreamResource> response = controller.descargarCotizacionPdf(7, false, usuarioCliente());

        assertEquals(200, response.getStatusCode().value());
        assertEquals(MediaType.APPLICATION_PDF, response.getHeaders().getContentType());
        assertEquals("inline; filename=\"proforma-COT-2026-0007.pdf\"",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
        assertEquals("no-store", response.getHeaders().getCacheControl());
        assertNotNull(response.getBody());
        verify(portalService).buscarCotizacionPorId(11, 7);
        verify(reporteService).exportarCotizacionPortalPdf(dto);
    }

    @Test
    void descargarCotizacionPdf_conDownloadTrue_retornaAttachmentYNombreSanitizado() {
        IPortalService portalService = mock(IPortalService.class);
        IDocumentoService documentoService = mock(IDocumentoService.class);
        IReporteJasperService reporteService = mock(IReporteJasperService.class);
        PortalController controller = new PortalController(portalService, documentoService, reporteService);

        PortalCotizacionDTO dto = PortalCotizacionDTO.builder()
                .idCotizacion(8)
                .codigoCotizacion("COT/2026 0008")
                .build();
        when(portalService.buscarCotizacionPorId(11, 8)).thenReturn(dto);
        when(reporteService.exportarCotizacionPortalPdf(dto))
                .thenReturn(new ByteArrayInputStream("%PDF-1.4".getBytes(StandardCharsets.US_ASCII)));

        ResponseEntity<InputStreamResource> response = controller.descargarCotizacionPdf(8, true, usuarioCliente());

        assertEquals(200, response.getStatusCode().value());
        assertEquals("attachment; filename=\"proforma-COT-2026-0008.pdf\"",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void descargarCotizacionPdf_codigoNuloOPuntuacion_usaFallbackSeguro() {
        IPortalService portalService = mock(IPortalService.class);
        IDocumentoService documentoService = mock(IDocumentoService.class);
        IReporteJasperService reporteService = mock(IReporteJasperService.class);
        PortalController controller = new PortalController(portalService, documentoService, reporteService);

        PortalCotizacionDTO dto = PortalCotizacionDTO.builder()
                .idCotizacion(9)
                .codigoCotizacion("///")
                .build();
        when(portalService.buscarCotizacionPorId(11, 9)).thenReturn(dto);
        when(reporteService.exportarCotizacionPortalPdf(dto))
                .thenReturn(new ByteArrayInputStream("%PDF-1.4".getBytes(StandardCharsets.US_ASCII)));

        ResponseEntity<InputStreamResource> response = controller.descargarCotizacionPdf(9, true, usuarioCliente());

        assertEquals("attachment; filename=\"proforma-cotizacion-9.pdf\"",
                response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void descargarCotizacionPdf_erroresDeAutorizacionYDisponibilidad() {
        IPortalService portalService = mock(IPortalService.class);
        IDocumentoService documentoService = mock(IDocumentoService.class);
        IReporteJasperService reporteService = mock(IReporteJasperService.class);

        PortalController controllerWithoutPdf = new PortalController(portalService, documentoService);
        assertThrows(IllegalStateException.class,
                () -> controllerWithoutPdf.descargarCotizacionPdf(7, false, usuarioCliente()));

        PortalController controller = new PortalController(portalService, documentoService, reporteService);
        assertThrows(BadRequestException.class,
                () -> controller.descargarCotizacionPdf(7, false, null));

        UsuarioCliente sinCliente = UsuarioCliente.builder().idUsuarioCliente(13).build();
        assertThrows(BadRequestException.class,
                () -> controller.descargarCotizacionPdf(7, false, sinCliente));

        when(portalService.buscarCotizacionPorId(11, 404))
                .thenThrow(new ResourceNotFoundException("No pertenece al cliente"));
        assertThrows(ResourceNotFoundException.class,
                () -> controller.descargarCotizacionPdf(404, false, usuarioCliente()));
    }
}
