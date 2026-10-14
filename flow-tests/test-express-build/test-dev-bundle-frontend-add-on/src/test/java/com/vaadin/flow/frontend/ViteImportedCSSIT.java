/*
 * Copyright (C) 2000-2026 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.frontend;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.testutil.ChromeBrowserTest;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

public class ViteImportedCSSIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/view/";
    }

    @Test
    public void cssImportedByVite_availableInApp() throws IOException {
        open();
        WebElement body = $("body").first();

        Assert.assertEquals("rgba(173, 216, 230, 1)",
                body.getCssValue("background-color"));
    }
}
