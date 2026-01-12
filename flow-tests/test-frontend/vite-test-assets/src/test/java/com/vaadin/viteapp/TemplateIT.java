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
package com.vaadin.viteapp;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.vaadin.example.addon.AddonLitComponent;

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.viteapp.views.template.LitComponent;
import com.vaadin.viteapp.views.template.PolymerComponent;
import com.vaadin.viteapp.views.template.TemplateView;

public class TemplateIT extends ChromeBrowserTest {

    @Before
    public void openView() {
        getDriver().get(getRootURL() + "/" + TemplateView.ROUTE);
        waitForDevServer();
        getCommandExecutor().waitForVaadin();
    }

    @Test
    public void testElementIdMapping() {
        final String initialValue = "Default";

        SpanElement litSpan = $(LitComponent.TAG).first().$(SpanElement.class)
                .first();
        Assert.assertEquals(initialValue, litSpan.getText());

        SpanElement polymerSpan = $(PolymerComponent.TAG).first()
                .$(SpanElement.class).first();
        Assert.assertEquals(initialValue, polymerSpan.getText());

        SpanElement addonLitSpan = $(AddonLitComponent.TAG).first()
                .$(SpanElement.class).first();
        Assert.assertEquals(initialValue, addonLitSpan.getText());

        final String newLabel = "New label";
        $(InputTextElement.class).first().setValue(newLabel);
        $(NativeButtonElement.class).first().click();

        Assert.assertEquals(newLabel, litSpan.getText());
        Assert.assertEquals(newLabel, polymerSpan.getText());
        Assert.assertEquals(newLabel, addonLitSpan.getText());
    }

}
