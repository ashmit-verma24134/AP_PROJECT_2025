package edu.univ.erp.service;

/**
 * Simple immutable result object used by service methods.
 */
public final class Result {
    private final boolean success;
    private final String message;

    private Result(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    // factories
    public static Result ok(String message) {
        return new Result(true, message == null ? "" : message);
    }

    public static Result error(String message) {
        return new Result(false, message == null ? "" : message);
    }

    // accessors
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "Result[" + (success ? "OK" : "ERR") + ", " + message + "]";
    }
}
