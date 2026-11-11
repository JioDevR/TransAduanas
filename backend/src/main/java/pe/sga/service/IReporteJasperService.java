package pe.sga.service;

import java.io.ByteArrayInputStream;

public interface IReporteJasperService {

    ByteArrayInputStream exportarUsuariosPdf();

    ByteArrayInputStream exportarUsuariosPdf(boolean soloActivos);

    ByteArrayInputStream exportarUsuariosPortalPdf();

    ByteArrayInputStream exportarUsuariosPortalPdf(boolean soloActivos);

    ByteArrayInputStream exportarSedesPdf();

    ByteArrayInputStream exportarPuertosPdf();

    ByteArrayInputStream exportarTiposCargaPdf();

    ByteArrayInputStream exportarMercanciasPdf();
}
