/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */

package com.vaadin.flow.testonlynpmfeatures.defaulttheme;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.testutil.ChromeBrowserTest;
import com.vaadin.testbench.TestBenchElement;

public class DefaultThemeIT extends ChromeBrowserTest {

    @Test
    public void should_have_loaded_lumo_theme_and_associated_dependencies() {
        open();

        TestBenchElement head = $("head").first();

        List<TestBenchElement> customStyles = head.$("custom-style").all()
                .stream()
                .flatMap(customStyle -> customStyle.$("style").all().stream())
                .collect(Collectors.toList());

        // 8 from Lumo and 1 custom-style from frontend/styles/styles.js
        Assert.assertEquals("Should have found 9 custom-styles", 9,
                customStyles.size());
    }

    @Test
    public void theme_override_expected_last() {
        open();

        String lastStyle = $("head").first().$("custom-style").last().$("style")
                .first().getAttribute("innerText");

        Assert.assertTrue("Theme override expected last",
                lastStyle.contains("--lumo-primary-color: red"));
    }

    @Test
    public void clientSideImportIsThemed() {
        open();

        TestBenchElement button = $("template-with-client-side-imports").first()
                .$("vaadin-button").first();
        Assert.assertEquals("rgba(255, 0, 0, 1)", button.getCssValue("color"));
    }
}
