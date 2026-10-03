package com.javeriana.zoo_fantastico.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoEx extends RuntimeException {
    public ResourceNotFoEx (String message) {
        super(message);
    }
}