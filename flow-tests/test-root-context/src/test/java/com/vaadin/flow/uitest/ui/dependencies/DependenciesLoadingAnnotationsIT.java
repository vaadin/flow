package com.vaadin.flow.uitest.ui.dependencies;

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
 * @see DependenciesLoadingAnnotationsUI
 * @see DependenciesLoadingPageApiUI
 * @see DependenciesLoadingPageApiIT
 */
@SuppressWarnings("Duplicates")
public class DependenciesLoadingAnnotationsIT extends PhantomJSTest {
    private static final String BLOCKING_PREFIX = "blocking.";
    private static final String NON_BLOCKING_PREFIX = "non-blocking.";

    @Test
    public void dependenciesLoadedAsExpectedWithAnnotationApi() {
        openAndVerifyPage();
    }

    private void openAndVerifyPage() {
        open();

        WebElement preloadedDiv = findElement(By.id(PRELOADED_DIV_ID));
        Assert.assertEquals(
                "Non-blocking css should be loaded last: color should be blue",
                "rgba(0, 0, 255, 1)", preloadedDiv.getCssValue("color"));

        List<String> testMessages = findElements(
                By.className("dependenciesTest")).stream()
                        .map(WebElement::getText).collect(Collectors.toList());

        Assert.assertEquals(
                "Four dependencies are supposed to create tags, each one at load",
                4, testMessages.size());

        Assert.assertTrue("Blocking dependencies should be loaded first",
                testMessages.get(0).startsWith(BLOCKING_PREFIX));
        Assert.assertTrue("Blocking dependencies should be loaded first",
                testMessages.get(1).startsWith(BLOCKING_PREFIX));
        Assert.assertTrue(
                "Non-blocking dependencies should be loaded after blocking",
                testMessages.get(2).startsWith(NON_BLOCKING_PREFIX));
        Assert.assertTrue(
                "Non-blocking dependencies should be loaded after blocking",
                testMessages.get(3).startsWith(NON_BLOCKING_PREFIX));
    }
}
