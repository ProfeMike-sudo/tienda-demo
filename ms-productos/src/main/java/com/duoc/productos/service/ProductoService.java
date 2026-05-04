package com.duoc.productos.service;

import com.duoc.productos.dto.ProductoCreateDTO;
import com.duoc.productos.dto.ProductoDTO;
import com.duoc.productos.exception.RecursoNoEncontradoException;
import com.duoc.productos.model.Producto;
import com.duoc.productos.repository.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductoService {

    private static final Logger log = LoggerFactory.getLogger(ProductoService.class);

    @Autowired
    private ProductoRepository repository;

    public List<ProductoDTO> findAll() {
        log.info("Consultando todos los productos");
        return repository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public ProductoDTO findById(Long id) {
        log.info("Buscando producto id={}", id);
        Producto p = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + id));
        log.info("Producto encontrado: nombre={}, precio={}", p.getNombre(), p.getPrecio());
        return toDTO(p);
    }

    public ProductoDTO crear(ProductoCreateDTO dto) {
        log.info("Creando producto nombre={}", dto.getNombre());
        Producto p = new Producto();
        p.setNombre(dto.getNombre());
        p.setDescripcion(dto.getDescripcion());
        p.setPrecio(dto.getPrecio());
        p.setStock(dto.getStock());
        Producto guardado = repository.save(p);
        log.info("Producto creado id={}", guardado.getId());
        return toDTO(guardado);
    }

    public ProductoDTO actualizar(Long id, ProductoCreateDTO dto) {
        log.info("Actualizando producto id={}", id);
        Producto p = repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Producto no encontrado: " + id));
        p.setNombre(dto.getNombre());
        p.setDescripcion(dto.getDescripcion());
        p.setPrecio(dto.getPrecio());
        p.setStock(dto.getStock());
        return toDTO(repository.save(p));
    }

    public void eliminar(Long id) {
        log.info("Eliminando producto id={}", id);
        if (!repository.existsById(id)) {
            throw new RecursoNoEncontradoException("Producto no encontrado: " + id);
        }
        repository.deleteById(id);
        log.info("Producto id={} eliminado", id);
    }

    private ProductoDTO toDTO(Producto p) {
        return new ProductoDTO(
                p.getId(),
                p.getNombre(),
                p.getDescripcion(),
                p.getPrecio(),
                p.getStock()
        );
    }
}