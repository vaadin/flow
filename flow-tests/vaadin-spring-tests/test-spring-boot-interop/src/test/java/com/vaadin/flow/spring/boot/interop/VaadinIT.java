package com.vaadin.flow.spring.boot.interop;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.spring.test.AbstractSpringTest;

public class VaadinIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/";
    }

    @Test
    public void vaadinUIShown() {
        open();
        $(NativeButtonElement.class).first().click();
        Assert.assertEquals("Hello", $(DivElement.class).id("output").getText());
    }
}
