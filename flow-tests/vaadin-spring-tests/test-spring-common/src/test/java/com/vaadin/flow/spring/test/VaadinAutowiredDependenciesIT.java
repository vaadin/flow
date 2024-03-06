/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.spring.test;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class VaadinAutowiredDependenciesIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/vaadin-autowired-deps";
    }

    @Test
    public void injectedUI_uiIsTheSameAsCurrent() {
        open();

        WebElement uiInjected = findElement(By.id("ui-injected"));
        WebElement uiCurrent = findElement(By.id("ui-current"));

        Assert.assertEquals(
                "UI id and hashcode for injected UI instance are "
                        + "not the same as for current UI instance",
                uiCurrent.getText(), uiInjected.getText());
    }

    @Test
    public void injectedSession_uiIsTheSameAsCurrent() {
        open();

        WebElement sessionInjected = findElement(By.id("session-injected"));
        WebElement sessionCurrent = findElement(By.id("session-current"));

        Assert.assertEquals(
                "Session csrf token and hashcode for injected session instance are "
                        + "not the same as for current session instance",
                sessionCurrent.getText(), sessionInjected.getText());

    }
}
