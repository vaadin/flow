/*
 * Copyright 2000-2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.quarkus.it.customservlet;

import io.quarkus.test.junit.QuarkusIntegrationTest;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.test.AbstractChromeIT;

/**
 * Verifies in a browser that an application whose Vaadin servlet is mapped to
 * {@code /app/*} loads its frontend bundle, handles events and receives Push
 * messages under that prefix.
 */
@QuarkusIntegrationTest
public class CustomServletIT extends AbstractChromeIT {

    @Override
    protected String getTestPath() {
        return "/app/";
    }

    @Test
    void click_handledByServerUnderPrefix() {
        open();
        waitForElementPresent(By.id(MainView.CLICK_BUTTON_ID));
        findElement(By.id(MainView.CLICK_BUTTON_ID)).click();
        waitUntil(driver -> MainView.CLICKED_TEXT.equals(
                findElement(By.id(MainView.CLICK_RESULT_ID)).getText()));
    }

    @Test
    void push_deliversUpdateUnderPrefix() {
        open();
        waitForElementPresent(By.id(MainView.PUSH_BUTTON_ID));
        findElement(By.id(MainView.PUSH_BUTTON_ID)).click();
        waitUntil(driver -> MainView.PUSHED_TEXT
                .equals(findElement(By.id(MainView.PUSH_RESULT_ID)).getText()));
    }
}
