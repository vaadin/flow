/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.component.html.testbench;

import org.openqa.selenium.By;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

/**
 * A TestBench element representing a <code>&lt;details&gt;</code> element.
 *
 * @since
 */
@Element("details")
public class NativeDetailsElement extends TestBenchElement {

    /**
     * Dispatches a {@code toggle} event by clicking the summary of the details.
     * Toggles the details element open state.
     */
    public void toggle() {
        findElement(By.tagName("summary")).click();
    }
}
