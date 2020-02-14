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
package com.vaadin.flow.uitest.ui.material;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;
import org.junit.Assert;
import org.junit.Test;

public class MaterialThemedTemplateIT extends ChromeBrowserTest {

    @Test
    public void materialThemeUsed_themedTemplateAndThemeResourcesLoaded() {
        open();

        // check that all imported templates are available in the DOM
        TestBenchElement template = $("material-themed-template").first();

        TestBenchElement div = template.$("div").first();

        Assert.assertEquals("Material themed Template", div.getText());

        // this is silly, but a concrete way to test that the material files are
        // imported by verifying that the material css variables introduced in the
        // files work
        Assert.assertEquals("color variable not applied", "rgba(176, 0, 32, 1)",
                div.getCssValue("color"));
        Assert.assertEquals("typography variable not applied","16px", div.getCssValue("font-size"));

    }

}
