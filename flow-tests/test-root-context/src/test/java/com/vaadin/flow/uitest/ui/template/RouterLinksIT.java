/*
 * Copyright 2000-2022 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.template;

import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.jupiter.ChromeBrowserTest;
import com.vaadin.testbench.BrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class RouterLinksIT extends ChromeBrowserTest {

    public static final String TEXT_INPUT = "abc";

    @BrowserTest
    @Disabled("Tests with V14 compatibility mode, not working with VITE")
    public void handleEventOnServer() {
        open();

        String originalUrl = getDriver().getCurrentUrl();
        WebElement textInput = $(TestBenchElement.class).id("template")
                .$(TestBenchElement.class).id("input");

        Assertions.assertTrue(textInput.getAttribute("value").isEmpty(),
                "Input was not empty");
        textInput.sendKeys(TEXT_INPUT);
        Assertions.assertEquals("Input was missing contents", TEXT_INPUT,
                textInput.getAttribute("value"));
        List<TestBenchElement> links = $(TestBenchElement.class).id("template")
                .$("a").all();
        WebElement link = links.stream()
                .filter(lnk -> lnk.getText().equals("Navigate")).findFirst()
                .get();

        getCommandExecutor().executeScript("return arguments[0].click()", link);

        // Original url should end with UI and the navigation link Template
        Assertions.assertNotEquals(originalUrl, getDriver().getCurrentUrl());

        textInput = $(TestBenchElement.class).id("template")
                .$(TestBenchElement.class).id("input");

        Assertions.assertEquals("Input didn't keep content through navigation",
                TEXT_INPUT, textInput.getAttribute("value"));
    }
}
