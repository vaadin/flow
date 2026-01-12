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

public class ParentChildNoOwnerIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/parent-no-owner";
    }

    @Test
    public void beanWithNoOwner_preservedWithinTheSameRoutingChain() {
        open();

        WebElement parentInfo = findElement(By.id("parent-info"));
        String parentBeanId = parentInfo.getText();

        navigateTo("to-child");

        Assert.assertEquals(
                "Bean with RouteScope and no owner is not preserved during "
                        + "navigation with preserved routing chain component",
                parentBeanId, findElement(By.id("child-info")).getText());

        navigateTo("to-parent");

        Assert.assertEquals(
                "Bean with RouteScope and no owner is not preserved after returning back "
                        + "to the view which is preserved routing chain component",
                parentBeanId, findElement(By.id("parent-info")).getText());
    }

    private void navigateTo(String linkId) {
        findElement(By.id(linkId)).click();
    }
}
