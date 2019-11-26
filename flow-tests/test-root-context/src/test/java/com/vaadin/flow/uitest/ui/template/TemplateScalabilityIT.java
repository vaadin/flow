package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;
import com.vaadin.testbench.elementsbase.Element;

public class TemplateScalabilityIT extends ChromeBrowserTest {

    @Test
    public void openPage_allButtonsRenderSuccessfully() {
        open();

        waitUntil(input -> {
            return !findInShadowRoot(
                    input.findElement(By.id("scalability-view")),
                    By.id(TemplateScalabilityView.COMPLETED)).isEmpty();
        });

        WebElement viewTemplate = getDriver()
                .findElement(By.id("scalability-view"));
        int buttons = findInShadowRoot(viewTemplate,
                By.tagName("template-scalability-panel")).size();

        Assert.assertEquals("Template should have created "
                        + TemplateScalabilityView.NUM_ITEMS + " panels with buttons.",
                TemplateScalabilityView.NUM_ITEMS, buttons);

        checkLogsForErrors();
    }

    @Element("template-scalability-panel")
    public class ScalabilityPanelElement extends TestBenchElement {

    }

    @Element("template-scalability-view")
    public class ScalabilityViewElement extends TestBenchElement {

    }
}
