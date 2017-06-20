package com.vaadin.flow.uitest.ui.dependencies;

import static com.vaadin.flow.uitest.ui.dependencies.DependenciesLoadingBaseUI.DOM_CHANGE_TEXT;
import static com.vaadin.flow.uitest.ui.dependencies.DependenciesLoadingBaseUI.PRELOADED_DIV_ID;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

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

        flowDependenciesShouldBeImportedBeforeUserDependenciesWithCorrectAttributes();

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

        Assert.assertTrue(
                "Eager dependencies should be loaded first, but got "
                        + testMessages.get(0),
                testMessages.get(0).startsWith(EAGER_PREFIX));
        Assert.assertTrue(
                "Eager dependencies should be loaded first, but got "
                        + testMessages.get(1),
                testMessages.get(1).startsWith(EAGER_PREFIX));

        Assert.assertTrue(
                "Expected dom change to happen after eager dependencies loaded and before lazy dependencies have loaded, but got "
                        + testMessages.get(2),
                testMessages.get(2).equals(DOM_CHANGE_TEXT));

        Assert.assertTrue(
                "Lazy dependencies should be loaded after eager, but got "
                        + testMessages.get(3),
                testMessages.get(3).startsWith(LAZY_PREFIX));
        Assert.assertTrue(
                "Lazy dependencies should be loaded after eager, but got "
                        + testMessages.get(4),
                testMessages.get(4).startsWith(LAZY_PREFIX));
    }

    private void flowDependenciesShouldBeImportedBeforeUserDependenciesWithCorrectAttributes() {
        boolean foundClientEngine = false;
        int flowDependencyMaxIndex = Integer.MAX_VALUE;
        int userDependencyMinIndex = Integer.MAX_VALUE;

        List<WebElement> jsImports = findElements(By.tagName("script"));
        for (int i = 0; i < jsImports.size(); i++) {
            WebElement jsImport = jsImports.get(i);
            String jsUrl = jsImport.getAttribute("src");
            if (foundClientEngine) {
                if (userDependencyMinIndex > i) {
                    userDependencyMinIndex = i;
                }
                assertThat("Expected to have here dependencies added with Flow public api",
                        jsUrl, either(containsString("eager"))
                                .or(containsString("lazy")));
            } else {
                flowDependencyMaxIndex = i;
                assertThat("Flow dependencies should not contain user dependencies",
                        jsUrl, both(not(containsString("eager")))
                                .and(not(containsString("lazy"))));

                if (jsUrl.endsWith(".cache.js")) {
                    foundClientEngine = true;
                }
            }

            assertThat(
                    String.format(
                            "All javascript dependencies should be loaded without 'async' attribute. Dependency with url %s has this attribute",
                            jsImport.getAttribute("src")),
                    jsImport.getAttribute("async"), is(nullValue()));
        }


        assertThat("Flow dependencies should be imported before user dependencies",
                flowDependencyMaxIndex, is(lessThan(userDependencyMinIndex)));
    }
}
