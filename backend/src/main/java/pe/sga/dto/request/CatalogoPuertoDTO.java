package pe.sga.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CatalogoPuertoDTO {

    @NotBlank(message = "El código del puerto es obligatorio")
    @Size(max = 20, message = "El código no puede exceder 20 caracteres")
    private String codigoPuerto;

    @NotBlank(message = "El nombre del puerto es obligatorio")
    @Size(max = 150, message = "El nombre no puede exceder 150 caracteres")
    private String nombre;

    @NotBlank(message = "El país es obligatorio")
    @Size(max = 50, message = "El país no puede exceder 50 caracteres")
    private String pais;

    @Builder.Default
    private String tipo = "MARITIMO";

    @Builder.Default
    private String estado = "ACTIVO";
}
