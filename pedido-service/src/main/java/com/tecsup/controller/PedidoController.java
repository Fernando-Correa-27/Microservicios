package com.tecsup.controller;

import com.tecsup.dto.PedidoDTO;
import com.tecsup.dto.PedidoResponseDTO;
import com.tecsup.services.PedidoService;
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
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<PedidoResponseDTO>> listar() {
        return ResponseEntity.ok(service.listar());
    }

    @GetMapping("/{id}")
    public ResponseEntity<PedidoResponseDTO> buscar(@PathVariable Long id) {
        return ResponseEntity.ok(service.obtener(id));
    }

    @PostMapping
    public ResponseEntity<PedidoResponseDTO> guardar(@Valid @RequestBody PedidoDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.guardar(dto));
    }

    /**
     * GLAB-S12: un pedido NO se elimina; se anula.
     */
    @PutMapping("/{id}/anular")
    public ResponseEntity<PedidoResponseDTO> anular(@PathVariable Long id) {
        return ResponseEntity.ok(service.anular(id));
    }

    /**
     * GLAB-S12: un pedido pasa a ATENDIDO.
     */
    @PutMapping("/{id}/atender")
    public ResponseEntity<PedidoResponseDTO> atender(@PathVariable Long id) {
        return ResponseEntity.ok(service.atender(id));
    }
}
