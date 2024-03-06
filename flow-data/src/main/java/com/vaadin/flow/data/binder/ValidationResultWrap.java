/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.data.binder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.function.SerializableFunction;

/**
 * Internal implementation of a {@code Result} that collects all possible
 * ValidationResults into one list. This class intercepts the normal chaining of
 * Converters and Validators, catching and collecting results.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @param <R>
 *            the result data type
 * @since 1.0
 */
class ValidationResultWrap<R> implements Result<R> {

    private final List<ValidationResult> resultList;
    private final Result<R> wrappedResult;

    ValidationResultWrap(Result<R> result, List<ValidationResult> resultList) {
        this.resultList = resultList;
        this.wrappedResult = result;
    }

    ValidationResultWrap(R value, ValidationResult result) {
        if (result.isError()) {
            wrappedResult = new SimpleResult<>(null, result.getErrorMessage());
        } else {
            wrappedResult = new SimpleResult<>(value, null);
        }
        this.resultList = new ArrayList<>();
        this.resultList.add(result);
    }

    List<ValidationResult> getValidationResults() {
        return Collections.unmodifiableList(resultList);
    }

    Result<R> getWrappedResult() {
        return wrappedResult;
    }

    @Override
    public <S> Result<S> flatMap(SerializableFunction<R, Result<S>> mapper) {
        Result<S> result = wrappedResult.flatMap(mapper);
        if (!(result instanceof ValidationResultWrap)) {
            return new ValidationResultWrap<S>(result, resultList);
        }

        List<ValidationResult> currentResults = new ArrayList<>(resultList);
        ValidationResultWrap<S> resultWrap = (ValidationResultWrap<S>) result;
        currentResults.addAll(resultWrap.getValidationResults());

        return new ValidationResultWrap<>(resultWrap.getWrappedResult(),
                currentResults);
    }

    @Override
    public void handle(SerializableConsumer<R> ifOk,
            SerializableConsumer<String> ifError) {
        wrappedResult.handle(ifOk, ifError);
    }

    @Override
    public boolean isError() {
        return wrappedResult.isError();
    }

    @Override
    public Optional<String> getMessage() {
        return wrappedResult.getMessage();
    }

    @Override
    public <X extends Throwable> R getOrThrow(
            SerializableFunction<String, ? extends X> exceptionProvider)
            throws X {
        return wrappedResult.getOrThrow(exceptionProvider);
    }

}
