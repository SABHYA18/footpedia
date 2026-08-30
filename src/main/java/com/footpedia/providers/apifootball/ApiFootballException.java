package com.footpedia.providers.apifootball;

public class ApiFootballException extends RuntimeException {

    public ApiFootballException(String message) {
        super(message);
    }

    public ApiFootballException(String message, Throwable cause) {
        super(message, cause);
    }
}
