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

package com.vaadin.server.webjar;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * @author Vaadin Ltd.
 */
public class WebJarBowerDependencyTest {
    private static final String BOWER_NAME = "bower-name";
    private static final String WEBJAR_NAME = "webjar-name";
    private static final String VERSION = "1.2.2-alpha77";

    @Test(expected = NullPointerException.class)
    public void nullBowerNameIsNotAllowed() {
        new WebJarBowerDependency(null, "webJarName", "1.2.3");
    }

    @Test(expected = NullPointerException.class)
    public void nullWebJarNameIsNotAllowed() {
        new WebJarBowerDependency("bowerName", null, "1.2.3");
    }

    @Test(expected = NullPointerException.class)
    public void nullVersionIsNotAllowed() {
        new WebJarBowerDependency("bowerName", "webJarName", null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void dependenciesWithDifferentBowerNamesAreIncomparable() {
        new WebJarBowerDependency("bowerName1", "webJarName", "1.2.3")
                .compareVersions(new WebJarBowerDependency("bowerName2",
                        "webJarName", "1.2.3"));
    }

    @Test
    public void webPathIsConstructedFromOriginalNameAndVersion() {
        String webPath = new WebJarBowerDependency(BOWER_NAME, WEBJAR_NAME,
                VERSION).toWebPath();

        assertThat("Constructed webPath should contain webJar name", webPath,
                startsWith(WEBJAR_NAME));
        assertThat("Constructed webPath should contain original version string",
                webPath, endsWith(VERSION));
        assertThat("Constructed webPath should not contain bower name", webPath,
                not(containsString(BOWER_NAME)));
    }

    @Test
    public void equalDependencies() {
        assertDependenciesEqual(
                new WebJarBowerDependency(BOWER_NAME, WEBJAR_NAME, VERSION),
                new WebJarBowerDependency(BOWER_NAME, WEBJAR_NAME, VERSION));
    }

    @Test
    public void equalDependencies_withWebJarVersionPrefixes() {
        assertDependenciesEqual(
                new WebJarBowerDependency(BOWER_NAME, WEBJAR_NAME, VERSION),
                new WebJarBowerDependency(BOWER_NAME, WEBJAR_NAME,
                        'v' + VERSION));
    }

    private void assertDependenciesEqual(WebJarBowerDependency one,
            WebJarBowerDependency two) {
        assertThat(
                "Dependencies with the same names (without webjar prefixes) and versions (without webjar 'v' prefix) strings should be equal",
                one.compareVersions(two), is(0));
    }
}
