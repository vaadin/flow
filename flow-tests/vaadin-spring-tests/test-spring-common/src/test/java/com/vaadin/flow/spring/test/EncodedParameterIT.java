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
package com.vaadin.flow.spring.test;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.DivElement;

import static com.vaadin.flow.spring.test.EncodedParameter.DECODED_CONTENT;
import static com.vaadin.flow.spring.test.EncodedParameter.ENCODED_CONTENT;

public class EncodedParameterIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/param/a%2bb";
    }

    @Test
    public void encodedUrlParameter_isNotUnencodedBeforeSetParameter() {
        open();
        waitForElementPresent(By.id(ENCODED_CONTENT));
        WebElement element = $(DivElement.class).id(ENCODED_CONTENT);

        Assert.assertEquals(
                "Element parameter should be received with encoding.", "a%2bb",
                element.getText());

        element = $(DivElement.class).id(DECODED_CONTENT);

        Assert.assertEquals("Element parameter should decode to +.", "a+b",
                element.getText());
    }

}
