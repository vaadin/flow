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

public class RangeInputElementIT extends ChromeBrowserTest {

    private RangeInputElement input;
    private DivElement log;

    @Before
    public void open() {
        getDriver().get("http://localhost:8888/RangeInput");
        input = $(RangeInputElement.class).id("input");
        log = $(DivElement.class).id("log");
    }

    @Test
    public void getSetValue() {
        Assert.assertNull(input.getValue());
        input.setValue(5.0);
        Assert.assertEquals(5.0, (double) input.getValue(), 0.1);
        Assert.assertEquals("Value is '5.0'", log.getText());
    }

    @Test
    public void setValueEmpty() {
        input.setValue(6.0);
        input.setValue(null);
        Assert.assertNull(input.getValue());
        Assert.assertEquals("Value is 'null'", log.getText());
    }

    @Test
    public void clearEmpty() {
        input.clear();
        Assert.assertNull(input.getValue());
        Assert.assertEquals("", log.getText());
    }

    @Test
    public void clearWithValue() {
        input.setValue(7.0);
        input.clear();
        Assert.assertNull(input.getValue());
        Assert.assertEquals("Value is 'null'", log.getText());
    }
}
