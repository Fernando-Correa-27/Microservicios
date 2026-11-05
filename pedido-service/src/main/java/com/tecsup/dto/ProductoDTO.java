package com.tecsup.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

/**
 * Respuesta de producto-service.
 * Ignora campos desconocidos (por ejemplo la "categoria" anidada que agrega S12).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ProductoDTO {

    private Long id;
    private String nombre;
    private BigDecimal precio;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }
}
