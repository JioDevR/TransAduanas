package pe.sga.dto.request;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ControlAlmacenSalidaDTO {

    private LocalDateTime fechaSalida;

    private String observaciones;
}
