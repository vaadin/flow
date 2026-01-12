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

import org.junit.Test;

import com.vaadin.testbench.TestBenchElement;

public class TemplatePushIT extends AbstractSpringTest {

    @Test
    public void elementChangesPushed() throws Exception {
        open();
        TestBenchElement tpl = $("template-push-view").first();
        tpl.$("button").id("elementTest").click();
        TestBenchElement label = tpl.$("label").id("label");
        waitUntil(foo -> {
            return "from Element API".equals(label.getText());
        }, 5);
    }

    @Test
    public void execJsPushed() throws Exception {
        open();
        TestBenchElement tpl = $("template-push-view").first();
        tpl.$("button").id("execJsTest").click();
        TestBenchElement label = tpl.$("label").id("label");
        waitUntil(foo -> {
            return "from execJS".equals(label.getText());
        }, 5);
    }

    @Test
    public void callFunctionPushed() throws Exception {
        open();
        TestBenchElement tpl = $("template-push-view").first();
        tpl.$("button").id("callFunctionTest").click();
        TestBenchElement label = tpl.$("label").id("label");
        waitUntil(foo -> {
            return "from callFunction".equals(label.getText());
        }, 5);
    }

    @Override
    protected String getTestPath() {
        return "/template-push";
    }
}
