package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

public class PolymerTemplateWithoutShadowRootIT extends ChromeBrowserTest {

    @Test
    public void componentMappedCorrectly() {
        open();
        DivElement content = $(DivElement.class).id("content");
        Assert.assertEquals("Hello",content.getText());
        content.click();
        Assert.assertEquals("Goodbye",content.getText());
    }
}
