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

import com.vaadin.flow.spring.test.AbstractSpringTest;

public class PreserveOnRefreshIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/preserve-on-refresh";
    }

    @Test
    public void routeScopedBeanIsPreservedAfterViewRefresh() {
        open();

        String beanCall = findElement(By.id("preserve-on-refresh")).getText();

        // refresh
        getDriver().navigate().refresh();

        Assert.assertEquals("Bean is not preserved after refresh", beanCall,
                findElement(By.id("preserve-on-refresh")).getText());
    }
}
