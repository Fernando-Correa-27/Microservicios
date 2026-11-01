package com.tecsup.controller;

import com.tecsup.dto.CitaDTO;
import com.tecsup.dto.EstadoCitaDTO;
import com.tecsup.model.Atencion;
import com.tecsup.model.Cita;
import com.tecsup.services.CitaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/citas")
public class CitaController {

    private final CitaService service;

    public CitaController(CitaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<Cita>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Cita> obtener(@PathVariable Integer id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    public ResponseEntity<Cita> guardar(@Valid @RequestBody CitaDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.guardar(dto));
    }

    @PutMapping("/{id}/estado")
    public ResponseEntity<Cita> cambiarEstado(@PathVariable Integer id,
                                              @Valid @RequestBody EstadoCitaDTO dto) {
        return ResponseEntity.ok(service.cambiarEstado(id, dto));
    }

    @PutMapping("/{id}/cancelar")
    public ResponseEntity<Cita> cancelar(@PathVariable Integer id) {
        return ResponseEntity.ok(service.cancelar(id));
    }

    @GetMapping("/{id}/atencion")
    public ResponseEntity<Atencion> obtenerAtencion(@PathVariable Integer id) {
        return ResponseEntity.ok(service.obtenerAtencion(id));
    }
}
