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

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testcategory.IgnoreOSGi;
import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * @author Vaadin Ltd
 * @since 1.0
 *
 */
@Category(IgnoreOSGi.class)
public class ServiceInitListenersIT extends ChromeBrowserTest {

    @Test
    public void testServiceInitListenerTriggered() {
        open();

        List<WebElement> labels = findElements(By.tagName("label"));
        Assert.assertNotEquals(labels.get(0).getText(), 0,
                extractCount(labels.get(0).getText()));
        Assert.assertNotEquals(labels.get(1).getText(), 0,
                extractCount(labels.get(1).getText()));
        Assert.assertNotEquals(labels.get(2).getText(), 0,
                extractCount(labels.get(2).getText()));
    }

    private int extractCount(String logRow) {
        // Assuming row pattern is "label: 1"
        String substring = logRow.replaceAll("[^:]*:\\s*", "");
        return Integer.parseInt(substring);
    }
}
