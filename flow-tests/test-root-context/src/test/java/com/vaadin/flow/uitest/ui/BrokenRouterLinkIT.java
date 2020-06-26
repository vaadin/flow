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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

// https://github.com/vaadin/flow/issues/8544
public class BrokenRouterLinkIT extends ChromeBrowserTest {

    @Test
    public void testRouterLink_linkIsBroken_urlIsUpdated() {
        open();

        WebElement link = findElement(By.id(BrokenRouterLinkView.LINK_ID));

        String href = link.getAttribute("href");

        link.click();

        Assert.assertTrue(getDriver().getCurrentUrl().endsWith(href));

    }
}
