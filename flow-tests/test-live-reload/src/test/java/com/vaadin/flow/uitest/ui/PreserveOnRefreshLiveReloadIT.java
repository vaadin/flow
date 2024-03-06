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

import com.vaadin.flow.testutil.DevToolsElement;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

@NotThreadSafe
public class PreserveOnRefreshLiveReloadIT extends AbstractLiveReloadIT {

    @Test
    public void notificationShownWhenLoadingPreserveOnRefreshView() {
        open();

        DevToolsElement liveReload = $(DevToolsElement.class).waitForFirst();
        WebElement messageDetails = liveReload.$("*")
                .attributeContains("class", "warning").first();
        Assert.assertTrue(
                messageDetails.getText().contains("@PreserveOnRefresh"));
    }

    @Test
    public void viewIsPreservedOnLiveReload() {
        open();

        String instanceId0 = findElement(
                By.id(AbstractLiveReloadView.INSTANCE_IDENTIFIER)).getText();
        WebElement liveReloadTrigger = findElement(
                By.id(JavaLiveReloadView.JAVA_LIVE_RELOAD_TRIGGER_BUTTON));
        liveReloadTrigger.click();

        // when: the page is reloaded
        waitForLiveReload();

        // then: the same instance is rendered
        String instanceId1 = findElement(
                By.id(AbstractLiveReloadView.INSTANCE_IDENTIFIER)).getText();
        Assert.assertEquals(instanceId0, instanceId1);
    }
}
