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
package com.vaadin.flow.uitest.ui;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.PhantomJSTest;

public class CustomElementMappingIT extends PhantomJSTest {

    @Test
    public void testAddRemoveComponentDuringSameRequest() {
        open();
        List<WebElement> div = findElements(By.tagName("div"));
        Set<WebElement> custom = div.stream()
                .filter(element -> element.getAttribute("custom") != null)
                .collect(Collectors.toSet());

        Assert.assertFalse("Didn't find custom element data", custom.isEmpty());
    }
}
