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

import java.util.List;
import java.util.Optional;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class InfoIT extends ChromeBrowserTest {

    @Test
    @Ignore
    public void productionModeServlet() {
        openProduction();
        Assert.assertEquals("true", getInfoValue("Production mode"));

    }

    @Test
    public void nonProductionModeServlet() {
        open();
        Assert.assertEquals("false", getInfoValue("Production mode"));

    }

    private String getInfoValue(String string) {
        String prefix = string + ": ";
        List<WebElement> divs = findElement(By.className("infoContainer"))
                .findElements(By.tagName("div"));
        Optional<String> infoText = divs.stream().map(WebElement::getText)
                .filter(text -> text.startsWith(prefix)).findFirst();

        return infoText.get().replace(prefix, "");
    }
}
