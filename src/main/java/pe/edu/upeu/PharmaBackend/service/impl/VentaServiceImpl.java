package pe.edu.upeu.PharmaBackend.service.impl;

import org.apache.commons.logging.Log;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pe.edu.upeu.PharmaBackend.dto.DetalleVentaRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.DetalleVentaResponseDTO;
import pe.edu.upeu.PharmaBackend.dto.VentaRequestDTO;
import pe.edu.upeu.PharmaBackend.dto.VentaResponseDTO;
import pe.edu.upeu.PharmaBackend.entity.Cliente;
import pe.edu.upeu.PharmaBackend.entity.DetalleVenta;
import pe.edu.upeu.PharmaBackend.entity.Producto;
import pe.edu.upeu.PharmaBackend.entity.Venta;
import pe.edu.upeu.PharmaBackend.enums.EstadoVenta;
import pe.edu.upeu.PharmaBackend.exception.RecursoNoEncontradoException;
import pe.edu.upeu.PharmaBackend.exception.ReglaNegocioException;
import pe.edu.upeu.PharmaBackend.repository.ClienteRepository;
import pe.edu.upeu.PharmaBackend.repository.ProductoRepository;
import pe.edu.upeu.PharmaBackend.repository.VentaRepository;
import pe.edu.upeu.PharmaBackend.service.service.VentaService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.slf4j.MDC;
import pe.edu.upeu.PharmaBackend.event.AuditoriaEvent;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

@Service
public class VentaServiceImpl implements VentaService {
    private static final Logger LOG = LoggerFactory.getLogger(VentaServiceImpl.class);
    private final VentaRepository ventaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public VentaServiceImpl(
            VentaRepository ventaRepository,
            ClienteRepository clienteRepository,
            ProductoRepository productoRepository,
            ApplicationEventPublisher eventPublisher) {

        this.ventaRepository = ventaRepository;
        this.clienteRepository = clienteRepository;
        this.productoRepository = productoRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "productos", allEntries = true)
    public VentaResponseDTO registrar(VentaRequestDTO request) {
        Cliente cliente = clienteRepository.findById(request.getClienteId())
                        .orElseThrow(() ->new RecursoNoEncontradoException("Cliente no encontrado con id: "+ request.getClienteId()));

        if (!Boolean.TRUE.equals(cliente.getEstado())) {
            throw new ReglaNegocioException("No se puede registrar una venta para un cliente inactivo");
        }
        Venta venta = new Venta();

        String usuarioActual = "vendedor";
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName())) {
            usuarioActual = auth.getName();
        }
        venta.setCreadoPor(usuarioActual);

        venta.setCliente(cliente);

        venta.setEstado(EstadoVenta.REGISTRADA);

        BigDecimal total = BigDecimal.ZERO;

        for (DetalleVentaRequestDTO item: request.getDetalles()) {
            Producto producto = productoRepository.findById(item.getProductoId()).orElseThrow(() ->
                                    new RecursoNoEncontradoException("Producto no encontrado con id: "+ item.getProductoId()));

            if (!Boolean.TRUE.equals(producto.getEstado())) {
                throw new ReglaNegocioException("El producto "+ producto.getNombre()+ " se encuentra inactivo");
            }

            if (producto.getStock()< item.getCantidad()) {

                throw new ReglaNegocioException("Stock insuficiente para "+ producto.getNombre()+ ". Disponible: "+ producto.getStock()
                                + ", solicitado: "+ item.getCantidad());
            }

            BigDecimal subtotal = producto.getPrecio().multiply(BigDecimal.valueOf(item.getCantidad()));

            DetalleVenta detalle = new DetalleVenta();

            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecio(producto.getPrecio());
            detalle.setSubtotal(subtotal);

            venta.agregarDetalle(detalle);

            total = total.add(subtotal);

            producto.setStock(producto.getStock()- item.getCantidad());
        }

        venta.setTotal(total);

        Venta guardada = ventaRepository.save(venta);

        String requestId = MDC.get("requestId");
        eventPublisher.publishEvent(new AuditoriaEvent(
                usuarioActual,
                "VENTA_REGISTRADA",
                "Venta",
                guardada.getId(),
                "Total: S/ " + guardada.getTotal() + ", Cliente: " + cliente.getNombres() + " " + cliente.getApellidos(),
                requestId
        ));

        return convertirResponse(guardada);
    }

    @Override
    @Transactional(readOnly = true)
    public VentaResponseDTO buscar(Long id) {
        long inicio = System.currentTimeMillis();
        LOG.info("Inicio buscar venta por id | id={}", id);

        Venta venta = ventaRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException(
                        "Venta no encontrada con id: " + id
                ));

        VentaResponseDTO response = convertirResponse(venta);
        LOG.info("Fin buscar venta por id | id={} | duracionMs={}", id, System.currentTimeMillis() - inicio);
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VentaResponseDTO> listar() {
        long inicio = System.currentTimeMillis();
        LOG.info("Inicio listar todas las ventas");

        List<VentaResponseDTO> resultado = ventaRepository.findAll().stream().map(this::convertirResponse).toList();

        LOG.info("Fin listar todas las ventas | filas={} | duracionMs={}", resultado.size(), System.currentTimeMillis() - inicio);
        return resultado;
    }

    private VentaResponseDTO convertirResponse(Venta venta) {

        List<DetalleVentaResponseDTO> detalles =
                venta.getDetalles()
                        .stream()
                        .map(detalle ->
                                new DetalleVentaResponseDTO(
                                        detalle.getProducto().getId(),
                                        detalle.getProducto().getNombre(),
                                        detalle.getCantidad(),
                                        detalle.getPrecio(),
                                        detalle.getSubtotal()
                                )
                        ).toList();

        String clienteNombre = venta.getCliente().getNombres()+ " "+ venta.getCliente().getApellidos();

        VentaResponseDTO resp = new VentaResponseDTO(
                venta.getId(),
                venta.getFechaRegistro(),
                venta.getCliente().getId(),
                clienteNombre,
                venta.getEstado().name(),
                venta.getTotal(),
                detalles
        );
        resp.setCreadoPor(venta.getCreadoPor());
        return resp;
    }

@Override
@Transactional(readOnly = true)
public List<VentaResponseDTO> buscar(
        Long clienteId,
        EstadoVenta estado,
        LocalDate desde,
        LocalDate hasta,
        String ordenarPor,
        String direccion) {

    long inicio = System.currentTimeMillis();
    LOG.info("Inicio buscar ventas | clienteId={} | estado={} | desde={} | hasta={} | ordenarPor={} | direccion={}",
            clienteId, estado, desde, hasta, ordenarPor, direccion);

    if (desde != null
            && hasta != null
            && desde.isAfter(hasta)) {

        throw new ReglaNegocioException(
                "El rango de fechas es inválido: 'desde' ("
                        + desde
                        + ") es posterior a 'hasta' ("
                        + hasta + ")");
    }

    Sort sort = construirSort(ordenarPor, direccion);

    LocalDateTime desdeHora = (desde == null)
            ? null
            : desde.atStartOfDay();

    LocalDateTime hastaHora = (hasta == null)
            ? null
            : hasta.atTime(LocalTime.MAX);

    List<VentaResponseDTO> resultado =
            ventaRepository
                    .buscar(clienteId, estado, desdeHora, hastaHora, sort)
                    .stream()
                    .map(this::convertirResponse)
                    .toList();

    LOG.info("Fin buscar ventas | clienteId={} | estado={} | desde={} | hasta={} | orden={} {} | filas={} | duracionMs={}",
            clienteId, estado, desde, hasta, ordenarPor, direccion,
            resultado.size(),
            System.currentTimeMillis() - inicio);

    return resultado;
}

private static final String ORDEN_POR_DEFECTO = "fecha";
    private static final Set<String> CAMPOS_ORDENABLES= Set.of("id" , "fecha", "total","estado");
private Sort construirSort(String ordenarPor, String direccion) {

    String campo = (ordenarPor == null || ordenarPor.isBlank())
            ? ORDEN_POR_DEFECTO
            : ordenarPor.trim();

    if (!CAMPOS_ORDENABLES.contains(campo)) {

        throw new ReglaNegocioException(
                "El campo de ordenamiento '"
                        + campo
                        + "' no está permitido. Campos válidos: "
                        + CAMPOS_ORDENABLES);
    }

    String sentido = (direccion == null || direccion.isBlank())
            ? "desc"
            : direccion.trim();

    if (!sentido.equalsIgnoreCase("asc")
            && !sentido.equalsIgnoreCase("desc")) {
        throw new ReglaNegocioException(
                "La dirección de ordenamiento '"
                        + sentido
                        + "' no está permitida. Valores válidos: asc, desc");
    }

    String propiedad = campo.equals("fecha") ? "fechaRegistro" : campo;

    return sentido.equalsIgnoreCase("asc")
            ? Sort.by(propiedad).ascending()
            : Sort.by(propiedad).descending();
}


}

