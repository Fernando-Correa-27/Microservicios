package com.tecsup.client;

import com.tecsup.exception.ResourceNotFoundException;
import com.tecsup.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ClienteClient {

    private final RestClient restClient;

    public ClienteClient(@Value("${servicios.cliente.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void validarCliente(Long idCliente) {
        try {
            restClient.get()
                    .uri("/api/clientes/{id}", idCliente)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Cliente no encontrado con id: " + idCliente);
            }
            throw new ServiceUnavailableException(
                    "cliente-service respondió con error " + ex.getStatusCode().value(), ex);
        } catch (ResourceAccessException ex) {
            throw new ServiceUnavailableException(
                    "cliente-service no está disponible. Verifique que se ejecute en el puerto 8081", ex);
        }
    }
}
