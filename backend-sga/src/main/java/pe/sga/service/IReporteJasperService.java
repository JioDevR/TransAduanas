package pe.sga.service;

import pe.sga.entity.Usuario;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;

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
}
