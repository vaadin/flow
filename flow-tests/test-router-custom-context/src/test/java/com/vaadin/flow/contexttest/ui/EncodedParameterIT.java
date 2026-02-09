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
package com.vaadin.flow.contexttest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import static com.vaadin.flow.contexttest.ui.EncodedParameter.DECODED_CONTENT;
import static com.vaadin.flow.contexttest.ui.EncodedParameter.ENCODED_CONTENT;

public class EncodedParameterIT extends ChromeBrowserTest {

    static final String JETTY_CONTEXT = System.getProperty(
            "vaadin.test.jettyContextPath", "/custom-context-router");

    @Override
    protected String getTestPath() {
        return JETTY_CONTEXT + "/param/a%2bb";
    }

    @Test
    public void encodedUrlParameter_isNotUnencodedBeforeSetParameter() {
        open();
        waitForElementPresent(By.id(ENCODED_CONTENT));
        WebElement element = findElement(By.id(ENCODED_CONTENT));

        Assert.assertEquals(
                "Element parameter should be received with encoding.", "a%2bb",
                element.getText());

        element = findElement(By.id(DECODED_CONTENT));

        Assert.assertEquals("Element parameter should decode to +.", "a+b",
                element.getText());
    }

}
