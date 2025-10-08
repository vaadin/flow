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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.SpanElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;

// IT for https://github.com/vaadin/flow/issues/12356
public class PreserveOnRefreshNestedBeforeEnterIT extends ChromeBrowserTest {

    @Test
    public void refreshViewWithNestedLayouts_eachBeforeEnterIsCalledOnlyOnce() {
        open();

        Assert.assertEquals("1", $(SpanElement.class)
                .id("RootLayout-before-enter-count").getText());
        Assert.assertEquals("1", $(SpanElement.class)
                .id("NestedLayout-before-enter-count").getText());
        Assert.assertEquals("1", $(SpanElement.class)
                .id("PreserveOnRefreshNestedBeforeEnterView-before-enter-count")
                .getText());

        open();

        Assert.assertEquals("2", $(SpanElement.class)
                .id("RootLayout-before-enter-count").getText());
        Assert.assertEquals("2", $(SpanElement.class)
                .id("NestedLayout-before-enter-count").getText());
        Assert.assertEquals("2", $(SpanElement.class)
                .id("PreserveOnRefreshNestedBeforeEnterView-before-enter-count")
                .getText());
    }
}
