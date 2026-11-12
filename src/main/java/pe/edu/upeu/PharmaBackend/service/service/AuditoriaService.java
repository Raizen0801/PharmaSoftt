package pe.edu.upeu.PharmaBackend.service.service;

import org.springframework.data.domain.Pageable;
import pe.edu.upeu.PharmaBackend.dto.auditoria.AuditoriaResponseDTO;
import pe.edu.upeu.PharmaBackend.dto.generic.PaginaResponseDTO;

import java.time.LocalDate;

public interface AuditoriaService {

    PaginaResponseDTO<AuditoriaResponseDTO> buscar(
            String usuario,
            String accion,
            LocalDate desde,
            LocalDate hasta,
            Pageable pageable
    );

    void registrar(String usuario, String accion, String entidad, Long entidadId, String detalle, String requestId);
}
