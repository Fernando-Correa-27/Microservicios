package com.tecsup.client;

import com.tecsup.exception.ResourceNotFoundException;
import com.tecsup.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

/**
 * Comunicación HTTP entre microservicios: producto-service consulta categoria-service.
 * La URL es configurable en application.yml (servicios.categoria.url).
 */
@Component
public class CategoriaClient {

    private final RestClient restClient;

    public CategoriaClient(@Value("${servicios.categoria.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void validarCategoria(Long categoriaId) {
        try {
            restClient.get()
                    .uri("/api/categorias/{id}", categoriaId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Categoría no encontrada con id: " + categoriaId);
            }
            throw new ServiceUnavailableException(
                    "categoria-service respondió con error " + ex.getStatusCode().value(), ex);
        } catch (ResourceAccessException ex) {
            throw new ServiceUnavailableException(
                    "categoria-service no está disponible. Verifique que se ejecute en el puerto 8082", ex);
        }
    }
}
