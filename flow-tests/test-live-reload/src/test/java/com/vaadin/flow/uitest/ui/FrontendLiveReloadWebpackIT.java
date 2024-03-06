/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.Component;
import com.vaadin.testbench.TestBenchElement;

@NotThreadSafe
@Ignore("Webpack specific Test")
public class FrontendLiveReloadWebpackIT extends FrontendLiveReloadIT {

    @Override
    protected Class<? extends Component> getViewClass() {
        return FrontendLiveReloadView.class;
    }

    protected By errorBoxSelector() {
        return By.className("v-system-error");
    }
}
