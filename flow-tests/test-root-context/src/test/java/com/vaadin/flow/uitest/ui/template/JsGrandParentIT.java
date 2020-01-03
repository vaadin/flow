/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import org.junit.Test;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class JsGrandParentIT extends ChromeBrowserTest {

    @Test
    public void callJsInsideGrandInjected() {
        open();

        TestBenchElement parent = $("js-grand-parent").first();
        TestBenchElement child = parent.$("js-sub-template").first();
        TestBenchElement grandChild = child.$("js-injected-grand-child")
                .first();
        WebElement label = grandChild.$(TestBenchElement.class).id("foo-prop");

        waitUntil(driver -> "bar".equals(label.getText()));
    }
}
