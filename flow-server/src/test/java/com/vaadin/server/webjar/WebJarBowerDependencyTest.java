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

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import org.junit.Test;

/**
 * @author Vaadin Ltd.
 */
public class WebJarBowerDependencyTest {

    @Test(expected = NullPointerException.class)
    public void nullVersionIsNotAllowed() {
        new WebJarBowerDependency("name", null);
    }

    @Test(expected = NullPointerException.class)
    public void nullNameIsNotAllowed() {
        new WebJarBowerDependency(null, "1.2.3");
    }

    @Test(expected = IllegalArgumentException.class)
    public void dependenciesWithDifferentNamesAreIncomparable() {
        new WebJarBowerDependency("name1", "1.2.3")
                .compareVersions(new WebJarBowerDependency("name2", "1.2.3"));
    }

    @Test
    public void webPathIsConstructedFromOriginalNameAndVersion() {
        String name = "github-com-polymer-test";
        String version = "v2.2.2";

        String webPath = new WebJarBowerDependency(name, version).toWebPath();

        assertThat("Constructed webPath is incorrect", webPath,
                startsWith(name));
        assertThat("Constructed webPath is incorrect", webPath,
                endsWith(version));
    }

    @Test
    public void equalDependencies() {
        String name = "aDependency";
        String versionString = "1.2.3";

        assertDependenciesEqual(new WebJarBowerDependency(name, versionString),
                new WebJarBowerDependency(name, versionString));
    }

    @Test
    public void equalDependencies_withWebJarVersionPrefixes() {
        String name = "aDependency";
        String versionString1 = "1.2.3";
        String versionString2 = 'v' + versionString1;

        assertDependenciesEqual(new WebJarBowerDependency(name, versionString1),
                new WebJarBowerDependency(name, versionString2));
    }

    @Test
    public void equalDependencies_withWebJarNamePrefixes_1() {
        String name1 = "paper-input";
        String name2 = "github-com-polymer-" + name1;
        String version = "1.2.3";

        assertDependenciesEqual(new WebJarBowerDependency(name1, version),
                new WebJarBowerDependency(name2, version));
    }

    @Test
    public void equalDependencies_withWebJarNamePrefixes_2() {
        String name1 = "paper-input";
        String name2 = "github-com-polymerelements-" + name1;
        String version = "1.2.3";

        assertDependenciesEqual(new WebJarBowerDependency(name1, version),
                new WebJarBowerDependency(name2, version));
    }

    @Test
    public void equalDependencies_withWebJarNamePrefixes_3() {
        String name1 = "paper-input";
        String name2 = "github-com-PolymerElements-" + name1;
        String version = "1.2.3";

        assertDependenciesEqual(new WebJarBowerDependency(name1, version),
                new WebJarBowerDependency(name2, version));
    }

    private void assertDependenciesEqual(WebJarBowerDependency one,
            WebJarBowerDependency two) {
        assertThat(
                "Dependencies with the same names (without webjar prefixes) and versions (without webjar 'v' prefix) strings should be equal",
                one.compareVersions(two), is(0));
    }
}
