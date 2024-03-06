/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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