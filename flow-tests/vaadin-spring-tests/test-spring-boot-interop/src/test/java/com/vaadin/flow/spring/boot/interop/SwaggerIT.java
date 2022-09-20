package com.vaadin.flow.spring.boot.interop;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.spring.test.AbstractSpringTest;

public class SwaggerIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/swagger-ui.html";
    }

    @Test
    public void swaggerUIShown() {
        open();
        Assert.assertTrue(
                getDriver().getPageSource().contains("OpenAPI definition"));
    }
}
