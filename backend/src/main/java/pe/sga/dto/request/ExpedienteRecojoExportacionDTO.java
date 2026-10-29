package pe.sga.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteRecojoExportacionDTO {

    @NotBlank(message = "La dirección de recojo en planta es obligatoria")
    @Size(max = 255, message = "La dirección de recojo no puede exceder 255 caracteres")
    private String direccionRecojoPlanta;

    @NotBlank(message = "El distrito de recojo es obligatorio")
    @Size(max = 100, message = "El distrito de recojo no puede exceder 100 caracteres")
    private String distritoRecojo;

    @Size(max = 100, message = "El contacto en planta no puede exceder 100 caracteres")
    private String contactoPlanta;

    @NotBlank(message = "El almacén o terminal de destino es obligatorio")
    @Size(max = 255, message = "El almacén de destino no puede exceder 255 caracteres")
    private String almacenDestinoTerminal;

    @NotNull(message = "La fecha programada de recojo es obligatoria")
    private LocalDateTime fechaProgramadaRecojo;

    private String observaciones;
}
