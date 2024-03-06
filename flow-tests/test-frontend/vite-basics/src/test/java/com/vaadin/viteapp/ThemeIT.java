/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.viteapp;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

public class ThemeIT extends ViteDevModeIT {

    @Test
    public void themeStylesShouldNotBeAddedToHead() {
        String styleFromTheme = "color: darkgreen";

        List<String> adoptedStyleSheetsWithString = (List<String>) executeScript(
                "return document.adoptedStyleSheets.map(sheet => sheet.cssRules).flatMap(rules => Array.from(rules).map(rule => rule.cssText)).filter(rule => rule.includes(arguments[0]))",
                styleFromTheme);
        List<String> styleTagsWithString = (List<String>) executeScript(
                "return Array.from(document.querySelectorAll('style')).map(style => style.textContent).filter(text => text.includes(arguments[0]))",
                styleFromTheme);

        Assert.assertEquals(
                "Theme rule should have been added once using adoptedStyleSheets",
                1, adoptedStyleSheetsWithString.size());
        Assert.assertEquals("Theme rule should not have been added to <head>",
                0, styleTagsWithString.size());
    }

    @Test
    public void cssImportAnnotation() {
        String bodyBackground = (String) executeScript(
                "return getComputedStyle(document.body).backgroundColor");
        Assert.assertEquals("rgb(211, 211, 211)", bodyBackground);
    }

    @Test
    public void cssImportAnnotationForComponent() {
        String fieldBackground = (String) executeScript(
                "return getComputedStyle(document.querySelector('#themedfield')).backgroundColor");
        Assert.assertEquals("rgb(173, 216, 230)", fieldBackground);
    }

    @Test
    public void documentCssImport_externalAddedToHeadAsLink() {
        checkLogsForErrors();

        final WebElement documentHead = getDriver()
                .findElement(By.tagName("head"));
        final List<WebElement> links = documentHead
                .findElements(By.tagName("link"));

        List<String> linkUrls = links.stream()
                .map(link -> link.getAttribute("href"))
                .collect(Collectors.toList());

        Assert.assertTrue("Missing link for external url", linkUrls
                .contains("https://fonts.googleapis.com/css?family=Itim"));
    }

}
