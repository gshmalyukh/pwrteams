package com.example.demo.problem;

import lombok.Getter;

@Getter
public class RetryExhaustedException extends RuntimeException {

    private String uri;
    private Class<?> responseType;
    public RetryExhaustedException(String message, Throwable cause, String uri, Class<?> responseType) {
        super(message, cause);
        this.uri = uri;
        this.responseType = responseType;
    }
}