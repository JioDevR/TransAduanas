package pe.sga.service;

import pe.sga.dto.portal.PortalCotizacionDTO;
import pe.sga.entity.Usuario;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface IReporteJasperService {

    ByteArrayInputStream exportarUsuariosPdf();

    ByteArrayInputStream exportarUsuariosPdf(boolean soloActivos);

    ByteArrayInputStream exportarUsuariosPortalPdf();

    ByteArrayInputStream exportarUsuariosPortalPdf(boolean soloActivos);

    ByteArrayInputStream exportarSedesPdf();

    ByteArrayInputStream exportarPuertosPdf();

    ByteArrayInputStream exportarTiposCargaPdf();

    ByteArrayInputStream exportarMercanciasPdf();

    ByteArrayInputStream exportarReporteComercialPdf(
            LocalDate desde,
            LocalDate hasta,
            Integer idUsuarioComercial,
            String regimen,
            Usuario usuarioActual
    );

    ByteArrayInputStream exportarAuditoriaTiPdf(
            String buscar,
            String modulo,
            String resultado,
            LocalDateTime desde,
            LocalDateTime hasta
    );

    ByteArrayInputStream exportarSistemaTiPdf(int dias);

    ByteArrayInputStream exportarCotizacionPortalPdf(PortalCotizacionDTO cotizacion);
}
