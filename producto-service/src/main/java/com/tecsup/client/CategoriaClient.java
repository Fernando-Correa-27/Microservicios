package com.tecsup.client;

import com.tecsup.dto.CategoriaDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente OpenFeign (GLAB-S12).
 * No usa URL fija: resuelve "categoria-service" a través de Eureka.
 */
@FeignClient(name = "categoria-service")
public interface CategoriaClient {

    @GetMapping("/api/categorias/{id}")
    CategoriaDTO obtenerCategoria(@PathVariable("id") Long id);
}
