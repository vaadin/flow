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
package com.vaadin.flow.uitest.ui;

import org.junit.Assert;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public abstract class RouteNotFoundIT extends ChromeBrowserTest {
    /*
     * Original script: <img src=x
     * onerror=(function(){d=document.createElement("DIV");document.body.
     * appendChild(d);d.id="injected";})()>
     */
    protected static final String INJECT_ATTACK = "%3Cimg%20src%3Dx%20onerror"
            + "%3D%28function%28%29%7Bd%3Ddocument.createElement%28%22DIV%22%"
            + "29%3Bdocument.body.appendChild%28d%29%3Bd.id%3D%22injected%22%"
            + "3B%7D%29%28%29%3E";

    protected void assertPageHasRoutes(boolean contains) {
        String pageSource = getDriver().getPageSource();
        Assert.assertEquals(contains, pageSource.contains("Available routes"));
        Assert.assertEquals(contains, pageSource.contains("noParent"));
        Assert.assertEquals(contains, pageSource.contains("foo/bar"));
        // check that <img src=x onerror=...> did not inject div via script
        Assert.assertFalse(pageSource.contains("<div id=\"injected\"></div>"));
    }

}
