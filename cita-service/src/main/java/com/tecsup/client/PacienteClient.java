package com.tecsup.client;

import com.tecsup.exception.ResourceNotFoundException;
import com.tecsup.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class PacienteClient {

    private final RestClient restClient;

    public PacienteClient(@Value("${servicios.paciente.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void validarPaciente(Integer idPaciente) {
        try {
            restClient.get()
                    .uri("/api/pacientes/{id}", idPaciente)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Paciente no encontrado con id: " + idPaciente);
            }
            throw new ServiceUnavailableException(
                    "paciente-service respondió con error " + ex.getStatusCode().value(), ex);
        } catch (ResourceAccessException ex) {
            throw new ServiceUnavailableException(
                    "paciente-service no está disponible. Verifique que se ejecute en el puerto 8085", ex);
        }
    }
}
