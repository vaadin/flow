package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testcategory.IgnoreIE11;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ElementStyleIT extends ChromeBrowserTest {

    @Test
    @Category(IgnoreIE11.class)
    public void customPropertiesWork() {
        open();
        DivElement red = $(DivElement.class).id("red-border");
        DivElement green = $(DivElement.class).id("green-border");

        Assert.assertEquals(ElementStyleView.RED_BORDER, executeScript(
                "return getComputedStyle(arguments[0]).border", red));
        Assert.assertEquals(ElementStyleView.GREEN_BORDER, executeScript(
                "return getComputedStyle(arguments[0]).border", green));
    }
}
