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
package com.vaadin.flow.misc.ui;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.AnchorElement;
import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import static com.vaadin.flow.misc.ui.partial.MainLayout.EVENT_LOG_ID;
import static com.vaadin.flow.misc.ui.partial.MainLayout.RESET_ID;
import static com.vaadin.flow.misc.ui.partial.RootLayout.ROOT_EVENT_LOG_ID;
import static com.vaadin.flow.misc.ui.partial.SecondView.SECOND_ID;

@NotThreadSafe
public class PartialMatchRefreshIT extends ChromeBrowserTest {

    @Test
    public void whenUpdatingUrl_parentChainShouldBeReused() {
        open();

        Assert.assertEquals("1: RootLayout: constructor",
                $(DivElement.class).id(ROOT_EVENT_LOG_ID).getText());

        Assert.assertEquals("1: MainLayout: constructor",
                $(DivElement.class).id(EVENT_LOG_ID).getText());
        Assert.assertEquals("Main navigation link should be available", 1,
                $(AnchorElement.class).all().size());

        getDriver().get(getRootURL() + "/second");

        Assert.assertTrue("Couldn't find second view text div",
                $(DivElement.class).id(SECOND_ID).isDisplayed());

        Assert.assertEquals("1: RootLayout: constructor",
                $(DivElement.class).id(ROOT_EVENT_LOG_ID).getText());
        Assert.assertEquals("1: MainLayout: constructor",
                $(DivElement.class).id(EVENT_LOG_ID).getText());

    }

    @After
    public void tearDown() {
        $(NativeButtonElement.class).id(RESET_ID).click();
    }

    @Override
    protected String getTestPath() {
        return "/main";
    }

}
