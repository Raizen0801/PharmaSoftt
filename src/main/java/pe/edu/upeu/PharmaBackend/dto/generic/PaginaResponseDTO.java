package pe.edu.upeu.PharmaBackend.dto.generic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PaginaResponseDTO<T> implements Serializable {
    private static final long serialVersionUID = 1L;

    private List<T> contenido;
    private int pagina;
    private int tamanio;
    private long totalElementos;
    private int totalPaginas;
    private boolean esPrimera;
    private boolean esUltima;

    public static <T> PaginaResponseDTO<T> of(Page<T> page) {
        return new PaginaResponseDTO<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
