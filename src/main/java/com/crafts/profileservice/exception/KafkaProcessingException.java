package com.crafts.profileservice.exception;

public class KafkaProcessingException extends Exception {
    public KafkaProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
