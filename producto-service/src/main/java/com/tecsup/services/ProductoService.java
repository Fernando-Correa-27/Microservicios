package com.tecsup.services;

import com.tecsup.client.CategoriaClient;
import com.tecsup.dto.CategoriaDTO;
import com.tecsup.dto.ProductoDTO;
import com.tecsup.exception.ResourceNotFoundException;
import com.tecsup.model.Producto;
import com.tecsup.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProductoService {

    private final ProductoRepository repo;
    private final CategoriaClient categoriaClient;

    public ProductoService(ProductoRepository repo, CategoriaClient categoriaClient) {
        this.repo = repo;
        this.categoriaClient = categoriaClient;
    }

    /**
     * Completa la categoría de cada producto.
     * Si una categoría falla (servicio caído o dato borrado) se deja en null
     * y el listado sigue respondiendo.
     */
    @Transactional(readOnly = true)
    public List<Producto> listar() {
        List<Producto> productos = repo.findAll();
        productos.forEach(this::completarCategoriaTolerante);
        return productos;
    }

    @Transactional(readOnly = true)
    public Producto obtener(Long id) {
        Producto producto = repo.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        producto.setCategoria(categoriaClient.obtenerCategoria(producto.getCategoriaId()));
        return producto;
    }

    /**
     * IMPORTANTE (corrección a la guía): se valida la categoría ANTES de guardar.
     * Así, si no existe, no queda un producto huérfano en la base de datos.
     */
    @Transactional
    public Producto guardar(ProductoDTO dto) {
        CategoriaDTO categoria = categoriaClient.obtenerCategoria(dto.getCategoriaId());
        Producto producto = new Producto();
        aplicar(producto, dto);
        Producto guardado = repo.save(producto);
        guardado.setCategoria(categoria);
        return guardado;
    }

    @Transactional
    public Producto actualizar(Long id, ProductoDTO dto) {
        Producto existente = repo.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        CategoriaDTO categoria = categoriaClient.obtenerCategoria(dto.getCategoriaId());
        aplicar(existente, dto);
        Producto guardado = repo.save(existente);
        guardado.setCategoria(categoria);
        return guardado;
    }

    @Transactional
    public void eliminar(Long id) {
        Producto producto = repo.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
        repo.delete(producto);
    }

    private void completarCategoriaTolerante(Producto producto) {
        try {
            producto.setCategoria(categoriaClient.obtenerCategoria(producto.getCategoriaId()));
        } catch (RuntimeException ex) {
            producto.setCategoria(null);
        }
    }

    private void aplicar(Producto producto, ProductoDTO dto) {
        producto.setNombre(dto.getNombre());
        producto.setPrecio(dto.getPrecio());
        producto.setStock(dto.getStock());
        producto.setCategoriaId(dto.getCategoriaId());
    }
}
