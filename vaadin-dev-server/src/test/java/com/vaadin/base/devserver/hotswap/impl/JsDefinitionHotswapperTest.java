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
package com.vaadin.base.devserver.hotswap.impl;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import com.vaadin.base.devserver.hotswap.HotswapClassEvent;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.frontend.Options;
import com.vaadin.flow.server.frontend.TaskGenerateJsDefinitions;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsDefinitionHotswapperTest {

    @JsDefinition
    interface GreeterJs extends Serializable {
        @JsExpression("window.alert($0); this.focus()")
        void showGreeting(String greeting);
    }

    @JsDefinition
    interface CounterJs extends Serializable {
        @JsExpression("this.count = ($0 || 0) + 1")
        void count(Integer from);
    }

    static class NotADefinition {
    }

    // Records what a change is reported for instead of logging it.
    private static class TestHotswapper extends JsDefinitionHotswapper {
        private final List<String> reported = new ArrayList<>();

        @Override
        void report(List<String> definitionNames) {
            reported.addAll(definitionNames);
        }
    }

    @TempDir
    File projectFolder;

    private TestHotswapper hotswapper;
    private MockVaadinServletService service;
    private File frontendFolder;
    private ApplicationConfiguration configuration;

    @BeforeEach
    void setUp() {
        hotswapper = new TestHotswapper();

        MockDeploymentConfiguration deploymentConfiguration = new MockDeploymentConfiguration();
        deploymentConfiguration.setProjectFolder(projectFolder);
        service = new MockVaadinServletService(deploymentConfiguration);
        // Resolved the way the production code resolves it, so a case writes
        // the file where the hotswapper looks for it
        frontendFolder = FrontendUtils
                .getProjectFrontendDir(deploymentConfiguration);

        configuration = Mockito.mock(ApplicationConfiguration.class);
        // What a browser runs without the frontend dev server is a bundle,
        // which a case has to opt out of to get the file written again.
        Mockito.when(configuration.getMode())
                .thenReturn(Mode.DEVELOPMENT_BUNDLE);
        Mockito.when(service.getLookup()
                .lookup(ApplicationConfigurationFactory.class))
                .thenReturn(context -> configuration);
    }

    /**
     * Puts the frontend dev server in play, which is what can replace the
     * generated file in a running browser.
     */
    private void withFrontendDevServer() {
        Mockito.when(configuration.getMode())
                .thenReturn(Mode.DEVELOPMENT_FRONTEND_LIVERELOAD);
    }

    private String readGeneratedDefinitions() throws IOException {
        return Files.readString(
                new File(
                        FrontendUtils
                                .getFrontendGeneratedFolder(frontendFolder),
                        FrontendUtils.JS_DEFINITIONS_FILE_NAME).toPath(),
                StandardCharsets.UTF_8);
    }

    private void writeGeneratedDefinitions(String content) throws IOException {
        File generated = FrontendUtils
                .getFrontendGeneratedFolder(frontendFolder);
        generated.mkdirs();
        Files.writeString(
                new File(generated, FrontendUtils.JS_DEFINITIONS_FILE_NAME)
                        .toPath(),
                content, StandardCharsets.UTF_8);
    }

    /**
     * The file as a build writes it for the given interface, which is what a
     * browser would be running.
     */
    private String generatedFor(Class<?> definition) throws IOException {
        Options options = new Options(Mockito.mock(Lookup.class), null, null)
                .withFrontendDirectory(frontendFolder);
        TaskGenerateJsDefinitions.updateJsDefinitions(options,
                List.of(definition));
        String content = readGeneratedDefinitions();
        Files.delete(new File(
                FrontendUtils.getFrontendGeneratedFolder(frontendFolder),
                FrontendUtils.JS_DEFINITIONS_FILE_NAME).toPath());
        return content;
    }

    private void classesChanged(Class<?>... classes) {
        hotswapper.onClassesChange(
                new HotswapClassEvent(service, Set.of(classes), true));
    }

    @Test
    void fileCarriesTheDeclarations_nothingReported() throws IOException {
        writeGeneratedDefinitions(generatedFor(GreeterJs.class));

        classesChanged(GreeterJs.class);

        assertTrue(hotswapper.reported.isEmpty(),
                "a browser running what the interface declares needs nothing said about it: "
                        + hotswapper.reported);
    }

    @Test
    void fileDoesNotCarryTheDeclarations_reported() {
        classesChanged(GreeterJs.class);

        assertEquals(List.of(GreeterJs.class.getName()), hotswapper.reported,
                "without the dev server only a build can put them there, so say so");
    }

    @Test
    void frontendDevServerRunning_appliedInsteadOfReported()
            throws IOException {
        writeGeneratedDefinitions(generatedFor(GreeterJs.class)
                .replace("window.alert($0); this.focus()", "window.alert($0)"));
        withFrontendDevServer();

        classesChanged(GreeterJs.class);

        assertTrue(hotswapper.reported.isEmpty(),
                "with the dev server the change is applied, not reported: "
                        + hotswapper.reported);
        assertTrue(
                readGeneratedDefinitions()
                        .contains("window.alert($0); this.focus()"),
                "and what the browser reloads is what the interface declares now");
    }

    @Test
    void frontendDevServerRunningButNothingCanBeWritten_reported() {
        // A directory where the file belongs: the change cannot be applied, so
        // it is reported rather than passing as applied
        new File(FrontendUtils.getFrontendGeneratedFolder(frontendFolder),
                FrontendUtils.JS_DEFINITIONS_FILE_NAME).mkdirs();
        withFrontendDevServer();

        classesChanged(GreeterJs.class);

        assertEquals(List.of(GreeterJs.class.getName()), hotswapper.reported);
    }

    @Test
    void noDefinitionChanged_nothingReported() throws IOException {
        writeGeneratedDefinitions(generatedFor(GreeterJs.class));

        classesChanged(NotADefinition.class);

        assertTrue(hotswapper.reported.isEmpty(),
                "a class that declares no JavaScript is not a frontend change");
    }
}
