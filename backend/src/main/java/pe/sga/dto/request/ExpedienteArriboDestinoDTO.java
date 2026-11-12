package pe.sga.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteArriboDestinoDTO {

    @NotNull(message = "La fecha y hora de arribo a aduana de destino es obligatoria")
    private LocalDateTime fechaArriboAduana;

    @Size(max = 100, message = "El terminal o almacén de destino no puede exceder 100 caracteres")
    private String terminalAlmacenDestino;

    private String observaciones;
}
