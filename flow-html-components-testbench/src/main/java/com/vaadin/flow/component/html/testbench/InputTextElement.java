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

import org.openqa.selenium.Keys;

import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

/**
 * A TestBench element representing an <code>&lt;input type='text'&gt;</code>
 * element.
 *
 * @since 1.0
 */
@Element("input")
public class InputTextElement extends TestBenchElement {

    /**
     * Sets the value of the text input to the given value, clearing out any old
     * value of the input.
     * 
     * @param value
     *            the value to set
     */
    public void setValue(String value) {
        if ("".equals(value)) {
            clear();
            return;
        }

        // Remove old value
        setProperty("value", "");
        // Type and add a TAB to ensure a change event is sent
        sendKeys(value + Keys.TAB);
    }

    /**
     * Clears the input field.
     */
    @Override
    public void clear() {
        String value = getValue();
        if ("".equals(value))
            return;

        // Type and add a TAB to ensure a change event is sent
        setValue("a");
        sendKeys(Keys.BACK_SPACE, Keys.TAB);
    }

    public String getValue() {
        return getPropertyString("value");
    }
}
