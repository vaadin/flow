/*
 * Copyright 2000-2023 Vaadin Ltd.
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

package com.vaadin.flow;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.AnchorElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class NavigationIT extends ChromeBrowserTest {

    @Test
    public void testNavigation() {
        open();

        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.ANCHOR_ID).click();
        Assert.assertEquals("AnchorView",
                $(SpanElement.class).first().getText());
        $(AnchorElement.class).id(NavigationView.ANCHOR_ID).click();
        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(NativeButtonElement.class).id(NavigationView.SERVER_ID).click();
        Assert.assertEquals("ServerView",
                $(SpanElement.class).first().getText());
        $(NativeButtonElement.class).id(NavigationView.SERVER_ID).click();
        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());

        $(AnchorElement.class).id(NavigationView.ROUTER_LINK_ID).click();
        Assert.assertEquals("RouterView",
                $(SpanElement.class).first().getText());
        $(AnchorElement.class).id(NavigationView.ROUTER_LINK_ID).click();
        Assert.assertEquals("NavigationView",
                $(SpanElement.class).first().getText());
    }

}
