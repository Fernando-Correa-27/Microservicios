package com.tecsup.services;

import com.tecsup.dto.MedicoDTO;
import com.tecsup.exception.DuplicateResourceException;
import com.tecsup.exception.ResourceNotFoundException;
import com.tecsup.model.Especialidad;
import com.tecsup.model.Medico;
import com.tecsup.repository.EspecialidadRepository;
import com.tecsup.repository.HorarioMedicoRepository;
import com.tecsup.repository.MedicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MedicoService {

    private final MedicoRepository repo;
    private final EspecialidadRepository especialidadRepo;
    private final HorarioMedicoRepository horarioRepo;

    public MedicoService(MedicoRepository repo, EspecialidadRepository especialidadRepo,
                         HorarioMedicoRepository horarioRepo) {
        this.repo = repo;
        this.especialidadRepo = especialidadRepo;
        this.horarioRepo = horarioRepo;
    }

    @Transactional(readOnly = true)
    public List<Medico> listar() {
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public Medico obtener(Integer id) {
        return repo.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Médico no encontrado con id: " + id));
    }

    @Transactional
    public Medico guardar(MedicoDTO dto) {
        if (repo.existsByCmp(dto.getCmp())) {
            throw new DuplicateResourceException("Ya existe un médico con CMP: " + dto.getCmp());
        }
        Medico medico = new Medico();
        aplicar(medico, dto);
        return repo.save(medico);
    }

    @Transactional
    public Medico actualizar(Integer id, MedicoDTO dto) {
        Medico existente = obtener(id);
        if (repo.existsByCmpAndIdMedicoNot(dto.getCmp(), id)) {
            throw new DuplicateResourceException("Ya existe un médico con CMP: " + dto.getCmp());
        }
        aplicar(existente, dto);
        return repo.save(existente);
    }

    @Transactional
    public void eliminar(Integer id) {
        Medico medico = obtener(id);
        // La FK horario_medico -> medico se respeta: primero se eliminan sus horarios.
        horarioRepo.deleteByMedico_IdMedico(id);
        repo.delete(medico);
    }

    private void aplicar(Medico medico, MedicoDTO dto) {
        Especialidad especialidad = especialidadRepo.findById(dto.getIdEspecialidad())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Especialidad no encontrada con id: " + dto.getIdEspecialidad()));
        medico.setCmp(dto.getCmp());
        medico.setNombres(dto.getNombres());
        medico.setApellidos(dto.getApellidos());
        medico.setTelefono(dto.getTelefono());
        medico.setCorreo(dto.getCorreo());
        medico.setEspecialidad(especialidad);
        medico.setEstado(dto.getEstado() == null ? "A" : dto.getEstado());
    }
}
