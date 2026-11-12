package pe.edu.upeu.PharmaBackend.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pe.edu.upeu.PharmaBackend.entity.Auditoria;

import java.time.LocalDateTime;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {

    @Query("""
            SELECT a FROM Auditoria a
            WHERE (:usuario IS NULL OR LOWER(a.usuario) LIKE LOWER(CONCAT('%', :usuario, '%')))
              AND (:accion IS NULL OR a.accion = :accion)
              AND (:desde IS NULL OR a.fecha >= :desde)
              AND (:hasta IS NULL OR a.fecha <= :hasta)
            """)
    Page<Auditoria> buscar(
            @Param("usuario") String usuario,
            @Param("accion") String accion,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
    );
}
