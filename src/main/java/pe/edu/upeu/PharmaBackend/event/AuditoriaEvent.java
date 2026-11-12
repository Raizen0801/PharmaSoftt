package pe.edu.upeu.PharmaBackend.event;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaEvent {
    private String usuario;
    private String accion;
    private String entidad;
    private Long entidadId;
    private String detalle;
    private String requestId;
}
