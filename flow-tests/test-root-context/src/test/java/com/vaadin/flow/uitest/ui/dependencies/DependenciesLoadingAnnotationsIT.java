package com.vaadin.flow.uitest.ui.dependencies;

import static com.vaadin.flow.uitest.ui.dependencies.DependenciesLoadingAnnotationsUI.DOM_CHANGE_TEXT;
import static com.vaadin.flow.uitest.ui.dependencies.DependenciesLoadingAnnotationsUI.PRELOADED_DIV_ID;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.PhantomJSTest;

/**
 * A test that ensures correct order of dependencies loaded. Test corresponds to
 * {@link DependenciesLoadingAnnotationsUI}, which uses annotations to add
 * dependencies. There is a related pair of classes,
 * {@link DependenciesLoadingPageApiUI} and
 * {@link DependenciesLoadingPageApiIT}, designed to test exactly the same
 * dependency loading functionality (hence reusing all methods and constants),
 * but using {@link com.vaadin.ui.Page} api to add dependencies.
 *
 * @author Vaadin Ltd.
 * @see DependenciesLoadingBaseUI
 * @see DependenciesLoadingAnnotationsUI
 * @see DependenciesLoadingPageApiUI
 * @see DependenciesLoadingPageApiIT
 */
public class DependenciesLoadingAnnotationsIT extends PhantomJSTest {
    private static final String BLOCKING_PREFIX = "blocking.";
    private static final String NON_BLOCKING_PREFIX = "non-blocking.";

    @Test
    public void dependenciesLoadedAsExpectedWithAnnotationApi() {
        open();

        waitUntil(input -> input.findElements(By.className("dependenciesTest"))
                .size() == 5);

        ensureDependenciesHaveCorrectAttributes();

        WebElement preloadedDiv = findElement(By.id(PRELOADED_DIV_ID));
        Assert.assertEquals(
                "Non-blocking css should be loaded last: color should be blue",
                "rgba(0, 0, 255, 1)", preloadedDiv.getCssValue("color"));

        List<String> testMessages = findElements(
                By.className("dependenciesTest")).stream()
                        .map(WebElement::getText).collect(Collectors.toList());

        Assert.assertEquals(
                "5 elements are expected to be added: 2 for blocking dependencies, 1 for UI 'onAttach' method, 2 for non-blocking dependencies",
                5, testMessages.size());

        Assert.assertTrue("Blocking dependencies should be loaded first",
                testMessages.get(0).startsWith(BLOCKING_PREFIX));
        Assert.assertTrue("Blocking dependencies should be loaded first",
                testMessages.get(1).startsWith(BLOCKING_PREFIX));

        Assert.assertTrue(
                "Expected dom change to happen after blocking dependencies loaded and before non-blocking dependencies have loaded",
                testMessages.get(2).equals(DOM_CHANGE_TEXT));

        Assert.assertTrue(
                "Non-blocking dependencies should be loaded after blocking",
                testMessages.get(3).startsWith(NON_BLOCKING_PREFIX));
        Assert.assertTrue(
                "Non-blocking dependencies should be loaded after blocking",
                testMessages.get(4).startsWith(NON_BLOCKING_PREFIX));
    }

    private void ensureDependenciesHaveCorrectAttributes() {
        findElements(By.tagName("script")).stream()
                // FW needs this element to be loaded asap, that's why it's an
                // exclusion
                .filter(javaScriptImport -> !javaScriptImport
                        .getAttribute("src").endsWith("es6-collections.js"))
                .forEach(javaScriptImport -> {
                    Assert.assertEquals(
                            String.format(
                                    "All javascript dependencies should be loaded with 'defer' attribute. Dependency with url %s does not have this attribute",
                                    javaScriptImport.getAttribute("src")),
                            "true", javaScriptImport.getAttribute("defer"));
                    Assert.assertNull(
                            String.format(
                                    "All javascript dependencies should be loaded without 'async' attribute. Dependency with url %s has this attribute",
                                    javaScriptImport.getAttribute("src")),
                            javaScriptImport.getAttribute("async"));
                });

        findElements(By.tagName("link")).stream()
                .filter(element -> element.getAttribute("href")
                        .endsWith(".html"))
                .forEach(
                        htmlImport -> Assert
                                .assertEquals(
                                        String.format(
                                                "All html dependencies should be loaded with 'async' attribute. Dependency with url %s does not have this attribute",
                                                htmlImport
                                                        .getAttribute("href")),
                                        "true",
                                        htmlImport.getAttribute("async")));
    }
}
