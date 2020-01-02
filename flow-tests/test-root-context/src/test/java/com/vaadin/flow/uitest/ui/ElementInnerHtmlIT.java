/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.DivElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ElementInnerHtmlIT extends ChromeBrowserTest {

    @Test
    public void elementInitOrder() {
        open();
        DivElement innerHtml = $(DivElement.class).id("inner-html-field");

        Assert.assertEquals("", innerHtml.getPropertyString("innerHTML"));

        $(NativeButtonElement.class).id("set-foo").click();
        Assert.assertEquals("<p>Foo</p>", innerHtml.getPropertyString("innerHTML"));

        $(NativeButtonElement.class).id("set-foo").click();
        Assert.assertEquals("<p>Foo</p>", innerHtml.getPropertyString("innerHTML"));

        $(NativeButtonElement.class).id("set-boo").click();
        Assert.assertEquals("<p>Boo</p>", innerHtml.getPropertyString("innerHTML"));

        $(NativeButtonElement.class).id("set-boo").click();
        Assert.assertEquals("<p>Boo</p>", innerHtml.getPropertyString("innerHTML"));

        $(NativeButtonElement.class).id("set-null").click();
        Assert.assertEquals("", innerHtml.getPropertyString("innerHTML"));

    }
}
