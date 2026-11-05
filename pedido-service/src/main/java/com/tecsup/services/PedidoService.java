package com.tecsup.services;

import com.tecsup.client.ClienteClient;
import com.tecsup.client.ProductoClient;
import com.tecsup.dto.ClienteDTO;
import com.tecsup.dto.PedidoDTO;
import com.tecsup.dto.PedidoResponseDTO;
import com.tecsup.dto.ProductoDTO;
import com.tecsup.exception.ConflictException;
import com.tecsup.exception.ResourceNotFoundException;
import com.tecsup.model.Pedido;
import com.tecsup.repository.PedidoRepository;
import feign.FeignException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class PedidoService {

    private final PedidoRepository repository;
    private final ClienteClient clienteClient;
    private final ProductoClient productoClient;

    public PedidoService(PedidoRepository repository, ClienteClient clienteClient, ProductoClient productoClient) {
        this.repository = repository;
        this.clienteClient = clienteClient;
        this.productoClient = productoClient;
    }

    /**
     * Listado completo. Si un cliente o producto remoto falla, ese pedido
     * se devuelve con el dato en null sin romper todo el listado.
     */
    @Transactional(readOnly = true)
    public List<PedidoResponseDTO> listar() {
        List<PedidoResponseDTO> respuesta = new ArrayList<>();
        for (Pedido pedido : repository.findAll()) {
            respuesta.add(toResponseTolerante(pedido));
        }
        return respuesta;
    }

    /**
     * Flujo principal de S12: pedido propio + cliente remoto + producto remoto.
     */
    @Transactional(readOnly = true)
    public PedidoResponseDTO obtener(Long id) {
        Pedido pedido = repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Pedido no encontrado con id: " + id));
        return toResponse(pedido);
    }

    /**
     * Valida cliente y producto ANTES de guardar (corrección a la guía),
     * para no dejar pedidos huérfanos si una referencia no existe.
     */
    @Transactional
    public PedidoResponseDTO guardar(PedidoDTO dto) {
        ClienteDTO cliente = obtenerClienteRemoto(dto.getClienteId());
        ProductoDTO producto = obtenerProductoRemoto(dto.getProductoId());

        Pedido pedido = new Pedido();
        pedido.setClienteId(dto.getClienteId());
        pedido.setProductoId(dto.getProductoId());
        pedido.setCantidad(dto.getCantidad());
        pedido.setEstado(Pedido.PENDIENTE);
        Pedido guardado = repository.save(pedido);

        return toResponse(guardado, cliente, producto);
    }

    @Transactional
    public PedidoResponseDTO anular(Long id) {
        return cambiarEstado(id, Pedido.ANULADO);
    }

    @Transactional
    public PedidoResponseDTO atender(Long id) {
        return cambiarEstado(id, Pedido.ATENDIDO);
    }

    /**
     * Transiciones válidas: PENDIENTE -> ATENDIDO | ANULADO.
     * ATENDIDO y ANULADO son terminales.
     */
    private PedidoResponseDTO cambiarEstado(Long id, String nuevoEstado) {
        Pedido pedido = repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Pedido no encontrado con id: " + id));

        if (!Pedido.PENDIENTE.equals(pedido.getEstado())) {
            throw new ConflictException("Transición no permitida: " + pedido.getEstado()
                    + " -> " + nuevoEstado + ". Solo un pedido PENDIENTE puede cambiar de estado");
        }

        pedido.setEstado(nuevoEstado);
        return toResponse(repository.save(pedido));
    }

    private PedidoResponseDTO toResponse(Pedido pedido) {
        return toResponse(pedido, obtenerClienteRemoto(pedido.getClienteId()),
                obtenerProductoRemoto(pedido.getProductoId()));
    }

    private PedidoResponseDTO toResponse(Pedido pedido, ClienteDTO cliente, ProductoDTO producto) {
        PedidoResponseDTO dto = new PedidoResponseDTO();
        dto.setId(pedido.getIdPedido());
        dto.setCantidad(pedido.getCantidad());
        dto.setEstado(pedido.getEstado());
        dto.setCliente(cliente);
        dto.setProducto(producto);
        return dto;
    }

    private PedidoResponseDTO toResponseTolerante(Pedido pedido) {
        ClienteDTO cliente = null;
        ProductoDTO producto = null;
        try {
            cliente = obtenerClienteRemoto(pedido.getClienteId());
        } catch (RuntimeException ignored) {
            // se deja null para no romper el listado
        }
        try {
            producto = obtenerProductoRemoto(pedido.getProductoId());
        } catch (RuntimeException ignored) {
            // se deja null para no romper el listado
        }
        return toResponse(pedido, cliente, producto);
    }

    private ClienteDTO obtenerClienteRemoto(Long id) {
        try {
            return clienteClient.obtenerCliente(id);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Cliente no encontrado con id: " + id);
        }
    }

    private ProductoDTO obtenerProductoRemoto(Long id) {
        try {
            return productoClient.obtenerProducto(id);
        } catch (FeignException.NotFound ex) {
            throw new ResourceNotFoundException("Producto no encontrado con id: " + id);
        }
    }
}
