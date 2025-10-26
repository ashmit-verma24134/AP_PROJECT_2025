package edu.univ.erp.service;

public class Result {
    public final boolean success;
    public final String message;

    private Result(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static Result ok(String message) {
        return new Result(true, message);
    }

    public static Result error(String message) {
        return new Result(false, message);
    }

    // Optional convenience:
    @Override
    public String toString() { return (success ? "OK: " : "ERR: ") + message; }
}
