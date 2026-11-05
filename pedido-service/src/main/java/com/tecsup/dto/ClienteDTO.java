package com.tecsup.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Respuesta de cliente-service.
 * Acepta tanto "apellido" como "apellidos" para no acoplar el contrato.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ClienteDTO {

    private Long id;
    private String nombre;

    @JsonAlias("apellidos")
    private String apellido;

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

    public String getApellido() {
        return apellido;
    }

    public void setApellido(String apellido) {
        this.apellido = apellido;
    }
}
