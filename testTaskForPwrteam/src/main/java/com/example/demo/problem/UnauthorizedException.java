package com.example.demo.problem;

public class UnauthorizedException  extends RuntimeException {
    public UnauthorizedException(String message, Throwable cause) {
        super(message, cause);
    }
}
