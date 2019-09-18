/*
 * Copyright 2000-2018 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
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
