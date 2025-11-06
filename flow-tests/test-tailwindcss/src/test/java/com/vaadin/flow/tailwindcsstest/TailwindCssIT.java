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
package com.vaadin.flow.tailwindcsstest;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class TailwindCssIT extends ChromeBrowserTest {

    @Test
    public void tailwindCssWorks_builtin() {
        var view = openView();
        String viewBackground = view.getCssValue("backgroundColor");
        Assert.assertEquals("oklch(0.967 0.003 264.542)", viewBackground);

        var h1 = view.findElement(By.tagName("h1"));
        Assert.assertEquals("Tailwind CSS does work!", h1.getText());
    }

    private WebElement openView() {
        open();
        waitForDevServer();
        return findElement(By.cssSelector(".bg-gray-100"));
    }
}
