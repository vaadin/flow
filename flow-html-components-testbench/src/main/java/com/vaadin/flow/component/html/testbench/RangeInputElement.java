/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html.testbench;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

/**
 * A TestBench element representing an <code>&lt;input type='range'&gt;</code>
 * element.
 *
 * @since 24.3
 */
@Element("input")
public class RangeInputElement extends TestBenchElement {

    /**
     * Sets the value of the text input to the given value, clearing out any old
     * value of the input.
     *
     * @param value
     *            the value to set
     */
    public void setValue(Double value) {
        setProperty("value", value);
    }

    /**
     * Clears the input field.
     */
    @Override
    public void clear() {
        setValue(null);
    }

    public Double getValue() {
        return getPropertyDouble("value");
    }
}
