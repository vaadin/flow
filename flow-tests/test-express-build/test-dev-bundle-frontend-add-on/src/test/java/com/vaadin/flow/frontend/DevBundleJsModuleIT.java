/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
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
