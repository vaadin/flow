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
package com.vaadin.flow.uitest.ui.template;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class SetInitialTextIT extends ChromeBrowserTest {

    @Test
    public void setText_updateDomViaClientSide_updateElementViaServerSide_allElementsArePreserved() {
        open();

        TestBenchElement template = $(TestBenchElement.class)
                .id("set-initial-text");
        // add a child via client side
        template.$(TestBenchElement.class).id("addClientSideChild").click();

        // add a child via sever side
        template.$(TestBenchElement.class).id("add-child").click();

        // Both children should be now in DOM

        TestBenchElement child = template.$(TestBenchElement.class).id("child");

        Assert.assertTrue(child.$(TestBenchElement.class)
                .attribute("id", "client-side").exists());
        Assert.assertTrue(child.$(TestBenchElement.class)
                .attribute("id", "new-child").exists());
    }

}
