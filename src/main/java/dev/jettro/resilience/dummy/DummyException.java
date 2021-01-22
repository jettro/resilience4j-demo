package dev.jettro.resilience.dummy;

public class DummyException extends RuntimeException{
    public DummyException(String message) {
        super(message);
    }

    public DummyException(String message, Throwable cause) {
        super(message, cause);
    }
}
