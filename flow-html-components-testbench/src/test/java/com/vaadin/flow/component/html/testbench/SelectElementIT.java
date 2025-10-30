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
package com.vaadin.flow.component.html.testbench;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class SelectElementIT extends ChromeBrowserTest {

    private SelectElement input;
    private DivElement log;

    @Before
    public void open() {
        getDriver().get("http://localhost:8888/Select");
        input = $(SelectElement.class).id("input");
        log = $(DivElement.class).id("log");
    }

    @Test
    public void selectByText() {
        input.selectByText("Visible text 5");
        Assert.assertEquals("value5", input.getValue());
        Assert.assertEquals("Value is 'value5'", log.getText());
        input.selectByText("Visible text 1");
        Assert.assertEquals("value1", input.getValue());
        Assert.assertEquals("Value is 'value1'", log.getText());
    }

}
