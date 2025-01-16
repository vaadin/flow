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

public class BeansWithNoOwnerIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/beans-no-owner";
    }

    @Test
    public void beansWithNoOwner_preservedWithinTheSameRouteTarget_notPreservedAfterNavigation() {
        open();

        WebElement button = findElement(By.id("no-owner-button"));
        String buttonId = button.getText();

        switchContent();

        WebElement div = findElement(By.id("no-owner-div"));
        String divId = div.getText();

        switchContent();

        Assert.assertEquals("Button component is not preserved", buttonId,
                findElement(By.id("no-owner-button")).getText());

        switchContent();

        Assert.assertEquals("Div component is not preserved", divId,
                findElement(By.id("no-owner-div")).getText());

        navigateTo("navigate-another");

        Assert.assertNotEquals(
                "Div with RouteScope and no owner is preserved during "
                        + "navigation but it should not be preserved",
                divId, findElement(By.id("another-no-owner")).getText());

        navigateTo("no-owner-view");

        switchContent();

        Assert.assertNotEquals(
                "Div with RouteScope and no owner is preserved after returning back "
                        + "to the view but the new instance should be created",
                divId, findElement(By.id("no-owner-div")).getText());
    }

    private void switchContent() {
        findElement(By.id("switch-content")).click();
    }

    private void navigateTo(String linkId) {
        findElement(By.id(linkId)).click();
    }
}
