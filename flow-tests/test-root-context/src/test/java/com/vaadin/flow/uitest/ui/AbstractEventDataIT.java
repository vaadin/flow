/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.uitest.ui;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public abstract class AbstractEventDataIT extends ChromeBrowserTest {

    protected void clickAndVerifyTarget(String id) {
        final WebElement element = findElement(By.id(id));
        element.click();

        verifyEventTargetString(id);
    }

    protected abstract void verifyEventTargetString(String text);

}
