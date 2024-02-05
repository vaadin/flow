package com.vaadin.flow;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.testbench.TestBenchElement;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ReactAdapterIT extends ChromeBrowserTest {

    @Test
    public void validateInitialState() {
        open();

        waitForDevServer();

        Assert.assertEquals("initialValue", getReactElement().getPropertyString("value"));

        $(NativeButtonElement.class).id("getValueButton").click();
        Assert.assertEquals("initialValue", $(SpanElement.class).id("output").getText());
    }

    @Test
    public void validateSetState() {
        open();

        waitForDevServer();

        $(NativeButtonElement.class).id("setValueButton").click();

        Assert.assertEquals("setValue", getReactElement().getPropertyString("value"));
    }

    @Test
    public void validateGetState() {
        open();

        waitForDevServer();

        getReactElement().clear();
        getReactElement().focus();
        getReactElement().sendKeys("get value");

        $(NativeButtonElement.class).id("getValueButton").click();

        Assert.assertEquals("get value", $(SpanElement.class).id("getOutput").getText());
    }

    @Test
    public void validateListener() {
        open();

        waitForDevServer();

        getReactElement().clear();
        getReactElement().focus();
        getReactElement().sendKeys("listener value");

        Assert.assertEquals("listener value", $(SpanElement.class).id("listenerOutput").getText());
    }

    private TestBenchElement getAdapterElement() {
        return $("react-input").first();
    }

    private TestBenchElement getReactElement() {
        return getAdapterElement().$("input").first();
    }

}
