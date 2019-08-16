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
package com.vaadin.flow.uitest.ui.notheme;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class NoThemeComponentIT extends ChromeBrowserTest {

    @Test
    public void themeIsNotApplied() {
        open();

        List<WebElement> customStyles = $("head").first()
                .findElements(By.tagName("custom-style"));

        Assert.assertEquals(
                "Found custom style whereas @NoTheme should disable Lumo "
                        + "and not add any 'custom-theme' element",
                0, customStyles.size());

        String color = $("a").first().getCssValue("color");
        Assert.assertEquals(
                "Unexpected color for a link. "
                        + "@NoTheme should not theme a link anyhow.",
                "rgba(0, 0, 0, 1)", color);
    }

    @Override
    protected String getTestPath() {
        String path = super.getTestPath();
        String view = "view/";
        String result;
        if (path.startsWith("/")) {
            result = path.substring(view.length() + 1);
        }
        result = path.substring(view.length());
        return result;
    }
}
