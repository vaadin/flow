/*
 * Copyright 2000-2025 Vaadin Ltd.
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

package com.vaadin.flow.custom;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.NoSuchElementException;

import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class CustomRouteIT extends ChromeBrowserTest {

    @Test
    public void CustomRegistry_hasExpectedErrorHandlers() {
        getDriver().get(getRootURL());

        final SpanElement notFoundException = $(SpanElement.class)
                .id("NotFoundException");
        Assert.assertEquals("Wrong error handler registered",
                "NotFoundException :: CustomNotFoundView",
                notFoundException.getText());

        try {
            $(SpanElement.class).id("IllegalAccessException");
            Assert.fail(
                    "Found IllegalAccessException error handler even though it should not be registered");
        } catch (NoSuchElementException nsee) {
            // NO-OP as this should throw element not found
        }
    }

    @Test
    public void testCustomErrorView() {
        getDriver().get(getRootURL() + "/none");
        final SpanElement error = $(SpanElement.class).id("error");
        Assert.assertEquals("Requested route was simply not found!",
                error.getText());
    }
}
