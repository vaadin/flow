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
package com.vaadin.flow.spring.test.routescope;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.spring.test.AbstractSpringTest;

public class ErrorParameterIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/throw-exception";
    }

    @Test
    public void navigateToViewWhichThrows_beansInsideErrorViewArePreservedinScope() {
        open();

        Assert.assertTrue(isElementPresent(By.id("custom-exception-created")));
        Assert.assertTrue(
                isElementPresent(By.id("custom-exception-destroyed")));

        WebElement button = findElement(By.id("custom-exception-button"));
        String buttonId = button.getText();

        switchContent();

        WebElement div = findElement(By.id("custom-exception-div"));
        String divId = div.getText();

        switchContent();

        Assert.assertEquals("Button sub component is not preserved", buttonId,
                findElement(By.id("custom-exception-button")).getText());

        switchContent();

        Assert.assertEquals("Div sub component is not preserved", divId,
                findElement(By.id("custom-exception-div")).getText());
    }

    private void switchContent() {
        findElement(By.id("switch-content")).click();
    }

}
