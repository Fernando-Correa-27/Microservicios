package com.tecsup.services;

import com.tecsup.client.ClienteClient;
import com.tecsup.client.ProductoClient;
import com.tecsup.dto.DetallePedidoDTO;
import com.tecsup.dto.PedidoDTO;
import com.tecsup.exception.ResourceNotFoundException;
import com.tecsup.model.DetallePedido;
import com.tecsup.model.Pedido;
import com.tecsup.repository.PedidoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
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

    @Transactional(readOnly = true)
    public List<Pedido> listar() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public Pedido obtener(Long id) {
        return repository.findById(id).orElseThrow(
                () -> new ResourceNotFoundException("Pedido no encontrado con id: " + id));
    }

    @Transactional
    public Pedido guardar(PedidoDTO dto) {
        validarReferencias(dto);
        Pedido pedido = new Pedido();
        aplicar(pedido, dto);
        return repository.save(pedido);
    }

    /**
     * Actualiza cabecera y REEMPLAZA los detalles (decisión documentada):
     * se eliminan los anteriores por orphanRemoval y se insertan los enviados.
     * El total se recalcula en el servidor para que no pueda quedar inconsistente.
     */
    @Transactional
    public Pedido actualizar(Long id, PedidoDTO dto) {
        Pedido existente = obtener(id);
        validarReferencias(dto);
        aplicar(existente, dto);
        return repository.save(existente);
    }

    @Transactional
    public void eliminar(Long id) {
        repository.delete(obtener(id));
    }

    private void validarReferencias(PedidoDTO dto) {
        clienteClient.validarCliente(dto.getIdCliente());
        dto.getDetalles().forEach(detalle -> productoClient.validarProducto(detalle.getIdProducto()));
    }

    private void aplicar(Pedido pedido, PedidoDTO dto) {
        pedido.setFecha(dto.getFecha());
        pedido.setIdCliente(dto.getIdCliente());
        pedido.getDetalles().clear();

        BigDecimal total = BigDecimal.ZERO;
        for (DetallePedidoDTO dtoDetalle : dto.getDetalles()) {
            DetallePedido detalle = new DetallePedido();
            detalle.setIdProducto(dtoDetalle.getIdProducto());
            detalle.setCantidad(dtoDetalle.getCantidad());
            detalle.setPrecio(dtoDetalle.getPrecio());
            detalle.setPedido(pedido);
            pedido.getDetalles().add(detalle);
            total = total.add(dtoDetalle.getPrecio()
                    .multiply(BigDecimal.valueOf(dtoDetalle.getCantidad())));
        }
        pedido.setTotal(total);
    }
}
