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

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.testbench.TestBenchElement;

public class DoubleNpmAnnotationIT extends AbstractSpringTest {

    @Test
    public void bothPaperWebComponentsAreLoaded() throws Exception {
        open();
        List<TestBenchElement> paperCheckboxes = $("paper-checkbox").all();
        List<TestBenchElement> paperInputs = $("paper-input").all();

        // check that elements are on the page
        Assert.assertTrue("Should have found a 'paper-checkbox'",
                paperCheckboxes.size() > 0);
        Assert.assertTrue("Should have found a 'paper-input'",
                paperInputs.size() > 0);

        // verify that the paper components are upgraded
        Assert.assertNotNull(
                "'paper-checkbox' should have had element in shadow dom",
                paperCheckboxes.get(0).$("checkboxContainer"));
        Assert.assertNotNull(
                "'paper-input' should have had element in shadow dom",
                paperInputs.get(0).$("paper-input-container"));
    }

    @Override
    protected String getTestPath() {
        return "/double-npm-annotation";
    }
}
