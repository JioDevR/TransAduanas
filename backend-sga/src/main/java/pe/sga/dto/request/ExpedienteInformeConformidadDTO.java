package pe.sga.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteInformeConformidadDTO {

    @NotBlank(message = "El nombre del receptor o cliente conforme es obligatorio")
    @Size(max = 150)
    private String receptorNombre;

    @Size(max = 30)
    private String receptorDni;

    @NotBlank(message = "La redacción del informe de conformidad de entrega es obligatoria")
    @Size(max = 2000)
    private String detalleConformidad;

    private LocalDateTime fechaEntrega;

    private Integer idDocumentoConformidad;
}
