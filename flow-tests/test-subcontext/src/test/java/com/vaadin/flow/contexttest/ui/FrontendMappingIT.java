/*
 * Copyright 2000-2017 Vaadin Ltd.
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
package com.vaadin.flow.contexttest.ui;

import static org.junit.Assert.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.vaadin.flow.testutil.ChromeBrowserTest;

public class FrontendMappingIT extends ChromeBrowserTest {

    @Override
    protected String getTestPath() {
        return "/context/frontend-resource/";
    }

    @Test
    public void customTemplate_contextWithMapping() {
        getDriver().get(getTestURL(getRootURL(), getTestPath() + "template",
                new String[0]));

        assertTemplate("frontend-template", "template-element");

        Set<String> hrefs = getHrefs();
        assertTrue(hrefs.contains(
                "/context/frontend-resource/frontend/frontend-template.html"));
        assertTrue(hrefs.contains(
                "/context/frontend-resource/frontend/bower_components/polymer/polymer-element.html"));
    }

    @Test
    public void webComponent_contextWithMapping() {
        getDriver().get(getTestURL(getRootURL(), getTestPath() + "slider",
                new String[0]));

        assertTemplate("paper-slider", "sliderContainer");

        Set<String> hrefs = getHrefs();
        assertTrue(hrefs.contains(
                "/context/frontend-resource/frontend/bower_components/paper-slider/paper-slider.html"));
    }

    private void assertTemplate(String templateTag, String nestedElementId) {
        WebElement template = findElement(By.tagName(templateTag));
        assertTrue(isPresentInShadowRoot(template, By.id(nestedElementId)));
    }

    private Set<String> getHrefs() {
        return findElements(By.tagName("link")).stream()
                .filter(link -> "import".equals(link.getAttribute("rel")))
                .map(link -> link.getAttribute("href"))
                .filter(href -> href.startsWith(getRootURL()))
                .map(href -> href.substring(getRootURL().length()))
                .collect(Collectors.toSet());
    }
}
