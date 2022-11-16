package com.vaadin.viteapp;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.testbench.BrowserTest;

public class ThemeIT extends ViteDevModeIT {

    @BrowserTest
    public void themeStylesShouldNotBeAddedToHead() {
        String styleFromTheme = "color: darkgreen";

        List<String> adoptedStyleSheetsWithString = (List<String>) executeScript(
                "return document.adoptedStyleSheets.map(sheet => sheet.cssRules).flatMap(rules => Array.from(rules).map(rule => rule.cssText)).filter(rule => rule.includes(arguments[0]))",
                styleFromTheme);
        List<String> styleTagsWithString = (List<String>) executeScript(
                "return Array.from(document.querySelectorAll('style')).map(style => style.textContent).filter(text => text.includes(arguments[0]))",
                styleFromTheme);

        Assertions.assertEquals(1, adoptedStyleSheetsWithString.size(),
                "Theme rule should have been added once using adoptedStyleSheets");
        Assertions.assertEquals(0, styleTagsWithString.size(),
                "Theme rule should not have been added to <head>");
    }

    @BrowserTest
    public void cssImportAnnotation() {
        String bodyBackground = (String) executeScript(
                "return getComputedStyle(document.body).backgroundColor");
        Assertions.assertEquals("rgb(211, 211, 211)", bodyBackground);
    }

    @BrowserTest
    public void cssImportAnnotationForComponent() {
        String fieldBackground = (String) executeScript(
                "return getComputedStyle(document.querySelector('#themedfield')).backgroundColor");
        Assertions.assertEquals("rgb(173, 216, 230)", fieldBackground);
    }

    @BrowserTest
    public void documentCssImport_externalAddedToHeadAsLink() {
        checkLogsForErrors();

        final WebElement documentHead = getDriver()
                .findElement(By.tagName("head"));
        final List<WebElement> links = documentHead
                .findElements(By.tagName("link"));

        List<String> linkUrls = links.stream()
                .map(link -> link.getAttribute("href"))
                .collect(Collectors.toList());

        Assertions.assertTrue(
                linkUrls.contains(
                        "https://fonts.googleapis.com/css?family=Itim"),
                "Missing link for external url");
    }

}
