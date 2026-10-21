package pe.sga.service;

import java.io.ByteArrayInputStream;

public interface IReporteJasperService {

    ByteArrayInputStream exportarUsuariosPdf();

    ByteArrayInputStream exportarSedesPdf();

    ByteArrayInputStream exportarPuertosPdf();
}
