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
package com.vaadin.viteapp;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class BundlesIT extends ChromeBrowserTest {

    @Before
    public void openView() {
        getDriver().get(getRootURL() + "/my-context");
        waitForDevServer();
        getCommandExecutor().waitForVaadin();
    }

    @Test
    public void bundlesIsNotUsedWhenHasVersionMismatch() {
        Assert.assertFalse((Boolean) $("testscope-button").first()
                .getProperty("isFromBundle"));
    }

    @Test
    public void optimizeDepsNotExcludeBundleContents() {
        Assert.assertFalse(isExcluded("@vaadin/testscope-all"));
        Assert.assertFalse(isExcluded("@vaadin/testscope-button"));
    }

    private boolean isExcluded(String dependency) {
        return (Boolean) executeScript(
                "return (window.ViteConfigOptimizeDeps.exclude || []).includes(arguments[0]);",
                dependency);
    }

}
