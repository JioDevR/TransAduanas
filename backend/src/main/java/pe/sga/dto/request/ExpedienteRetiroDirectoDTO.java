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
public class ExpedienteRetiroDirectoDTO {

    @NotBlank(message = "El nombre del receptor en terminal es obligatorio")
    @Size(max = 120, message = "El nombre del receptor no puede exceder 120 caracteres")
    private String receptorNombre;

    @NotBlank(message = "El DNI/Documento del receptor es obligatorio")
    @Size(max = 20, message = "El documento del receptor no puede exceder 20 caracteres")
    private String receptorDni;

    private String terminalAlmacen;
    private String numeroActaRetiro;
    private LocalDateTime fechaRetiro;
    private String observaciones;
}
