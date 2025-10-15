/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class PolymerTemplateWithoutShadowRootIT extends ChromeBrowserTest {

    @Test
    public void componentMappedCorrectly() {
        open();
        DivElement content = $(DivElement.class).attribute("real", "deal")
                .first();
        Assert.assertEquals("Hello", content.getText());
        DivElement special = $(DivElement.class).id("special!#id");
        Assert.assertEquals("Special", special.getText());
        DivElement map = $(DivElement.class).id("map");
        Assert.assertEquals("Map", map.getText());
        content.click();
        Assert.assertEquals("Goodbye", content.getText());
    }
}
