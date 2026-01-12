/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ReactAdapterIT extends ChromeBrowserTest {

    @Test
    public void validateInitialState() {
        open();

        waitForDevServer();

        Assert.assertEquals("initialValue",
                getReactElement().getPropertyString("value"));

        $(NativeButtonElement.class).id("getValueButton").click();
        Assert.assertEquals("initialValue",
                $(SpanElement.class).id("getOutput").getText());
    }

    @Test
    public void validateSetState() {
        open();

        waitForDevServer();

        $(NativeButtonElement.class).id("setValueButton").click();

        Assert.assertEquals("set value",
                getReactElement().getPropertyString("value"));
    }

    @Test
    public void validateGetState() {
        open();

        waitForDevServer();

        getReactElement().clear();
        getReactElement().focus();
        getReactElement().sendKeys("get value");

        $(NativeButtonElement.class).id("getValueButton").click();

        Assert.assertEquals("get value",
                $(SpanElement.class).id("getOutput").getText());
    }

    @Test
    public void validateListener() {
        open();

        waitForDevServer();

        getReactElement().clear();
        getReactElement().focus();
        getReactElement().sendKeys("listener value");

        Assert.assertEquals("listener value",
                $(SpanElement.class).id("listenerOutput").getText());
    }

    private TestBenchElement getAdapterElement() {
        return $("react-input").first();
    }

    private TestBenchElement getReactElement() {
        return getAdapterElement().$("input").first();
    }

}
