package com.footpedia.providers.footballdata;

public class FootballDataException extends RuntimeException {

    public FootballDataException(String message) {
        super(message);
    }

    public FootballDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
