package com.tecsup.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Cambio de estado de una cita. Cuando el estado es ATENDIDA, los campos
 * diagnostico/tratamiento/observaciones se guardan en la tabla atencion (1:1).
 */
public class EstadoCitaDTO {

    @NotBlank(message = "El estado es obligatorio")
    @Pattern(regexp = "PROGRAMADA|CONFIRMADA|ATENDIDA|CANCELADA",
            message = "El estado debe ser PROGRAMADA, CONFIRMADA, ATENDIDA o CANCELADA")
    private String estado;

    @Size(max = 2000, message = "El diagnóstico no debe superar los 2000 caracteres")
    private String diagnostico;

    @Size(max = 2000, message = "El tratamiento no debe superar los 2000 caracteres")
    private String tratamiento;

    @Size(max = 2000, message = "Las observaciones no deben superar los 2000 caracteres")
    private String observaciones;

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getDiagnostico() {
        return diagnostico;
    }

    public void setDiagnostico(String diagnostico) {
        this.diagnostico = diagnostico;
    }

    public String getTratamiento() {
        return tratamiento;
    }

    public void setTratamiento(String tratamiento) {
        this.tratamiento = tratamiento;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public void setObservaciones(String observaciones) {
        this.observaciones = observaciones;
    }
}
