package com.tecsup.services;

import com.tecsup.client.CategoriaClient;
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

    @Transactional(readOnly = true)
    public List<Producto> listar() {
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public Producto obtener(Long id) {
        return repo.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Producto no encontrado con id: " + id));
    }

    @Transactional
    public Producto guardar(ProductoDTO dto) {
        categoriaClient.validarCategoria(dto.getCategoriaId());
        Producto producto = new Producto();
        aplicar(producto, dto);
        return repo.save(producto);
    }

    @Transactional
    public Producto actualizar(Long id, ProductoDTO dto) {
        Producto existente = obtener(id);
        categoriaClient.validarCategoria(dto.getCategoriaId());
        aplicar(existente, dto);
        return repo.save(existente);
    }

    @Transactional
    public void eliminar(Long id) {
        repo.delete(obtener(id));
    }

    private void aplicar(Producto producto, ProductoDTO dto) {
        producto.setNombre(dto.getNombre());
        producto.setPrecio(dto.getPrecio());
        producto.setStock(dto.getStock());
        producto.setCategoriaId(dto.getCategoriaId());
    }
}
