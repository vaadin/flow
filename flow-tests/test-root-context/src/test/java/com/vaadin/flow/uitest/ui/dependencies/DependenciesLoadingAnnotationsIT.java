package com.vaadin.flow.uitest.ui.dependencies;

import static com.vaadin.flow.uitest.ui.dependencies.DependenciesLoadingBaseView.DOM_CHANGE_TEXT;
import static com.vaadin.flow.uitest.ui.dependencies.DependenciesLoadingBaseView.PRELOADED_DIV_ID;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.either;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testcategory.IgnoreNPM;
import com.vaadin.flow.testutil.ChromeBrowserTest;

/**
 * A test that ensures correct order of dependencies loaded. Test corresponds to
 * {@link DependenciesLoadingAnnotationsView}, which uses annotations to add
 * dependencies. There is a related pair of classes,
 * {@link DependenciesLoadingPageApiView} and
 * {@link DependenciesLoadingPageApiIT}, designed to test exactly the same
 * dependency loading functionality (hence reusing all methods and constants),
 * but using {@link com.vaadin.flow.component.page.Page} api to add
 * dependencies.
 *
 * @author Vaadin Ltd
 * @since 1.0.
 * @see DependenciesLoadingBaseView
 * @see DependenciesLoadingAnnotationsView
 * @see DependenciesLoadingPageApiView
 * @see DependenciesLoadingPageApiIT
 */
@Category(IgnoreNPM.class)
public class DependenciesLoadingAnnotationsIT extends ChromeBrowserTest {
    private static final String EAGER_PREFIX = "eager.";
    private static final String INLINE_PREFIX = "inline.";
    private static final String LAZY_PREFIX = "lazy.";

    @Test
    public void dependenciesLoadedAsExpectedWithAnnotationApi() {
        open();

        waitUntil(input -> !input.findElements(By.className("dependenciesTest"))
                .isEmpty());

        flowDependenciesShouldBeImportedBeforeUserDependenciesWithCorrectAttributes();
        checkInlinedCss();

        WebElement preloadedDiv = findElement(By.id(PRELOADED_DIV_ID));
        Assert.assertEquals(
                "Lazy css should be loaded last: color should be blue",
                "rgba(0, 0, 255, 1)", preloadedDiv.getCssValue("color"));

        List<String> testMessages = findElements(
                By.className("dependenciesTest")).stream()
                        .map(WebElement::getText).collect(Collectors.toList());

        assertThat(
                "7 elements are expected to be added: 2 for eager dependencies, 2 for inline dependencies, 1 for UI 'onAttach' method, 2 for lazy dependencies",
                testMessages, hasSize(7));

        List<String> inlineAndEagerMessages = testMessages.subList(0, 4);

        List<String> eagerMessages = inlineAndEagerMessages.stream()
                .filter(message -> message.startsWith(EAGER_PREFIX))
                .collect(Collectors.toList());
        assertThat("2 eager messages should be posted before lazy messages",
                eagerMessages, hasSize(2));

        List<String> inlineMessages = inlineAndEagerMessages.stream()
                .filter(message -> message.startsWith(INLINE_PREFIX))
                .collect(Collectors.toList());
        assertThat("2 inline messages should be posted before lazy messages",
                inlineMessages, hasSize(2));

        Assert.assertTrue(
                "Expected dom change to happen after eager dependencies loaded and before lazy dependencies have loaded, but got "
                        + testMessages.get(4),
                testMessages.get(4).equals(DOM_CHANGE_TEXT));

        List<String> lazyMessages = testMessages.subList(5, 7);
        Assert.assertTrue(
                "Lazy dependencies should be loaded after eager and inline, but got "
                        + lazyMessages.get(0),
                lazyMessages.get(0).startsWith(LAZY_PREFIX));
        Assert.assertTrue(
                "Lazy dependencies should be loaded after eager and inline, but got "
                        + lazyMessages.get(1),
                lazyMessages.get(1).startsWith(LAZY_PREFIX));
    }

    private void checkInlinedCss() {
        Optional<String> inlinedCss = findElements(By.tagName("style")).stream()
                .map(webElement -> webElement.getAttribute("innerHTML"))
                .filter(cssContents -> cssContents.contains("inline.css"))
                .findAny();
        assertThat(
                "One of the inlined css should be our dependency containing `inline.css` string inside",
                inlinedCss.isPresent(), is(true));

        WebElement inlineCssTestDiv = findElement(
                By.id(DependenciesLoadingBaseView.INLINE_CSS_TEST_DIV_ID));
        Assert.assertEquals(
                "Incorrect color for the div that should be styled with inline.css",
                "rgba(255, 255, 0, 1)", inlineCssTestDiv.getCssValue("color"));
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
                assertThat(
                        "Expected to have here dependencies added with Flow public api",
                        jsUrl,
                        either(containsString("eager"))
                                .or(containsString("lazy"))
                                // inline elements do not have the url
                                .or(isEmptyString())
                                .or(containsString("dndConnector.js")));
            } else {
                flowDependencyMaxIndex = i;
                assertThat(
                        "Flow dependencies should not contain user dependencies",
                        jsUrl, both(not(containsString("eager")))
                                .and(not(containsString("lazy"))));

                if (jsUrl.endsWith(".cache.js")) {
                    foundClientEngine = true;
                }
            }

            assertThat(String.format(
                    "All javascript dependencies should be loaded without 'async' attribute. Dependency with url %s has this attribute",
                    jsImport.getAttribute("src")),
                    jsImport.getAttribute("async"), is(nullValue()));
        }

        assertThat(
                "Flow dependencies should be imported before user dependencies",
                flowDependencyMaxIndex, is(lessThan(userDependencyMinIndex)));
    }
}
