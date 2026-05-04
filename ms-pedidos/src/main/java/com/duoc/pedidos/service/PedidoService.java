package com.duoc.pedidos.service;

import com.duoc.pedidos.client.ProductoClient;
import com.duoc.pedidos.dto.PedidoCreateDTO;
import com.duoc.pedidos.dto.PedidoDTO;
import com.duoc.pedidos.dto.ProductoDTO;
import com.duoc.pedidos.exception.RecursoNoEncontradoException;
import com.duoc.pedidos.exception.ServicioNoDisponibleException;
import com.duoc.pedidos.model.Pedido;
import com.duoc.pedidos.repository.PedidoRepository;
import feign.FeignException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    @Autowired
    private PedidoRepository repository;

    @Autowired
    private ProductoClient productoClient;

    public List<PedidoDTO> findAll() {
        log.info("Consultando todos los pedidos");
        return repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public PedidoDTO findById(Long id) {
        log.info("Buscando pedido id={}", id);
        Pedido pedido = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado: " + id));
        return toDTO(pedido);
    }

    public PedidoDTO crear(PedidoCreateDTO dto) {
        log.info("Creando pedido: productoId={}, cantidad={}", dto.getProductoId(), dto.getCantidad());

        ProductoDTO producto;
        try {
            log.info("Consultando ms-productos para productoId={}", dto.getProductoId());
            producto = productoClient.obtenerProducto(dto.getProductoId());
            log.info("Producto recibido: nombre={}, precio={}", producto.getNombre(), producto.getPrecio());
        } catch (FeignException.NotFound e) {
            throw new RecursoNoEncontradoException("Producto no encontrado en el catalogo: " + dto.getProductoId());
        } catch (FeignException e) {
            log.error("Error al contactar ms-productos: {}", e.getMessage());
            throw new ServicioNoDisponibleException("Servicio de productos no disponible. Intente nuevamente.");
        }

        BigDecimal total = producto.getPrecio().multiply(BigDecimal.valueOf(dto.getCantidad()));
        log.info("Total calculado: {}", total);

        Pedido pedido = new Pedido();
        pedido.setProductoId(dto.getProductoId());
        pedido.setCantidad(dto.getCantidad());
        pedido.setPrecioUnitario(producto.getPrecio());
        pedido.setTotal(total);
        pedido.setEstado("PENDIENTE");
        pedido.setFechaCreacion(LocalDateTime.now());

        Pedido guardado = repository.save(pedido);
        log.info("Pedido creado id={} — producto: {} — total: {}", guardado.getId(), producto.getNombre(), total);

        return toDTOConNombre(guardado, producto.getNombre());
    }

    public PedidoDTO cancelar(Long id) {
        log.info("Cancelando pedido id={}", id);
        Pedido pedido = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Pedido no encontrado: " + id));
        pedido.setEstado("CANCELADO");
        return toDTO(repository.save(pedido));
    }

    private PedidoDTO toDTO(Pedido p) {
        return new PedidoDTO(
                p.getId(), p.getProductoId(), null,
                p.getCantidad(), p.getPrecioUnitario(), p.getTotal(),
                p.getEstado(), p.getFechaCreacion()
        );
    }

    private PedidoDTO toDTOConNombre(Pedido p, String nombreProducto) {
        return new PedidoDTO(
                p.getId(), p.getProductoId(), nombreProducto,
                p.getCantidad(), p.getPrecioUnitario(), p.getTotal(),
                p.getEstado(), p.getFechaCreacion()
        );
    }
}