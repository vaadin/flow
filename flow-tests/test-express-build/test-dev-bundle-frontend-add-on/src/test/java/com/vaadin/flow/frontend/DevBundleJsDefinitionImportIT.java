/*
 * Copyright 2000-2026 Vaadin Ltd.
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
import org.openqa.selenium.support.ui.ExpectedConditions;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class DevBundleJsDefinitionImportIT extends ChromeBrowserTest {

    @Test
    public void importedModules_loadedFromTheBundle() {
        open();

        findElement(By.id(DevBundleJsDefinitionImportView.RUN_ID)).click();

        waitUntil(ExpectedConditions.textToBe(
                By.id(DevBundleJsDefinitionImportView.STATUS_ID),
                "add-on project imported"));
    }

    @Test
    public void importedModules_hashCalculated() throws IOException {
        open();
        waitForElementPresent(By.id(DevBundleJsDefinitionImportView.RUN_ID));
        ObjectNode frontendHashes = DevBundleCssImportIT.getFrontendHashes();
        Assert.assertTrue(
                "content hash of the project file a definition imports is expected",
                frontendHashes.has("js/definition-import.js"));
        Assert.assertTrue(
                "content hash of the jar file a definition imports is expected",
                frontendHashes.has("definition-import-addon.js"));
    }
}
