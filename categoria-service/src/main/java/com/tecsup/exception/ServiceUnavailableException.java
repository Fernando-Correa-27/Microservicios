package com.tecsup.exception;

/**
 * Se lanza cuando un microservicio dependiente no responde (caído, timeout o error inesperado).
 * El handler global la traduce a HTTP 503.
 */
public class ServiceUnavailableException extends RuntimeException {

    public ServiceUnavailableException(String mensaje) {
        super(mensaje);
    }

    public ServiceUnavailableException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
