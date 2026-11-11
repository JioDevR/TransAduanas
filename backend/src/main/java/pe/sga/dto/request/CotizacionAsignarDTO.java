package pe.sga.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Solicitud para asignar o reclamar una cotización comercial sin asesor asignado.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CotizacionAsignarDTO {
    private Integer idUsuarioComercial;
}
