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
package com.vaadin.flow.uitest.ui.lumo;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;
import org.junit.Assert;
import org.junit.Test;

public abstract class AbstractThemedTemplateIT extends ChromeBrowserTest {

    @Test
    public void lumoThemeUsed_themedTemplateAndLumoThemeResourcesLoaded() {
        open();

        // check that all imported templates are available in the DOM
        TestBenchElement template = $(getTagName()).first();

        TestBenchElement div = template.$("div").first();

        Assert.assertEquals("Lumo themed Template", div.getText());

        // this is silly, but a concrete way to test that the lumo files are
        // imported by verifying that the lumo css variables introduced in the
        // files work
        Assert.assertEquals("color variables not applied", "rgba(255, 66, 56, 1)",
                div.getCssValue("color"));
        Assert.assertEquals("typography variables not applied","40px", div.getCssValue("font-size"));
        Assert.assertEquals("sizing variables not applied","36px solid rgb(0, 0, 0)",
                div.getCssValue("border"));
        Assert.assertEquals("spacing variables not applied","12px 24px", div.getCssValue("margin"));
        Assert.assertEquals("style variables not applied","20px", div.getCssValue("border-radius"));
        Assert.assertEquals("icons variables not applied","lumo-icons", div.getCssValue("font-family"));
    }

    protected abstract String getTagName();

    protected abstract String getThemedTemplate();

}
