/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import org.junit.Assert;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class InjectScriptTagIT extends ChromeBrowserTest {

    public void openPage_scriptIsEscaped() {
        open();

        WebElement parent = findElement(
                By.tagName("inject-script-tag-template"));

        WebElement div = findInShadowRoot(parent, By.id("value-div")).get(0);
        Assert.assertEquals("&lt;!-- &lt;script>", div.getText());

        WebElement slot = findInShadowRoot(parent, By.id("slot")).get(0);
        Assert.assertEquals(
                "<!-- &lt;script> --> &lt;!-- &lt;script>&lt;/script>",
                slot.getText());

        WebElement button = findInShadowRoot(parent, By.id("change-value"))
                .get(0);
        button.click();

        Assert.assertEquals("<!-- <SCRIPT>", div.getText());

        slot = findInShadowRoot(parent, By.id("slot")).get(0);
        Assert.assertEquals("<!-- <script> --> <!-- <script></script>",
                slot.getText());
    }

}
