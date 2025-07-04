package com.vaadin.viteapp;

import java.util.List;

import org.junit.Assert;
import org.junit.Test;

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

        Assert.assertEquals("Theme rule should have been added once in total",
                1, adoptedStyleSheetsWithString.size()
                        + styleTagsWithString.size());
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

    // Feature flag should not override theme.json setting
    @Test
    public void autoInjectComponentsIsFalse_cssNotImported() {
        String fieldBorder = (String) executeScript(
                "return getComputedStyle(document.querySelector('#themedfield').shadowRoot.querySelector('[part=input-field]')).border");
        Assert.assertNotEquals("10px solid rgb(255, 0, 0)", fieldBorder);
    }

    @Test
    public void documentCssImport_externalUrlLoaded() {
        checkLogsForErrors();
        waitForFont("10px Itim");
    }

}
