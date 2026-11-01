package com.tecsup.services;

import com.tecsup.dto.ClienteDTO;
import com.tecsup.exception.DuplicateResourceException;
import com.tecsup.exception.ResourceNotFoundException;
import com.tecsup.model.Cliente;
import com.tecsup.repository.ClienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ClienteService {

    private final ClienteRepository repo;

    public ClienteService(ClienteRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<Cliente> listar() {
        return repo.findAll();
    }

    @Transactional(readOnly = true)
    public Cliente obtener(Long id) {
        return repo.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Cliente no encontrado con id: " + id));
    }

    @Transactional
    public Cliente guardar(ClienteDTO dto) {
        if (repo.existsByNroDni(dto.getNroDni())) {
            throw new DuplicateResourceException("Ya existe un cliente con DNI: " + dto.getNroDni());
        }
        Cliente cliente = new Cliente();
        aplicar(cliente, dto);
        return repo.save(cliente);
    }

    @Transactional
    public Cliente actualizar(Long id, ClienteDTO dto) {
        Cliente existente = obtener(id);
        if (repo.existsByNroDniAndIdNot(dto.getNroDni(), id)) {
            throw new DuplicateResourceException("Ya existe un cliente con DNI: " + dto.getNroDni());
        }
        aplicar(existente, dto);
        return repo.save(existente);
    }

    @Transactional
    public void eliminar(Long id) {
        repo.delete(obtener(id));
    }

    private void aplicar(Cliente cliente, ClienteDTO dto) {
        cliente.setNombre(dto.getNombre());
        cliente.setApellidos(dto.getApellidos());
        cliente.setDireccion(dto.getDireccion());
        cliente.setTelefono(dto.getTelefono());
        cliente.setNroDni(dto.getNroDni());
        cliente.setEstado(dto.getEstado());
    }
}
