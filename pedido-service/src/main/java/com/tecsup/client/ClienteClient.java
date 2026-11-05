package com.tecsup.client;

import com.tecsup.dto.ClienteDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Cliente OpenFeign (GLAB-S12): resuelve "cliente-service" por Eureka.
 * Sin URL fija.
 */
@FeignClient(name = "cliente-service")
public interface ClienteClient {

    @GetMapping("/api/clientes/{id}")
    ClienteDTO obtenerCliente(@PathVariable("id") Long id);
}
