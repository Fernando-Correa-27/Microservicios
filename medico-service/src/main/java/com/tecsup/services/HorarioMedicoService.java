package com.tecsup.services;

import com.tecsup.dto.HorarioMedicoDTO;
import com.tecsup.exception.BusinessRuleException;
import com.tecsup.exception.ResourceNotFoundException;
import com.tecsup.model.HorarioMedico;
import com.tecsup.model.Medico;
import com.tecsup.repository.HorarioMedicoRepository;
import com.tecsup.repository.MedicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class HorarioMedicoService {

    private final HorarioMedicoRepository repo;
    private final MedicoRepository medicoRepo;

    public HorarioMedicoService(HorarioMedicoRepository repo, MedicoRepository medicoRepo) {
        this.repo = repo;
        this.medicoRepo = medicoRepo;
    }

    @Transactional(readOnly = true)
    public List<HorarioMedico> listarPorMedico(Integer idMedico) {
        obtenerMedico(idMedico);
        return repo.findByMedico_IdMedicoOrderByDiaSemanaAscHoraInicioAsc(idMedico);
    }

    @Transactional
    public HorarioMedico guardar(Integer idMedico, HorarioMedicoDTO dto) {
        Medico medico = obtenerMedico(idMedico);
        if (!dto.getHoraFin().isAfter(dto.getHoraInicio())) {
            throw new BusinessRuleException("La hora de fin debe ser posterior a la hora de inicio");
        }
        HorarioMedico horario = new HorarioMedico();
        horario.setMedico(medico);
        horario.setDiaSemana(dto.getDiaSemana());
        horario.setHoraInicio(dto.getHoraInicio());
        horario.setHoraFin(dto.getHoraFin());
        return repo.save(horario);
    }

    @Transactional
    public void eliminar(Integer idHorario) {
        HorarioMedico horario = repo.findById(idHorario).orElseThrow(
                () -> new ResourceNotFoundException("Horario no encontrado con id: " + idHorario));
        repo.delete(horario);
    }

    private Medico obtenerMedico(Integer idMedico) {
        return medicoRepo.findById(idMedico).orElseThrow(
                () -> new ResourceNotFoundException("Médico no encontrado con id: " + idMedico));
    }
}
