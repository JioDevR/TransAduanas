package pe.sga.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DespachoEntregaDTO {

    private LocalDateTime fechaEntregaConforme;

    @Size(max = 120, message = "El nombre del receptor no puede exceder 120 caracteres")
    private String receptorNombre;

    @Size(max = 15, message = "El DNI del receptor no puede exceder 15 caracteres")
    private String receptorDni;

    @Size(max = 100, message = "El contacto de recepción no puede exceder 100 caracteres")
    private String contactoRecepcion;

    private Integer idDocumentoRemision;

    private String detalleIncidencia;

    private String observaciones;
}
