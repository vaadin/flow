/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.tests.util;

import org.junit.Assert;

import com.vaadin.flow.function.SerializableConsumer;

public class SingleCaptureConsumer<T> implements SerializableConsumer<T> {

    private boolean captured = false;
    private T capturedValue;

    @Override
    public void accept(T value) {
        if (captured) {
            Assert.fail("Consumer has already been run");
        }
        captured = true;
        capturedValue = value;
    }

    public T getCapturedValue() {
        if (!captured) {
            Assert.fail("Consumer has not been run");
        }
        return capturedValue;
    }

    public boolean isCaptured() {
        return captured;
    }

}
