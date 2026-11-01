package com.tecsup.services;

import com.tecsup.dto.PacienteDTO;
import com.tecsup.exception.DuplicateResourceException;
import com.tecsup.exception.ResourceNotFoundException;
import com.tecsup.model.Paciente;
import com.tecsup.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PacienteService {

    private final PacienteRepository repo;

    public PacienteService(PacienteRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<Paciente> listar() {
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public Paciente obtener(Integer id) {
        return repo.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Paciente no encontrado con id: " + id));
    }

    @Transactional
    public Paciente guardar(PacienteDTO dto) {
        if (repo.existsByDni(dto.getDni())) {
            throw new DuplicateResourceException("Ya existe un paciente con DNI: " + dto.getDni());
        }
        Paciente paciente = new Paciente();
        aplicar(paciente, dto);
        return repo.save(paciente);
    }

    @Transactional
    public Paciente actualizar(Integer id, PacienteDTO dto) {
        Paciente existente = obtener(id);
        if (repo.existsByDniAndIdPacienteNot(dto.getDni(), id)) {
            throw new DuplicateResourceException("Ya existe un paciente con DNI: " + dto.getDni());
        }
        aplicar(existente, dto);
        return repo.save(existente);
    }

    @Transactional
    public void eliminar(Integer id) {
        repo.delete(obtener(id));
    }

    private void aplicar(Paciente paciente, PacienteDTO dto) {
        paciente.setDni(dto.getDni());
        paciente.setNombres(dto.getNombres());
        paciente.setApellidos(dto.getApellidos());
        paciente.setFechaNacimiento(dto.getFechaNacimiento());
        paciente.setSexo(dto.getSexo());
        paciente.setTelefono(dto.getTelefono());
        paciente.setDireccion(dto.getDireccion());
        paciente.setCorreo(dto.getCorreo());
        paciente.setEstado(dto.getEstado() == null ? "A" : dto.getEstado());
    }
}
