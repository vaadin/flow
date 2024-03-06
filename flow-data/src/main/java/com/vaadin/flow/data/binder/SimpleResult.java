/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import java.util.Objects;
import java.util.Optional;

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;

/**
 * An internal implementation of {@code Result}.
 *
 * @param <R>
 *            the result value type
 * @since 1.0
 */
class SimpleResult<R> implements Result<R> {

    private final R value;
    private final String message;

    /**
     * Creates a new {@link Result} instance using {@code value} for a non error
     * {@link Result} and {@code message} for an error {@link Result}.
     * <p>
     * If {@code message} is null then {@code value} is ignored and result is an
     * error.
     *
     * @param value
     *            the value of the result, may be {@code null}
     * @param message
     *            the error message of the result, may be {@code null}
     */
    SimpleResult(R value, String message) {
        // value != null => message == null
        assert value == null || message == null
                : "Message must be null if value is provided";
        this.value = value;
        this.message = message;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S> Result<S> flatMap(SerializableFunction<R, Result<S>> mapper) {
        Objects.requireNonNull(mapper, "mapper cannot be null");

        if (isError()) {
            // Safe cast; valueless
            return (Result<S>) this;
        } else {
            return mapper.apply(value);
        }
    }

    @Override
    public void handle(SerializableConsumer<R> ifOk,
            SerializableConsumer<String> ifError) {
        Objects.requireNonNull(ifOk, "ifOk cannot be null");
        Objects.requireNonNull(ifError, "ifError cannot be null");
        if (isError()) {
            ifError.accept(message);
        } else {
            ifOk.accept(value);
        }
    }

    @Override
    public Optional<String> getMessage() {
        return Optional.ofNullable(message);
    }

    @Override
    public boolean isError() {
        return message != null;
    }

    @Override
    public String toString() {
        if (isError()) {
            return "error(" + message + ")";
        } else {
            return "ok(" + value + ")";
        }
    }

    @Override
    public <X extends Throwable> R getOrThrow(
            SerializableFunction<String, ? extends X> exceptionSupplier)
            throws X {
        Objects.requireNonNull(exceptionSupplier,
                "Exception supplier cannot be null");
        if (isError()) {
            throw exceptionSupplier.apply(message);
        } else {
            return value;
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!obj.getClass().equals(getClass())) {
            return false;
        }
        SimpleResult<?> that = (SimpleResult<?>) obj;
        return Objects.equals(that.value, value)
                && Objects.equals(that.message, message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value, message);
    }

}
