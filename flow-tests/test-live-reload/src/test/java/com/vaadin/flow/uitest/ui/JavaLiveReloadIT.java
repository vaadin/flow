/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.DevToolsElement;

@NotThreadSafe
public class JavaLiveReloadIT extends AbstractLiveReloadIT {

    @Test
    public void deactivateLiveReload() {
        open();

        // given: live reload is deactivated
        DevToolsElement devTools = $(DevToolsElement.class).waitForFirst();

        devTools.setLiveReload(false);

        // when: live reload is triggered
        WebElement liveReloadTrigger = findElement(
                By.id(JavaLiveReloadView.JAVA_LIVE_RELOAD_TRIGGER_BUTTON));
        liveReloadTrigger.click();

        // then: page is not reloaded
        DevToolsElement liveReload2 = $(DevToolsElement.class).waitForFirst();
        WebElement devTools2 = liveReload2.$("*")
                .withAttributeContainingWord("class", "dev-tools").first();
        Assert.assertTrue(
                devTools2.getAttribute("class").contains("dev-tools"));
    }
}
