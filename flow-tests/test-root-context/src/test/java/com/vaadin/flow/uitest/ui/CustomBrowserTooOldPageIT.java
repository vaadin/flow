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
import org.junit.Test;

import com.vaadin.flow.server.HandlerHelper.RequestType;
import com.vaadin.flow.testutil.ChromeBrowserTest;

public class CustomBrowserTooOldPageIT extends ChromeBrowserTest {

    @Test
    public void customPageUsed() {
        // There needs to be a session for the "too old page" to be shown
        getDriver().get(getRootURL() + "/view/");
        getDriver().get(getRootURL() + "/view/?v-r="
                + RequestType.BROWSER_TOO_OLD.getIdentifier());
        String pageSource = getDriver().getPageSource();
        Assert.assertTrue(
                pageSource.contains("You so old you cannot view this page"));
    }

}
