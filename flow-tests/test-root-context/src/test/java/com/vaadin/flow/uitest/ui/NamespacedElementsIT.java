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
package com.vaadin.flow.uitest.ui;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.flow.uitest.ui.*;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebElement;

public class NamespacedElementsIT extends ChromeBrowserTest {

    @Test
    public void checkNamespace() {
        open();

        String svgNamespace = getNameSpace(findElement(By.tagName("rect")));
        Assert.assertTrue("Namespace should contain svg",
                svgNamespace.toLowerCase().contains("svg"));

        String mathNamespace = getNameSpace(findElement(By.tagName("math")));
        Assert.assertTrue("Namespace should contain mathml",
                mathNamespace.toLowerCase().contains("mathml"));

        String paragraphNamespace = getNameSpace(findElement(By.tagName("p")));
        Assert.assertFalse("Default NS should not contain svg",
                paragraphNamespace.toLowerCase().contains("svg"));

    }

    private static String getNameSpace(WebElement element) {
        return element.getDomProperty("namespaceURI");
    }
}
