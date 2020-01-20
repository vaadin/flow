/*
 * Copyright 2000-2018 Vaadin Ltd.
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

package com.vaadin.flow.webcomponent;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class NpmOnlyIndexIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return Constants.PAGE_CONTEXT + "/index.html";
    }

    // test for #7005
    @Test
    public void globalStylesAreUnderTheWebComponent() {
        open();

        waitForElementVisible(By.tagName("themed-web-component"));

        TestBenchElement webComponent = $("themed-web-component").first();

        List<TestBenchElement> styles = webComponent.$("style").all();
        System.out.println(styles.size());

        // getAttribute wouldn't work, so we are counting the elements
        Assert.assertEquals(2, styles.size());
    }
}
