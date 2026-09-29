/*
 * Copyright 2000-2026 Vaadin Ltd.
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

    @Test
    public void validateSetNullState() {
        open();

        waitForDevServer();

        $(NativeButtonElement.class).id("setNullValueButton").click();

        // getPropertyString returns null for null/undefined values
        String value = getAdapterElement().getPropertyString("value");
        Assert.assertNull("Expected null value, not string 'null'", value);
    }

    @Test
    public void contentTeleportedWhileConnecting_rendersComponentOnce() {
        open();

        waitForDevServer();

        $(NativeButtonElement.class).id("openTeleportingContainerButton")
                .click();

        TestBenchElement container = $(TestBenchElement.class)
                .id("teleportingContainer");
        waitUntil(driver -> !container.$("input").all().isEmpty());

        Assert.assertEquals(
                "Adapter element teleported while connecting must render "
                        + "exactly one React component",
                1, container.$("input").all().size());
        Assert.assertEquals(
                "The rendered component must be bound to the server-side state",
                ReactAdapterView.TELEPORTED_VALUE,
                container.$("input").all().get(0).getPropertyString("value"));
    }

    @Test
    public void ownRootReconnectedQuickly_rendersComponentOnceAndUpdates() {
        open();

        waitForDevServer();

        // Outside the Flow container no portal is used, so the adapter creates
        // its own React root. Reconnect it before connectedCallback finishes.
        executeScript("const element = document.createElement('react-input');"
                + "element.id = 'ownRootInput';"
                + "document.body.append(element);" + "element.remove();"
                + "document.body.append(element);"
                + "return new Promise(resolve => setTimeout(resolve, 100));");
        TestBenchElement adapter = $(TestBenchElement.class).id("ownRootInput");
        waitUntil(driver -> !adapter.$("input").all().isEmpty());
        // React logs an error when a second root is created for the element
        checkLogsForErrors();
        assertSingleInputUpdates(adapter, "first");

        // Reconnect the element again after its root has been rendered, and
        // let the pending unmount of the disconnect run.
        executeScript("const element = arguments[0];" + "element.remove();"
                + "document.body.append(element);"
                + "return new Promise(resolve => setTimeout(resolve, 100));",
                adapter);
        assertSingleInputUpdates(adapter, "second");
    }

    private void assertSingleInputUpdates(TestBenchElement adapter,
            String value) {
        Assert.assertEquals(
                "Adapter element with its own root must render exactly one "
                        + "React component",
                1, adapter.$("input").all().size());

        TestBenchElement input = adapter.$("input").first();
        input.clear();
        input.focus();
        input.sendKeys(value);

        Assert.assertEquals(value, adapter.getPropertyString("value"));
        Assert.assertEquals("State update must re-render the visible output",
                value, input.getPropertyString("value"));
    }

    private TestBenchElement getAdapterElement() {
        return $("react-input").first();
    }

    private TestBenchElement getReactElement() {
        return getAdapterElement().$("input").first();
    }

}
