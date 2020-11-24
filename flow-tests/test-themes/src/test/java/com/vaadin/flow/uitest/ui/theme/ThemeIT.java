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
package com.vaadin.flow.uitest.ui.theme;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ThemeIT extends ChromeBrowserTest {

    @Test
    public void secondTheme_staticFilesNotCopied() {
        getDriver().get(getRootURL() + "/VAADIN/static/img/bg.jpg");
        Assert.assertFalse("app-theme static files should be copied",
            driver.getPageSource().contains("HTTP ERROR 404"));

        getDriver().get(getRootURL() + "/VAADIN/static/no-copy.txt");
        System.out.println(driver.getPageSource());
        Assert.assertTrue("no-copy theme should not be handled",
            driver.getPageSource().contains("HTTP ERROR 404"));
    }

    @Test
    public void applicationTheme_GlobalCss_isUsed() {
        open();
        // No exception for bg-image should exist
        checkLogsForErrors();

        final WebElement body = findElement(By.tagName("body"));
        Assert.assertEquals(
            "url(\"" + getRootURL() + "/VAADIN/static/img/bg.jpg\")",
            body.getCssValue("background-image"));

        Assert.assertEquals("Ostrich", body.getCssValue("font-family"));

        getDriver().get(getRootURL() + "/VAADIN/static/img/bg.jpg");
        Assert.assertFalse("app-theme background file should be served",
            driver.getPageSource().contains("Could not navigate"));
    }

    @Test
    public void subCssWithRelativePath_urlPathIsNotRelative() {
        open();
        checkLogsForErrors();

        Assert.assertEquals("Imported css file URLs should have been handled.",
            "url(\"" + getRootURL() + "/VAADIN/static/icons/archive.png\")",
            $(SpanElement.class).id("sub-component")
                .getCssValue("background-image"));
    }

    @Override
    protected String getTestPath() {
        String path = super.getTestPath();
        String view = "view/";
        return path.substring(view.length());
    }

}
