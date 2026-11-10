/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.example.application;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.AnchorElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import static org.junit.Assert.assertTrue;

public class MainViewIT extends ChromeBrowserTest {

    @Test
    public void open_injectedServiceUsed() {
        open();

        Assert.assertEquals("Hello from a Spring bean",
                $(SpanElement.class).id(MainView.GREETING_ID).getText());
    }

    @Test
    public void open_layoutPostConstructCalled() {
        open();

        Assert.assertEquals("Layout initialized",
                $(SpanElement.class).id(MainLayout.INITIALIZED_ID).getText());
    }

    @Test
    public void open_menuEntriesListed() {
        open();

        Assert.assertEquals("Binder",
                $(SpanElement.class).id(MainView.MENU_ID).getText());
    }

    @Test
    public void clickRouterLink_targetViewShown() {
        open();

        $(AnchorElement.class).id(MainView.BINDER_LINK_ID).click();

        assertTrue($(NativeButtonElement.class).id(BinderView.SAVE_ID)
                .isDisplayed());
    }

    @Override
    protected String getTestPath() {
        return "/";
    }
}
