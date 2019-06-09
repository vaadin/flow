/*
 * Copyright 2000-2019 Vaadin Ltd.
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

package com.vaadin.flow.testnpmonlyfeatures;

import java.util.List;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class ExternalJSModuleIT extends ChromeBrowserTest {
    @Before
    public void init() {
        open();
        waitForElementPresent(By.tagName("div"));
    }

    @Test
    public void jsModuleAnnotation_externalJs_shouldBeAddedToPage() {
        List<WebElement> scriptTags = findElements(By.tagName("script"));
        Assert.assertTrue(
                "External JS annotated with @JsModule annotation should be added as a script tag with module type to the page!",
                scriptTags.stream().anyMatch(
                        scriptTag -> ExternalJSModuleView.SOME_RANDOM_EXTERNAL_JS_MODULE_URL
                                .equals(scriptTag.getAttribute("src"))
                                && "module".equals(
                                        scriptTag.getAttribute("type"))));
    }
}
