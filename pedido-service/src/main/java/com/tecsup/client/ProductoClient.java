package com.tecsup.client;

import com.tecsup.exception.ResourceNotFoundException;
import com.tecsup.exception.ServiceUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Component
public class ProductoClient {

    private final RestClient restClient;

    public ProductoClient(@Value("${servicios.producto.url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public void validarProducto(Long idProducto) {
        try {
            restClient.get()
                    .uri("/api/productos/{id}", idProducto)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 404) {
                throw new ResourceNotFoundException("Producto no encontrado con id: " + idProducto);
            }
            throw new ServiceUnavailableException(
                    "producto-service respondió con error " + ex.getStatusCode().value(), ex);
        } catch (ResourceAccessException ex) {
            throw new ServiceUnavailableException(
                    "producto-service no está disponible. Verifique que se ejecute en el puerto 8083", ex);
        }
    }
}
