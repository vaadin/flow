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
 *
 */

package com.vaadin.flow.ccdmtest;

import java.util.List;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class CCDMTest extends ChromeBrowserTest {

    protected void openTestUrl(String url) {
        getDriver().get(getRootURL() + "/foo" + url);
        waitForDevServer();
    }

    protected void openVaadinRouter() {
        openVaadinRouter("/");
    }

    protected void openVaadinRouter(String url) {
        openTestUrl(url);

        waitForElementPresent(By.id("loadVaadinRouter"));
        findElement(By.id("loadVaadinRouter")).click();
        waitForElementPresent(By.id("outlet"));
    }

    protected WebElement findAnchor(String href) {
        final List<WebElement> anchors = findElements(By.tagName("a"));
        for (WebElement element : anchors) {
            if (element.getAttribute("href").endsWith(href)) {
                return element;
            }
        }

        return null;
    }

}
