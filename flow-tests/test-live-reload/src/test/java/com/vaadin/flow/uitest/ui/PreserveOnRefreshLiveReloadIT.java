/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import com.vaadin.testbench.TestBenchElement;
import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.WebElement;

@NotThreadSafe
public class PreserveOnRefreshLiveReloadIT extends AbstractLiveReloadIT {

    @Test
    public void notificationShownWhenLoadingPreserveOnRefreshView() {
        open();

        TestBenchElement liveReload = $("vaadin-devmode-gizmo").first();
        Assert.assertNotNull(liveReload);
        WebElement messageDetails = liveReload.$("*")
                .attributeContains("class", "warning").first();
        Assert.assertTrue(
                messageDetails.getText().contains("@PreserveOnRefresh"));
    }

}
