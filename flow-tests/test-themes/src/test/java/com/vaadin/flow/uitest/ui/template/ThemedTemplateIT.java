/*
 * Copyright 2000-2020 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.vaadin.flow.uitest.ui.template;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class ThemedTemplateIT extends ChromeBrowserTest {

    @Test
    public void themedUrlsAreAdded() {
        open();

        // check that all imported templates are available in the DOM
        WebElement template = findElement(By.tagName("themed-template"));

        Assert.assertTrue("The main template has no simple child Div inside it",
                isPresentInShadowRoot(template, By.id("div")));
        Assert.assertTrue(
                "The main template has no sub template which is imported by "
                        + "relative URL referring to the resource in the same folder",
                isPresentInShadowRoot(template, By.id("relative1")));
        Assert.assertTrue(
                "The main template has no sub template which is imported by "
                        + "relative URL referring to the resource in the parent folder",
                isPresentInShadowRoot(template, By.id("relative2")));
        Assert.assertTrue(
                "The main template has no sub template which is imported by "
                        + "absolute URL",
                isPresentInShadowRoot(template, By.id("absolute")));

        WebElement head = findElement(By.tagName("head"));
        List<WebElement> links = head.findElements(By.tagName("link"));
        Set<String> hrefs = links.stream()
                .map(link -> link.getAttribute("href")).filter(Objects::nonNull)
                .map(this::getFilePath).collect(Collectors.toSet());

        Assert.assertTrue(
                "The themed HTML file for the template is not added as an HMTL import to the head",
                hrefs.stream().anyMatch(href -> href.endsWith(
                        "/frontend/bower_components/themed-template/theme/myTheme/com/ThemedTemplate.html")));

        Assert.assertTrue(
                "The themed HTML file for the simple relative file (same location as the template file) "
                        + "is not added as an HMTL import to the head",
                hrefs.stream().anyMatch(href -> href.endsWith(
                        "/frontend/bower_components/themed-template/theme/myTheme/com/relative1.html")));
        Assert.assertTrue(
                "The themed HTML file for the relative file (located in the parent folder of the template file) "
                        + "is not added as an HMTL import to the head",
                hrefs.stream().anyMatch(href -> href.endsWith(
                        "frontend/bower_components/themed-template/theme/myTheme/relative2.html")));
        Assert.assertTrue(
                "The themed HTML file for the absolute file "
                        + "is not added as an HMTL import to the head",
                hrefs.contains("/frontend/absolute.html"));
    }

    private String getFilePath(String url) {
        try {
            URL location = new URL(url);
            return location.getFile();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unexpected URL string '" + url + "'",
                    e);
        }
    }
}
