package com.tecsup.services;

import com.tecsup.client.MedicoClient;
import com.tecsup.client.PacienteClient;
import com.tecsup.dto.CitaDTO;
import com.tecsup.dto.EstadoCitaDTO;
import com.tecsup.exception.BadRequestException;
import com.tecsup.exception.BusinessRuleException;
import com.tecsup.exception.ResourceNotFoundException;
import com.tecsup.model.Atencion;
import com.tecsup.model.Cita;
import com.tecsup.repository.CitaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CitaService {

    /**
     * Transiciones válidas del ciclo de vida de una cita.
     * ATENDIDA y CANCELADA son estados terminales: no admiten cambios.
     */
    private static final Map<String, Set<String>> TRANSICIONES = Map.of(
            "PROGRAMADA", Set.of("CONFIRMADA", "CANCELADA"),
            "CONFIRMADA", Set.of("ATENDIDA", "CANCELADA"),
            "ATENDIDA", Set.of(),
            "CANCELADA", Set.of()
    );

    private final CitaRepository repo;
    private final PacienteClient pacienteClient;
    private final MedicoClient medicoClient;

    public CitaService(CitaRepository repo, PacienteClient pacienteClient, MedicoClient medicoClient) {
        this.repo = repo;
        this.pacienteClient = pacienteClient;
        this.medicoClient = medicoClient;
    }

    @Transactional(readOnly = true)
    public List<Cita> listar() {
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public Cita obtener(Integer id) {
        return repo.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Cita no encontrada con id: " + id));
    }

    @Transactional
    public Cita guardar(CitaDTO dto) {
        // Integridad referencial entre microservicios por HTTP (no hay FK física).
        pacienteClient.validarPaciente(dto.getIdPaciente());
        medicoClient.validarMedico(dto.getIdMedico());

        Cita cita = new Cita();
        cita.setFecha(dto.getFecha());
        cita.setHora(dto.getHora());
        cita.setIdPaciente(dto.getIdPaciente());
        cita.setIdMedico(dto.getIdMedico());
        cita.setMotivo(dto.getMotivo());
        cita.setEstado("PROGRAMADA");
        return repo.save(cita);
    }

    @Transactional
    public Cita cambiarEstado(Integer id, EstadoCitaDTO dto) {
        Cita cita = obtener(id);
        String actual = cita.getEstado();
        String nuevo = dto.getEstado();

        if (!TRANSICIONES.getOrDefault(actual, Set.of()).contains(nuevo)) {
            throw new BusinessRuleException(
                    "Transición no permitida: " + actual + " -> " + nuevo
                            + ". Estados terminales: ATENDIDA y CANCELADA");
        }

        if ("ATENDIDA".equals(nuevo)) {
            if (dto.getDiagnostico() == null || dto.getDiagnostico().isBlank()) {
                throw new BadRequestException("Para marcar la cita como ATENDIDA el diagnóstico es obligatorio");
            }
            if (cita.getAtencion() != null) {
                throw new BusinessRuleException("La cita ya tiene una atención registrada");
            }
            Atencion atencion = new Atencion();
            atencion.setDiagnostico(dto.getDiagnostico());
            atencion.setTratamiento(dto.getTratamiento());
            atencion.setObservaciones(dto.getObservaciones());
            cita.setAtencion(atencion);
        }

        cita.setEstado(nuevo);
        return repo.save(cita);
    }

    @Transactional
    public Cita cancelar(Integer id) {
        Cita cita = obtener(id);
        if ("ATENDIDA".equals(cita.getEstado()) || "CANCELADA".equals(cita.getEstado())) {
            throw new BusinessRuleException(
                    "No se puede cancelar una cita en estado " + cita.getEstado());
        }
        cita.setEstado("CANCELADA");
        return repo.save(cita);
    }

    @Transactional(readOnly = true)
    public Atencion obtenerAtencion(Integer idCita) {
        Cita cita = obtener(idCita);
        if (cita.getAtencion() == null) {
            throw new ResourceNotFoundException("La cita " + idCita + " no tiene atención registrada");
        }
        return cita.getAtencion();
    }
}
