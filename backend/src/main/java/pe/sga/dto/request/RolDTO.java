package pe.sga.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import pe.sga.entity.enums.RolUsuario;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolDTO {

    @NotNull(message = "El nombre del rol es obligatorio")
    private RolUsuario nombreRol;

    @Size(max = 255, message = "La descripción no puede exceder 255 caracteres")
    private String descripcion;

    @Builder.Default
    private Boolean estadoActivo = true;
}
