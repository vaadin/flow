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
package com.vaadin.flow.frontend;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import elemental.json.JsonObject;

public class DevBundleJsModuleIT extends ChromeBrowserTest {

    @Test
    public void frontendJsModules_hashCalculated() throws IOException {
        open();
        waitForElementPresent(By.id(DevBundleJsModuleView.SPAN_ID));
        JsonObject frontendHashes = DevBundleCssImportIT.getFrontendHashes();
        Assert.assertTrue("test.ts content hash is expected",
                frontendHashes.hasKey("test.ts"));
        Assert.assertTrue("js/test.js content hash is expected",
                frontendHashes.hasKey("js/test.js"));
        Assert.assertFalse("unknownfile.js content hash is not expected",
                frontendHashes.hasKey("unknownfile.js"));
    }

}
