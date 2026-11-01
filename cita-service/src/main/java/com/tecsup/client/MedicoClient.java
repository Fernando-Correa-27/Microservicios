package com.tecsup.client;

import com.tecsup.exception.ResourceNotFoundException;
import com.tecsup.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class MedicoClient {

    private final RestClient restClient;

    public MedicoClient(@Value("${servicios.medico.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void validarMedico(Integer idMedico) {
        try {
            restClient.get()
                    .uri("/api/medicos/{id}", idMedico)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Médico no encontrado con id: " + idMedico);
            }
            throw new ServiceUnavailableException(
                    "medico-service respondió con error " + ex.getStatusCode().value(), ex);
        } catch (ResourceAccessException ex) {
            throw new ServiceUnavailableException(
                    "medico-service no está disponible. Verifique que se ejecute en el puerto 8086", ex);
        }
    }
}
