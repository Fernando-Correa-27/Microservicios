package com.tecsup.services;

import com.tecsup.dto.CategoriaDTO;
import com.tecsup.exception.ResourceNotFoundException;
import com.tecsup.model.Categoria;
import com.tecsup.repository.CategoriaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository repo;

    public CategoriaService(CategoriaRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<Categoria> listar() {
        return repo.findAll();
    }

    @Transactional
    public Categoria guardar(Categoria categoria) {
        return repo.save(categoria);
    }

    @Transactional(readOnly = true)
    public Categoria obtener(Long id) {
        return repo.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Categoría no encontrada con id: " + id));
    }

    @Transactional
    public Categoria actualizar(Long id, CategoriaDTO dto) {
        Categoria existente = obtener(id);
        existente.setNombre(dto.getNombre());
        existente.setEstado(dto.getEstado());
        return repo.save(existente);
    }

    @Transactional
    public void eliminar(Long id) {
        Categoria categoria = obtener(id);
        repo.delete(categoria);
    }
}
