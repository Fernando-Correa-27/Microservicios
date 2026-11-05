package com.tecsup.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * DTO de respuesta de categoria-service (GLAB-S12).
 * Se ignoran campos desconocidos para tolerar cambios en el contrato remoto.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CategoriaDTO {

    private Long id;
    private String nombre;
    private String estado;

    public CategoriaDTO() {
    }

    public CategoriaDTO(Long id, String nombre, String estado) {
        this.id = id;
        this.nombre = nombre;
        this.estado = estado;
    }

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

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }
}
