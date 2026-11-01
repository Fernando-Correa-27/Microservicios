package com.tecsup.controller;

import com.tecsup.dto.HorarioMedicoDTO;
import com.tecsup.model.HorarioMedico;
import com.tecsup.services.HorarioMedicoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HorarioMedicoController {

    private final HorarioMedicoService service;

    public HorarioMedicoController(HorarioMedicoService service) {
        this.service = service;
    }

    @GetMapping("/medicos/{idMedico}/horarios")
    public ResponseEntity<List<HorarioMedico>> listar(@PathVariable Integer idMedico) {
        return ResponseEntity.ok(service.listarPorMedico(idMedico));
    }

    @PostMapping("/medicos/{idMedico}/horarios")
    public ResponseEntity<HorarioMedico> guardar(@PathVariable Integer idMedico,
                                                 @Valid @RequestBody HorarioMedicoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.guardar(idMedico, dto));
    }

    @DeleteMapping("/horarios/{idHorario}")
    public ResponseEntity<Map<String, String>> eliminar(@PathVariable Integer idHorario) {
        service.eliminar(idHorario);
        return ResponseEntity.ok(Map.of("mensaje", "Horario eliminado correctamente"));
    }
}
