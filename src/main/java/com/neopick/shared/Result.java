package com.neopick.shared;

import java.util.Optional;
import java.util.function.Function;

public final class Result<T, E> {

    private final T value;
    private final E error;

    private Result(T value, E error) {
        this.value = value;
        this.error = error;
    }

    public static <T, E> Result<T, E> success(T value) {
        return new Result<>(value, null);
    }

    public static <T, E> Result<T, E> failure(E error) {
        return new Result<>(null, error);
    }

    public boolean isSuccess() {
        return error == null;
    }

    public boolean isFailure() {
        return error != null;
    }

    public T getValue() {
        if (isFailure()) {
            throw new IllegalStateException("Cannot get value from a failed result");
        }
        return value;
    }

    public E getError() {
        if (isSuccess()) {
            throw new IllegalStateException("Cannot get error from a successful result");
        }
        return error;
    }

    public Optional<T> valueOptional() {
        return Optional.ofNullable(value);
    }

    public <U> Result<U, E> map(Function<T, U> mapper) {
        if (isSuccess()) {
            return Result.success(mapper.apply(value));
        }
        return Result.failure(error);
    }
}
