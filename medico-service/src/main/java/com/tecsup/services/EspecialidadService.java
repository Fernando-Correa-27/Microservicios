package com.tecsup.services;

import com.tecsup.dto.EspecialidadDTO;
import com.tecsup.exception.ResourceNotFoundException;
import com.tecsup.model.Especialidad;
import com.tecsup.repository.EspecialidadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EspecialidadService {

    private final EspecialidadRepository repo;

    public EspecialidadService(EspecialidadRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<Especialidad> listar() {
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public Especialidad obtener(Integer id) {
        return repo.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Especialidad no encontrada con id: " + id));
    }

    @Transactional
    public Especialidad guardar(EspecialidadDTO dto) {
        Especialidad especialidad = new Especialidad();
        aplicar(especialidad, dto);
        return repo.save(especialidad);
    }

    @Transactional
    public Especialidad actualizar(Integer id, EspecialidadDTO dto) {
        Especialidad existente = obtener(id);
        aplicar(existente, dto);
        return repo.save(existente);
    }

    @Transactional
    public void eliminar(Integer id) {
        repo.delete(obtener(id));
    }

    private void aplicar(Especialidad especialidad, EspecialidadDTO dto) {
        especialidad.setNombre(dto.getNombre());
        especialidad.setDescripcion(dto.getDescripcion());
        especialidad.setEstado(dto.getEstado() == null ? "A" : dto.getEstado());
    }
}
