/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html.testbench;

import org.openqa.selenium.support.ui.Select;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

/**
 * A TestBench element representing a <code>&lt;select&gt;</code> element.
 *
 * @since 1.0
 */
@Element("select")
public class SelectElement extends TestBenchElement {

    /**
     * Selects the first option matching the given text.
     *
     * @param text
     *            the text of the option to select
     */
    public void selectByText(String text) {
        // Must use the wrapped element to avoid to click() override
        new Select(getWrappedElement()).selectByVisibleText(text);
    }

    /**
     * Gets the text of the currently selected option.
     *
     * @return the text of the current option
     */
    public String getSelectedText() {
        return new Select(this).getFirstSelectedOption().getText();
    }

    /**
     * Selects the option with the given value.
     * <p>
     * To select based on the visible text, use {@link #selectByText(String)}.
     *
     * @param value
     *            the value to select
     */
    public void setValue(String value) {
        new Select(getWrappedElement()).selectByValue(value);
    }

    /**
     * Gets the value of the currently selected option.
     * <p>
     * To get the visible text, use {@link #getSelectedText()}.
     *
     * @return the value of the current option
     */
    public String getValue() {
        return getPropertyString("value");
    }
}
