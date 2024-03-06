/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.testutil;

import org.junit.After;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

/**
 * A base class for testing a view using TestBench. It opens the view and wait
 * for its root element to be present. It also checks errors in browser log at
 * both before and after phases of each test.
 *
 * @since 2.0
 */
public abstract class AbstractViewTest extends ChromeBrowserTest {
    private final By rootElementSelector;
    private WebElement rootElement;

    public AbstractViewTest(By rootElementSelector) {
        this.rootElementSelector = rootElementSelector;
    }

    @Before
    public void initialCheck() {
        open();

        waitForElementPresent(rootElementSelector);
        rootElement = findElement(rootElementSelector);

        checkLogsForErrors();
    }

    @After
    public void finalCheck() {
        checkLogsForErrors();
    }

    public WebElement getRootElement() {
        return rootElement;
    }
}
