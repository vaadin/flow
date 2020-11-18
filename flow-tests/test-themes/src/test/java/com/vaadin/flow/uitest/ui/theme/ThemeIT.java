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

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

import static com.vaadin.flow.uitest.ui.theme.ThemeView.MY_FIELD_ID;

public class ThemeIT extends ChromeBrowserTest {

    @Test
    public void secondTheme_staticFilesNotCopied() {
        getDriver().get(getRootURL() + "/theme/app-theme/img/bg.jpg");
        Assert.assertFalse("app-theme static files should be copied",
            driver.getPageSource().contains("Could not navigate"));

        getDriver().get(getRootURL() + "/theme/no-copy/no-copy.txt");
        Assert.assertTrue("no-copy theme should not be handled",
            driver.getPageSource()
                .contains("Could not navigate to 'theme/no-copy/no-copy.txt'"));
    }

    @Test
    public void applicationTheme_GlobalCss_isUsed() {
        open();
        // No exception for bg-image should exist
        checkLogsForErrors();

        Assert.assertEquals(
            "url(\"" + getRootURL() + "/theme/app-theme/img/bg.jpg\")",
            findElement(By.tagName("body")).getCssValue("background-image"));

        getDriver().get(getRootURL() + "/theme/app-theme/img/bg.jpg");
        Assert.assertFalse("app-theme background file should be served",
            driver.getPageSource().contains("Could not navigate"));
    }

    @Test
    public void componentTheme_isApplied() {
        open();
        final TestBenchElement myField = $(TestBenchElement.class)
            .id(MY_FIELD_ID);
        final TestBenchElement input = myField.$(TestBenchElement.class)
            .id("input");
        Assert.assertEquals("rgba(255, 0, 0, 1)",
            input.getCssValue("background-color"));
    }

    @Override
    protected String getTestPath() {
        String path = super.getTestPath();
        String view = "view/";
        return path.substring(view.length());
    }

}
