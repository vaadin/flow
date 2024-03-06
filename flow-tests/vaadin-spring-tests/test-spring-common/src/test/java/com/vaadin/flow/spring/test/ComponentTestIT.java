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

import com.vaadin.flow.component.button.testbench.ButtonElement;
import com.vaadin.flow.component.notification.testbench.NotificationElement;

public class ComponentTestIT extends AbstractSpringTest {

    @Test
    public void componentsAreFoundAndLoaded() throws Exception {
        open();

        $(ButtonElement.class).waitForFirst();

        $(ButtonElement.class).first().click();

        Assert.assertTrue(
                "Clicking button should have opened a notification successfully.",
                $(NotificationElement.class).exists());
    }

    @Override
    protected String getTestPath() {
        return "/component-test";
    }

}
