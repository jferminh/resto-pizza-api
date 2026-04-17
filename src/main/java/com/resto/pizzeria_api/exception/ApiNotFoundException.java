package com.resto.pizzeria_api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception levée lorsqu'une ressource n'est pas trouvée.
 * Retourne un statut HTTP 404.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ApiNotFoundException extends Exception {
    /**
     * Construit l'exception avec un message.
     *
     * @param message Message d'erreur
     */
    public ApiNotFoundException(String message) {
        super(message);
    }
}
