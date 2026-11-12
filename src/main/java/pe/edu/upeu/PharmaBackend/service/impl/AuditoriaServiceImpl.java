package pe.edu.upeu.PharmaBackend.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import pe.edu.upeu.PharmaBackend.dto.auditoria.AuditoriaResponseDTO;
import pe.edu.upeu.PharmaBackend.dto.generic.PaginaResponseDTO;
import pe.edu.upeu.PharmaBackend.entity.Auditoria;
import pe.edu.upeu.PharmaBackend.event.AuditoriaEvent;
import pe.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import pe.edu.upeu.PharmaBackend.repository.AuditoriaRepository;
import pe.edu.upeu.PharmaBackend.service.service.AuditoriaService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class AuditoriaServiceImpl implements AuditoriaService {

    private static final Logger log = LoggerFactory.getLogger(AuditoriaServiceImpl.class);

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaServiceImpl(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onAuditoriaEvent(AuditoriaEvent event) {
        log.info("Registrando evento en bitácora post-commit: accion={} entidad={} id={} usuario={} requestId={}",
                event.getAccion(), event.getEntidad(), event.getEntidadId(), event.getUsuario(), event.getRequestId());

        Auditoria auditoria = new Auditoria();
        auditoria.setFecha(LocalDateTime.now());
        auditoria.setUsuario(event.getUsuario() != null ? event.getUsuario() : "SISTEMA");
        auditoria.setAccion(event.getAccion());
        auditoria.setEntidad(event.getEntidad());
        auditoria.setEntidadId(event.getEntidadId());
        auditoria.setDetalle(event.getDetalle());
        auditoria.setRequestId(event.getRequestId());

        auditoriaRepository.save(auditoria);
    }

    @Override
    @Transactional
    public void registrar(String usuario, String accion, String entidad, Long entidadId, String detalle, String requestId) {
        Auditoria auditoria = new Auditoria();
        auditoria.setFecha(LocalDateTime.now());
        auditoria.setUsuario(usuario != null ? usuario : "SISTEMA");
        auditoria.setAccion(accion);
        auditoria.setEntidad(entidad);
        auditoria.setEntidadId(entidadId);
        auditoria.setDetalle(detalle);
        auditoria.setRequestId(requestId);

        auditoriaRepository.save(auditoria);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResponseDTO<AuditoriaResponseDTO> buscar(
            String usuario,
            String accion,
            LocalDate desde,
            LocalDate hasta,
            Pageable pageable) {

        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new ReglaNegocioException("El rango de fechas es inválido: 'desde' (" + desde + ") es posterior a 'hasta' (" + hasta + ")");
        }

        LocalDateTime desdeHora = (desde != null) ? desde.atStartOfDay() : null;
        LocalDateTime hastaHora = (hasta != null) ? hasta.atTime(LocalTime.MAX) : null;

        String filtroUsuario = (usuario != null && !usuario.isBlank()) ? usuario.trim() : null;
        String filtroAccion = (accion != null && !accion.isBlank()) ? accion.trim() : null;

        Page<Auditoria> page = auditoriaRepository.buscar(filtroUsuario, filtroAccion, desdeHora, hastaHora, pageable);

        Page<AuditoriaResponseDTO> dtoPage = page.map(this::toDTO);
        return PaginaResponseDTO.of(dtoPage);
    }

    private AuditoriaResponseDTO toDTO(Auditoria a) {
        return new AuditoriaResponseDTO(
                a.getId(),
                a.getFecha(),
                a.getUsuario(),
                a.getAccion(),
                a.getEntidad(),
                a.getEntidadId(),
                a.getDetalle(),
                a.getRequestId()
        );
    }
}
