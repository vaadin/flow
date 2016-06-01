/*
 * Copyright 2000-2016 Vaadin Ltd.
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
package com.vaadin.hummingbird.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.hummingbird.testutil.PhantomJSTest;

public class TemplateIncludeIT extends PhantomJSTest {

    @Test
    public void ensureCorrectDom() {
        open();
        WebElement root = findElement(By.id("root"));
        String outerHtml = root.getAttribute("outerHTML");
        String expected = "<div id=\"root\">" //
                + "<div id=\"header\"> <span>Menu item 1</span> <span>Menu item 2</span> <span>Menu item 3</span> </div>" //
                + "<div id=\"content\">Here goes the content</div>" //
                + "<div id=\"footer\"> <span>Footer goes here</span> </div></div>";
        Assert.assertEquals(expected, outerHtml);
    }
}
