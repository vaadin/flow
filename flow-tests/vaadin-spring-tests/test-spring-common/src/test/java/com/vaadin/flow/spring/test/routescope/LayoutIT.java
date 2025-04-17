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

public class LayoutIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/div-in-layout";
    }

    @Test
    public void divInLayoutComponentIsPreserved_buttonInLayoutIsNotPreserved_serviceIsPreserved_noOwnerBeanIsNotPreserved() {
        open();

        WebElement divId = findElement(By.id("div-id"));
        String id = divId.getText();

        String serviceCall = findElement(By.id("service-info")).getText();

        // navigate to the button view
        navigateTo("button-link");

        String serviceCallInsideButtonView = findElement(By.id("route-service"))
                .getText();

        Assert.assertEquals("Service call in the " + DivInLayout.class
                + " view doesn't match to the call before navigation happens",
                serviceCall, serviceCallInsideButtonView);

        String buttonViewId = findElement(By.id("component-id")).getText();

        String noOwnerBeanCall = findElement(By.id("no-owner-bean")).getText();

        Assert.assertEquals(
                "Only one bean " + ButtonScopedBean.class
                        + " instance is expected",
                String.valueOf(1),
                findElement(By.id("button-scoped-bean-count")).getText());

        // navigate back to the div view
        navigateTo("div-link");

        Assert.assertEquals("Service call in the " + DivInLayout.class
                + " view doesn't match to the call before navigation happens",
                serviceCall, findElement(By.id("service-info")).getText());

        Assert.assertEquals(DivInLayout.class + " instance is not preserved",
                id, findElement(By.id("div-id")).getText());

        Assert.assertTrue(
                isElementPresent(By.id("button-scoped-bean-destroy")));

        // navigate to the button view again
        navigateTo("button-link");

        Assert.assertNotEquals(
                "Button view should not be preserved on navigation",
                buttonViewId, findElement(By.id("component-id")).getText());
        Assert.assertNotEquals(
                "No owner bean should not be preserved on navigtaion",
                noOwnerBeanCall, findElement(By.id("no-owner-bean")).getText());

        Assert.assertEquals(
                "Only one bean " + ButtonScopedBean.class
                        + " instance is expected",
                String.valueOf(1),
                findElement(By.id("button-scoped-bean-count")).getText());
    }

    @Test
    public void navigateFromButtonViewToInvalidRouteScopeUsage_throws() {
        open();

        // navigate to the button view
        navigateTo("button-link");

        // navigate to the invalid route
        navigateTo("invalid-route-link");

        // in dev mode there will be an exception shown, production mode should
        // not show an exception but the view should not be rendered
        Assert.assertFalse(isElementPresent(By.id("invalid-bean")));
    }

    private void navigateTo(String linkId) {
        findElement(By.id(linkId)).click();
    }
}
