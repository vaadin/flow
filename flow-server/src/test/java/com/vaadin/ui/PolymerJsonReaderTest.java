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

package com.vaadin.ui;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;

import org.junit.Before;
import org.junit.Test;

import com.vaadin.shared.ui.Dependency;
import com.vaadin.shared.ui.LoadMode;

/**
 * @author Vaadin Ltd.
 */
public class PolymerJsonReaderTest {
    private static final String ROOT_URL = "https://localhost:8888";

    private ServletContext servletContextMock;

    @Before
    public void setUp() {
        servletContextMock = mock(ServletContext.class);
    }

    @Test
    public void fileAbsent_noDependencies() {
        checkThatJsonIsParsedCorrectly(ROOT_URL, null, Collections.emptyList());
    }

    @Test
    public void requiredFieldsAbsent_noDependencies() {
        checkThatJsonIsParsedCorrectly(ROOT_URL, "{}", Collections.emptyList());
    }

    @Test
    public void entryPointSpecifiedOnly() {
        checkThatJsonIsParsedCorrectly(ROOT_URL,
                "{'entrypoint' : 'index.html'}", Collections
                        .singletonList(createExpectedDependency("index.html")));
    }

    @Test
    public void shellSpecifiedOnly() {
        checkThatJsonIsParsedCorrectly(ROOT_URL, "{'shell' : 'shell.html'}",
                Collections
                        .singletonList(createExpectedDependency("shell.html")));
    }

    @Test
    public void entryPointAndShellSpecified_shellComesFirstInList() {
        String shellUrl = "bootstrap.html";
        String entryPointUrl = "index.html";

        checkThatJsonIsParsedCorrectly(ROOT_URL + '/',
                String.format("{'entrypoint' : '%s', 'shell' : '%s'}",
                        entryPointUrl, shellUrl),
                Arrays.asList(createExpectedDependency(shellUrl),
                        createExpectedDependency(entryPointUrl)));
    }

    @Test
    public void multipleSlashesInPathsShouldBeRemoved() {
        String shellUrl = "bootstrap.html";
        String entryPointUrl = "index.html";

        checkThatJsonIsParsedCorrectly(ROOT_URL,
                String.format("{'entrypoint' : '%s', 'shell' : '%s'}",
                        '/' + entryPointUrl, '/' + shellUrl),
                Arrays.asList(createExpectedDependency(shellUrl),
                        createExpectedDependency(entryPointUrl)));
    }

    private Dependency createExpectedDependency(String urlInJsonFile) {
        return new Dependency(Dependency.Type.HTML_IMPORT,
                ROOT_URL + '/' + urlInJsonFile, LoadMode.EAGER);
    }

    private void checkThatJsonIsParsedCorrectly(String rootUrl,
            String jsonString, List<Dependency> expectedDependencies) {
        final Charset charset = StandardCharsets.UTF_8;
        final String polymerJsonPath = "/polymer.json";

        when(servletContextMock.getResourceAsStream(polymerJsonPath))
                .thenReturn(jsonString == null ? null
                        : new ByteArrayInputStream(
                                jsonString.getBytes(charset)));

        assertThat(
                new PolymerJsonReader(charset.name(), servletContextMock,
                        rootUrl).getBootstrapDependencies(),
                equalTo(expectedDependencies));

        verify(servletContextMock, only()).getResourceAsStream(polymerJsonPath);
        verify(servletContextMock, times(1))
                .getResourceAsStream(polymerJsonPath);
    }
}
