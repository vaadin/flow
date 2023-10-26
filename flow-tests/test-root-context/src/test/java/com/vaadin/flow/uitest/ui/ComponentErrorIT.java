package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ComponentErrorIT extends ChromeBrowserTest {

    @Test
    public void verify_that_throwing_component_is_present() {
        open();

        $(NativeButtonElement.class).id("throw").click();

        Assert.assertEquals("true",
                $(SpanElement.class).id("present").getText());
        Assert.assertEquals("com.vaadin.flow.component.html.NativeButton",
                $(SpanElement.class).id("name").getText());

    }
}
