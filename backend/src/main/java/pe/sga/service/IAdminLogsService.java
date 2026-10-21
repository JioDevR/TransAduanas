package pe.sga.service;

import org.springframework.core.io.Resource;

import java.util.Map;

public interface IAdminLogsService {

    /**
     * Obtiene las últimas N líneas del archivo de log especificado ('all' o 'error').
     *
     * @param tipo     Tipo de log ('all' para sga-backend.log, 'error' para sga-backend-error.log)
     * @param maxLines Cantidad máxima de líneas recientes a retornar
     * @return Mapa con metadatos del archivo y lista de líneas de log
     */
    Map<String, Object> getLogs(String tipo, int maxLines);

    /**
     * Retorna el archivo de log completo como recurso descargable.
     *
     * @param tipo Tipo de log ('all' o 'error')
     * @return Resource descargable
     */
    Resource getLogFileResource(String tipo);

    /**
     * Trunca o vacía el archivo de log especificado.
     *
     * @param tipo Tipo de log ('all' o 'error')
     */
    void clearLogFile(String tipo);
}
