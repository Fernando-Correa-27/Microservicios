package com.tecsup.client;

import com.tecsup.dto.ProductoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente OpenFeign (GLAB-S12): resuelve "producto-service" por Eureka.
 * Sin URL fija.
 */
@FeignClient(name = "producto-service")
public interface ProductoClient {

    @GetMapping("/api/productos/{id}")
    ProductoDTO obtenerProducto(@PathVariable("id") Long id);
}
