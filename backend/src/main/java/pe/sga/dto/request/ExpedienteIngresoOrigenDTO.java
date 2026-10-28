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
public class ExpedienteIngresoOrigenDTO {

    @NotBlank(message = "El terminal o almacén temporal de origen es obligatorio")
    @Size(max = 200, message = "El terminal no puede exceder 200 caracteres")
    private String terminalAlmacenOrigen;

    @Size(max = 50, message = "El ticket de ingreso no puede exceder 50 caracteres")
    private String numeroTicketIngreso;

    @Size(max = 30, message = "El contenedor no puede exceder 30 caracteres")
    private String numeroContenedor;

    @Size(max = 50, message = "El precinto aduanero no puede exceder 50 caracteres")
    private String precintoAduanero;

    private LocalDateTime fechaIngreso;

    private String observaciones;
}
