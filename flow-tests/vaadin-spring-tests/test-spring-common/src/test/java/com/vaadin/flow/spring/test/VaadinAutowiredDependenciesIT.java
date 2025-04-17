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
