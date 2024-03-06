/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.contexttest.ui;

import org.junit.Assert;
import org.openqa.selenium.By;

public class RoutedContextIT extends AbstractContextIT {

    @Override
    protected String getAppContext() {
        // change by '/routed/' when https://github.com/vaadin/flow/issues/7583
        // is fixed
        return "/routed";
    }

    @Override
    protected void verifyCorrectUI() {
        Assert.assertNotNull(findElement(By.id("routed")));
    }

}
