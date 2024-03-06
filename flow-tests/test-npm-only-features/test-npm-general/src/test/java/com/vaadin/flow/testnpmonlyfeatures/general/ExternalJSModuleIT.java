/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.testnpmonlyfeatures.general;

import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ExternalJSModuleIT extends ChromeBrowserTest {
    // prefix with "http:" since Selenium drives seem to expand url fragments
    // to full length:
    // https://github.com/seleniumhq/selenium-google-code-issue-archive/issues/1824
    private final static String EXPECTED_SRC_FOR_NO_PROTOCOL = "http:"
            + ComponentWithExternalJsModule.SOME_RANDOM_EXTERNAL_JS_URL_WITHOUT_PROTOCOL;

    @Before
    public void init() {
        open();
        waitForElementPresent(By.tagName("div"));
    }

    @Test
    public void jsModuleAnnotation_externalJs_shouldBeAddedToPage() {
        List<WebElement> scriptTags = findElements(By.tagName("script"));
        Assert.assertTrue(
                "External JS annotated with @JsModule annotation should be added as a script tag with module type to the page!",
                scriptTags.stream().anyMatch(
                        scriptTag -> ComponentWithExternalJsModule.SOME_RANDOM_EXTERNAL_JS_URL
                                .equals(scriptTag.getAttribute("src"))
                                && "module".equals(
                                        scriptTag.getAttribute("type"))));
        Assert.assertTrue(
                "External JS without protocol annotated with @JsModule annotation should be added as a script tag with module type to the page!",
                scriptTags.stream()
                        .anyMatch(scriptTag -> EXPECTED_SRC_FOR_NO_PROTOCOL
                                .equals(scriptTag.getAttribute("src"))
                                && "module".equals(
                                        scriptTag.getAttribute("type"))));
    }

    @Test
    public void jsModuleAnnotation_externalJsInAComponentBeingAdded_shouldBeAddedToPage() {
        findElement(By.id("addComponentButton")).click();
        waitForElementPresent(By.id("componentWithExternalJsModule"));

        List<WebElement> scriptTags = findElements(By.tagName("script"));
        Assert.assertTrue(
                "When a component is added to the page, external JS annotated with @JsModule annotation in the component should be added as a script tag with module type to the page!",
                scriptTags.stream().anyMatch(
                        scriptTag -> ComponentWithExternalJsModule.SOME_RANDOM_EXTERNAL_JS_URL
                                .equals(scriptTag.getAttribute("src"))
                                && "module".equals(
                                        scriptTag.getAttribute("type"))));
        Assert.assertTrue(
                "When a component is added to the page, external JS without protocol annotated with @JsModule annotation in the component should be added as a script tag with module type to the page!",
                scriptTags.stream()
                        .anyMatch(scriptTag -> EXPECTED_SRC_FOR_NO_PROTOCOL
                                .equals(scriptTag.getAttribute("src"))
                                && "module".equals(
                                        scriptTag.getAttribute("type"))));

        Assert.assertTrue(
                "When a component is added to the page, non-external JS annotated with @JsModule annotation in the component should not be added as a script tag to the page!",
                scriptTags.stream().noneMatch(scriptTag -> scriptTag
                        .getAttribute("src") != null
                        && scriptTag.getAttribute("src").endsWith(
                                ComponentWithExternalJsModule.NON_EXTERNAL_JS_MODULE_NAME)));
    }
}
