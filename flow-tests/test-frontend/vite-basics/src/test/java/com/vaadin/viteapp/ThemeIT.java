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

        Assert.assertEquals("Theme rule should have been added once using adoptedStyleSheets", 1,
                adoptedStyleSheetsWithString.size());
        Assert.assertEquals("Theme rule should not have been added to <head>", 0, styleTagsWithString.size());
    }
}
