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
package com.vaadin.flow.spring.test;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.testbench.TestBenchElement;

public class ParentTemplateIT extends AbstractSpringTest {

    @Override
    protected String getTestPath() {
        return "/parent-template";
    }

    @Test
    public void customElementIsRegistered() throws Exception {
        open();

        TestBenchElement template = $("parent-template").first();
        TestBenchElement div = template.$("*").id("div");

        Assert.assertEquals("baz", div.getText());

        TestBenchElement child = template.$("*").id("child");

        Assert.assertEquals("bar", child.$("*").id("info").getText());
    }

    @Test
    public void injectedComponentIsSpringManaged() throws Exception {
        open();

        TestBenchElement template = $("parent-template").first();

        TestBenchElement child = template.$("*").id("child");

        Assert.assertEquals("foo", child.$("*").id("message").getText());
    }
}
