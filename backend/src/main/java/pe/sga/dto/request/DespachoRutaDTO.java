package pe.sga.dto.request;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DespachoRutaDTO {

    private LocalDateTime fechaSalidaRuta;

    private String guiaRemisionTransporte;

    private String observaciones;
}
