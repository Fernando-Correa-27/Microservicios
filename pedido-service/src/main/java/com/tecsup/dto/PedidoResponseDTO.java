package com.tecsup.dto;

/**
 * Respuesta completa de un pedido (GLAB-S12).
 * Formato pedido por la guía:
 * {"id":1,"cantidad":2,"cliente":{...},"producto":{...}}
 */
public class PedidoResponseDTO {

    private Long id;
    private Integer cantidad;
    private String estado;
    private ClienteDTO cliente;
    private ProductoDTO producto;

    public PedidoResponseDTO() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getCantidad() {
        return cantidad;
    }

    public void setCantidad(Integer cantidad) {
        this.cantidad = cantidad;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public ClienteDTO getCliente() {
        return cliente;
    }

    public void setCliente(ClienteDTO cliente) {
        this.cliente = cliente;
    }

    public ProductoDTO getProducto() {
        return producto;
    }

    public void setProducto(ProductoDTO producto) {
        this.producto = producto;
    }
}
