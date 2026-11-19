package pe.sga.dto.request;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExpedienteLevanteDTO {

    private String detalleInspeccion;

    private LocalDateTime fechaLevanteAutorizacion;

    private String observaciones;
}
