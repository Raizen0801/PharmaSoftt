package pe.edu.upeu.PharmaBackend.controller;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.upeu.PharmaBackend.dto.auditoria.AuditoriaResponseDTO;
import pe.edu.upeu.PharmaBackend.dto.generic.PaginaResponseDTO;
import pe.edu.upeu.PharmaBackend.service.service.AuditoriaService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/auditoria")
@PreAuthorize("hasAuthority('USUARIOS_ADMINISTRAR')")
public class AuditoriaController {

    private final AuditoriaService auditoriaService;

    public AuditoriaController(AuditoriaService auditoriaService) {
        this.auditoriaService = auditoriaService;
    }

    @GetMapping
    public ResponseEntity<PaginaResponseDTO<AuditoriaResponseDTO>> listar(
            @RequestParam(required = false) String usuario,
            @RequestParam(required = false) String accion,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "fecha") String ordenarPor,
            @RequestParam(defaultValue = "desc") String direccion) {

        Sort sort = direccion.equalsIgnoreCase("asc")
                ? Sort.by(ordenarPor).ascending()
                : Sort.by(ordenarPor).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        return ResponseEntity.ok(
                auditoriaService.buscar(usuario, accion, desde, hasta, pageable)
        );
    }
}
