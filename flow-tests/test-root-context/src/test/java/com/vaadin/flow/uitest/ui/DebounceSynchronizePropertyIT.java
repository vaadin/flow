/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class DebounceSynchronizePropertyIT
        extends AbstractDebounceSynchronizeIT {
    private WebElement input;

    @Before
    public void setUp() {
        open();
        input = findElement(By.id("input"));
    }

    @Test
    public void eager() {
        toggleMode("eager");
        assertEager(input);

        toggleMode("eager");
        assertMessages("a", "ab");
    }

    @Test
    public void filtered() {
        toggleMode("filtered");

        input.sendKeys("a");
        assertMessages();

        input.sendKeys("b");
        assertMessages("ab");

        input.sendKeys("c");
        assertMessages("ab");

        input.sendKeys("d");
        assertMessages("ab", "abcd");
    }

    @Test
    public void debounce() throws InterruptedException {
        toggleMode("debounce");
        assertDebounce(input);
    }

    @Test
    @Ignore
    public void throttle() throws InterruptedException {
        toggleMode("throttle");
        assertThrottle(input);
    }

    private void toggleMode(String name) {
        findElement(By.id(name)).click();
    }

}
