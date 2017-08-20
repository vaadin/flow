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

package com.vaadin.flow.uitest.ui.dependencies;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;

import com.vaadin.flow.testutil.ChromeBrowserTest;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;

public class PolymerJsonDependenciesIT extends ChromeBrowserTest {

    @Test
    public void polymerJsonDependenciesAreLoadedFirst() {
        final String bootstrapFileName = "shell.html";
        final String entryPointName = "entry.html";
        final String classImportName = "import.html";
        final String rootURL = getRootURL();

        open();

        List<String> imports = findElements(By.tagName("link")).stream()
                .map(element -> element.getAttribute("href"))
                .collect(Collectors.toList());

        assertThat(
                "Expect two dependencies from polymer.json and one from the class",
                imports, hasSize(3));

        assertThat(
                "First dependency should be the bootstrap one: "
                        + bootstrapFileName,
                imports.get(0), is(rootURL + '/' + bootstrapFileName));
        assertThat("Second dependency should be entry point: " + entryPointName,
                imports.get(1), is(rootURL + '/' + entryPointName));
        assertThat(
                "Third dependency should be the one from the class: "
                        + classImportName,
                imports.get(2), endsWith(classImportName));

        String bootstrapPropertyName = "bootstrapApplied";
        assertThat(String.format(
                "Bootstrap script '%s' was expected to load and set window property '%s' to true",
                bootstrapFileName, bootstrapPropertyName),
                getWindowProperty(bootstrapPropertyName), is(true));

        String entryPropertyName = "importedAfterBootstrap";
        assertThat(String.format(
                "Entry page '%s' was expected to load and set window property '%s' to true",
                entryPointName, entryPropertyName),
                getWindowProperty(entryPropertyName), is(true));
    }

    private boolean getWindowProperty(String property) {
        return Boolean.parseBoolean(Optional
                .ofNullable(((JavascriptExecutor) getDriver()).executeScript(
                        String.format("return window.%s", property)))
                .map(Object::toString).orElse(null)

        );
    }
}
