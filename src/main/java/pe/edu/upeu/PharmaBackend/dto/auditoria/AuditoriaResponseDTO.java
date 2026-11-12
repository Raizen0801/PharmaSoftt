package pe.edu.upeu.PharmaBackend.dto.auditoria;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class AuditoriaResponseDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long id;
    private LocalDateTime fecha;
    private String usuario;
    private String accion;
    private String entidad;
    private Long entidadId;
    private String detalle;
    private String requestId;
}
