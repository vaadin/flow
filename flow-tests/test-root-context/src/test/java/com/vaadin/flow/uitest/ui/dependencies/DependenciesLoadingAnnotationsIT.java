package com.vaadin.flow.uitest.ui.dependencies;

import static com.vaadin.flow.uitest.ui.dependencies.DependenciesLoadingAnnotationsUI.DOM_CHANGE_TEXT;
import static com.vaadin.flow.uitest.ui.dependencies.DependenciesLoadingAnnotationsUI.PRELOADED_DIV_ID;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

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
public class DependenciesLoadingAnnotationsIT extends ChromeBrowserTest {
    private static final String EAGER_PREFIX = "eager.";
    private static final String LAZY_PREFIX = "lazy.";

    @Test
    public void dependenciesLoadedAsExpectedWithAnnotationApi() {
        open();

        waitUntil(input -> input.findElements(By.className("dependenciesTest"))
                .size() == 5);

        ensureDependenciesHaveCorrectAttributes();

        WebElement preloadedDiv = findElement(By.id(PRELOADED_DIV_ID));
        Assert.assertEquals(
                "Lazy css should be loaded last: color should be blue",
                "rgba(0, 0, 255, 1)", preloadedDiv.getCssValue("color"));

        List<String> testMessages = findElements(
                By.className("dependenciesTest")).stream()
                        .map(WebElement::getText).collect(Collectors.toList());

        Assert.assertEquals(
                "5 elements are expected to be added: 2 for eager dependencies, 1 for UI 'onAttach' method, 2 for lazy dependencies",
                5, testMessages.size());

        Assert.assertTrue("Eager dependencies should be loaded first",
                testMessages.get(0).startsWith(EAGER_PREFIX));
        Assert.assertTrue("Eager dependencies should be loaded first",
                testMessages.get(1).startsWith(EAGER_PREFIX));

        Assert.assertTrue(
                "Expected dom change to happen after eager dependencies loaded and before lazy dependencies have loaded",
                testMessages.get(2).equals(DOM_CHANGE_TEXT));

        Assert.assertTrue(
                "Lazy dependencies should be loaded after eager",
                testMessages.get(3).startsWith(LAZY_PREFIX));
        Assert.assertTrue(
                "Lazy dependencies should be loaded after eager",
                testMessages.get(4).startsWith(LAZY_PREFIX));
    }

    private void ensureDependenciesHaveCorrectAttributes() {
        findElements(By.tagName("script")).stream()
                // FW needs this element to be loaded asap, that's why it's an
                // exclusion
                .filter(javaScriptImport -> {
                    String jsUrl = javaScriptImport.getAttribute("src");
                    return !jsUrl.endsWith("es6-collections.js")
                            && !jsUrl.endsWith("webcomponents-lite.js");
                }).forEach(javaScriptImport -> {
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
    }
}
