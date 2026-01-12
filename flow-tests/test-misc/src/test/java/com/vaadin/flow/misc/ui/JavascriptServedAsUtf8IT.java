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
package com.vaadin.flow.misc.ui;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class JavascriptServedAsUtf8IT extends ChromeBrowserTest {

    @Test
    public void loadJavascriptWithUtf8() {
        getDriver().get(getRootURL() + "/frontend/test-files/js/unicode.js");
        String source = getDriver().getPageSource();
        Assert.assertTrue(
                "Page should have contained umlaut characters but contained: "
                        + source,
                source.contains("åäöü°"));
    }
}
