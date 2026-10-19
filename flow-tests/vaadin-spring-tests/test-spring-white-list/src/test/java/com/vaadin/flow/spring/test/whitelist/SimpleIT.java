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
package com.vaadin.flow.spring.test.whitelist;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.html.testbench.InputTextElement;
import com.vaadin.flow.component.html.testbench.NativeButtonElement;
import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class SimpleIT extends ChromeBrowserTest {
    @Before
    public void init() {
        open();
    }

    @Test
    public void simplePage_withWhiteList_works() {
        TestBenchElement viewElement = $("simple-view").first();
        NativeButtonElement button = viewElement.$(NativeButtonElement.class)
                .id("button");

        button.click();

        InputTextElement log = viewElement.$(InputTextElement.class).id("log");
        Assert.assertEquals(SimpleView.CLICKED_MESSAGE, log.getValue());
    }

    @Override
    protected String getTestPath() {
        return "/";
    }
}
