package com.footpedia.providers.espn;

public class EspnException extends RuntimeException {

    public EspnException(String message) {
        super(message);
    }

    public EspnException(String message, Throwable cause) {
        super(message, cause);
    }
}
