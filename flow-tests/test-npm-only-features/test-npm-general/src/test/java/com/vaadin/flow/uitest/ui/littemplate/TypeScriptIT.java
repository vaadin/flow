/*
 * Copyright 2000-2021 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui.littemplate;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class TypeScriptIT extends ChromeBrowserTest {

    private TestBenchElement template;

    protected String getTemplateTag() {
        return "type-script-view";
    }

    public void setup() throws Exception {
        super.setup();
        open();
        template = $(getTemplateTag()).first();
    }

    @Test
    public void idMappingWorks() {
        NativeButtonElement mappedButton = template.$(NativeButtonElement.class)
                .id("mappedButton");

        Assert.assertEquals("Server button", mappedButton.getText());
        mappedButton.click();

        DivElement label = template.$(DivElement.class).id("label");
        Assert.assertEquals("Hello from server component event listener",
                label.getText());
    }
}
