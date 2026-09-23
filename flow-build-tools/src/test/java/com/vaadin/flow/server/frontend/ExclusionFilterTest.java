/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.server.frontend;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.tests.util.MockOptions;

public class ExclusionFilterTest {

    private ExclusionFilter filter;

    @BeforeEach
    public void init() {
        // Without versions files the only exclusions left are the packages
        // that come with a package Flow manages the version of.
        filter = new ExclusionFilter(Mockito.mock(ClassFinder.class), true);
    }

    @Test
    public void exclusions_areTakenFromThePinnedNpmVersionsOfTheBuild()
            throws IOException {
        ClassFinder finder = Mockito.mock(ClassFinder.class);
        Mockito.when(finder.getResources(Constants.PINNED_NPM_VERSIONS_FOLDER))
                .thenReturn(List.of());
        Options options = new MockOptions(finder, null);

        new ExclusionFilter(options, true, false).exclude(Map.of());
        new ExclusionFilter(options, true, false).exclude(Map.of());

        Mockito.verify(finder, Mockito.times(1))
                .getResources(Constants.PINNED_NPM_VERSIONS_FOLDER);
    }

    @Test
    public void packagesOwnedByLit_excluded() throws IOException {
        Map<String, String> dependencies = filter.exclude(Map.of("lit-element",
                "^3.2.2", "lit-html", "2.4.0", "@lit/reactive-element", "1.6.3",
                "@polymer/paper-button", "^3.0.1"));

        Assert.assertEquals(Map.of("@polymer/paper-button", "^3.0.1"),
                dependencies);
    }

    @Test
    public void litItself_notExcluded() throws IOException {
        Map<String, String> dependencies = filter
                .exclude(Map.of("lit", "3.3.3"));

        Assert.assertEquals(Map.of("lit", "3.3.3"), dependencies);
    }
}
