/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.router;

import org.junit.Assert;
import org.junit.Test;

public class ErrorParameterTest {
    @Test
    public void matchingExceptionType() {
        NullPointerException exception = new NullPointerException();

        ErrorParameter<NullPointerException> errorParameter = new ErrorParameter<>(
                NullPointerException.class, exception);

        Assert.assertSame(exception, errorParameter.getException());
        Assert.assertSame(exception, errorParameter.getCaughtException());
    }

    @Test
    public void superExceptionType() {
        NullPointerException exception = new NullPointerException();

        ErrorParameter<RuntimeException> errorParameter = new ErrorParameter<>(
                RuntimeException.class, exception);

        Assert.assertSame(exception, errorParameter.getException());
        Assert.assertSame(exception, errorParameter.getCaughtException());
    }

    @Test
    public void matchingCauseType() {
        NullPointerException cause = new NullPointerException();
        IllegalStateException exception = new IllegalStateException(cause);

        ErrorParameter<NullPointerException> errorParameter = new ErrorParameter<>(
                NullPointerException.class, exception);

        Assert.assertSame(cause, errorParameter.getException());
        Assert.assertSame(exception, errorParameter.getCaughtException());
    }

    @Test
    public void superMatchingCauseType() {
        NullPointerException cause = new NullPointerException() {

        };
        IllegalStateException exception = new IllegalStateException(cause);

        ErrorParameter<NullPointerException> errorParameter = new ErrorParameter<>(
                NullPointerException.class, exception);

        Assert.assertSame(cause, errorParameter.getException());
        Assert.assertSame(exception, errorParameter.getCaughtException());
    }

}
