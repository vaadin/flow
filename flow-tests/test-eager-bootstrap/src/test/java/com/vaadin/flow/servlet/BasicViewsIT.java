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

package com.vaadin.flow.servlet;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BasicViewsIT extends ChromeBrowserTest {

    @Test
    public void rootViewShown() throws Exception {
        getDriver().get(getRootURL() + "/");
        waitForDevServer();
        Assert.assertEquals("This is the root view",
                $("*").id("view").getText());
    }

    @Test
    public void helloViewShown() throws Exception {
        getDriver().get(getRootURL() + "/hello");
        waitForDevServer();
        Assert.assertEquals("This is the Hello view",
                $("*").id("view").getText());
    }

    @Test
    public void invalidViewShowsNotFound() throws Exception {
        getDriver().get(getRootURL() + "/nonexistant");
        waitForDevServer();

        Assert.assertTrue(getDriver().getPageSource()
                .contains("Could not navigate to 'nonexistant'"));
    }

}
