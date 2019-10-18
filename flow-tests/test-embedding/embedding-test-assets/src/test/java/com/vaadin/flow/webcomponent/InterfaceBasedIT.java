/*
 * Copyright 2000-2018 Vaadin Ltd.
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
package com.vaadin.flow.webcomponent;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class InterfaceBasedIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return Constants.PAGE_CONTEXT + "/interfaceBased.html";
    }

    @Test
    public void webComponentExportedByExportsWebComponentInterfaceShouldBeDisplayedAndUpgraded() {
        open();

        waitForElementVisible(By.id("interface"));
        WebElement webComponent = findElement(By.id("interface"));
        WebElement paragraph = webComponent.findElement(By.id("paragraph"));

        Assert.assertNotNull("Correct tag should have been found",
                webComponent);
        Assert.assertNotNull("Web component exported by interface based "
                + "exporter should have been upgraded", paragraph);
    }
}
