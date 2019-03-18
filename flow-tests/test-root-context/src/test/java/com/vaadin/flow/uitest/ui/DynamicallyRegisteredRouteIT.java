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

package com.vaadin.flow.uitest.ui;

import java.util.List;

import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

@Category(IgnoreOSGi.class)
public class DynamicallyRegisteredRouteIT extends ChromeBrowserTest {

    @Test
    public void testServiceInitListener_canRegisterRoutes() {
        String testURL = getTestURL(getRootURL(), "/view/"
                + TestingServiceInitListener.DYNAMICALLY_REGISTERED_ROUTE,
                null);
        getDriver().get(testURL);

        List<WebElement> elements = findElements(
                By.id(DynamicallyRegisteredRoute.ID));

        Assert.assertEquals("Route registered during startup is not available",
                1, elements.size());
        Assert.assertEquals("Dynamically registered route not rendered",
                DynamicallyRegisteredRoute.TEXT, elements.get(0).getText());
    }
}
