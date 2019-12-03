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
package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class ChangeInjectedComponentTextIT extends ChromeBrowserTest {

    @Test
    public void setText_injectedComponent_textReplacesContent() {
        open();

        WebElement injected = $("update-injected-component-text").first()
                .$(TestBenchElement.class).id("injected");
        Assert.assertEquals(
                "New text value doesn't replace the content of the element",
                "new text", injected.getText());
        Assert.assertEquals(
                "The 'setText()' method should remove all children from the injected component",
                0, injected.findElements(By.cssSelector("*")).size());
    }
}
