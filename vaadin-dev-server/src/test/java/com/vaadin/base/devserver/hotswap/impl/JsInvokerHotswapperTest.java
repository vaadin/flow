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
import com.vaadin.flow.dom.JsExpression;
import com.vaadin.flow.dom.JsInvoker;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.frontend.TaskGenerateJsInvokers;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.frontend.scanner.ClassFinder.DefaultClassFinder;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsInvokerHotswapperTest {

    @JsInvoker
    interface GreeterJs extends Serializable {
        @JsExpression("window.alert($0); this.focus()")
        void showGreeting(String greeting);
    }

    static class NotAnInvoker {
    }

    // Records what a change is reported for instead of logging it.
    private static class TestHotswapper extends JsInvokerHotswapper {
        private final List<String> reported = new ArrayList<>();

        @Override
        void report(List<String> invokerNames) {
            reported.addAll(invokerNames);
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
        frontendFolder = new File(projectFolder, FrontendUtils.FRONTEND);

        service = new MockVaadinServletService(
                new MockDeploymentConfiguration());
        configuration = Mockito.mock(ApplicationConfiguration.class);
        Mockito.when(configuration.getFrontendFolder())
                .thenReturn(frontendFolder);
        // What a browser runs without the frontend dev server is a bundle,
        // which a case has to opt out of to get the file written again.
        Mockito.when(configuration.getMode())
                .thenReturn(Mode.DEVELOPMENT_BUNDLE);
        Mockito.when(service.getLookup()
                .lookup(ApplicationConfigurationFactory.class))
                .thenReturn(context -> configuration);
    }

    /**
     * Puts the frontend dev server in play, with the given interfaces as the
     * ones the application declares.
     */
    private void withFrontendDevServer(Class<?>... invokers) {
        Mockito.when(configuration.getMode())
                .thenReturn(Mode.DEVELOPMENT_FRONTEND_LIVERELOAD);
        Mockito.when(service.getLookup().lookup(ClassFinder.class))
                .thenReturn(new DefaultClassFinder(Set.of(invokers)));
    }

    private String readGeneratedInvokers() throws IOException {
        return Files
                .readString(
                        new File(
                                FrontendUtils.getFrontendGeneratedFolder(
                                        frontendFolder),
                                FrontendUtils.JS_INVOKERS_FILE_NAME).toPath(),
                        StandardCharsets.UTF_8);
    }

    private void writeGeneratedInvokers(String content) throws IOException {
        File generated = FrontendUtils
                .getFrontendGeneratedFolder(frontendFolder);
        generated.mkdirs();
        Files.writeString(
                new File(generated, FrontendUtils.JS_INVOKERS_FILE_NAME)
                        .toPath(),
                content, StandardCharsets.UTF_8);
    }

    private String generatedFor(Class<?> invoker) {
        return String.join(System.lineSeparator(),
                TaskGenerateJsInvokers.invokerLines(invoker));
    }

    private void classesChanged(Class<?>... classes) {
        hotswapper.onClassesChange(
                new HotswapClassEvent(service, Set.of(classes), true));
    }

    @Test
    void bundleCarriesTheDeclarations_nothingReported() throws IOException {
        writeGeneratedInvokers(generatedFor(GreeterJs.class));

        classesChanged(GreeterJs.class);

        assertTrue(hotswapper.reported.isEmpty(),
                "a bundle built from these declarations runs exactly them: "
                        + hotswapper.reported);
    }

    @Test
    void bundleCarriesAnotherVersionOfTheDeclarations_reported()
            throws IOException {
        // The JavaScript the interface declared before it was shortened: the
        // bundle would keep running the extra statement.
        writeGeneratedInvokers(generatedFor(GreeterJs.class).replace(
                "window.alert($0); this.focus()",
                "window.alert($0); this.focus(); this.scrollTo(0, 0)"));

        classesChanged(GreeterJs.class);

        assertEquals(List.of(GreeterJs.class.getName()), hotswapper.reported);
    }

    @Test
    void bundleCarriesTheDeclarationsUnderAnotherName_reported()
            throws IOException {
        // What renaming or moving the interface leaves behind: the methods and
        // the JavaScript are in the bundle, but under the name of before, so a
        // call looks up an invoker the bundle does not have.
        writeGeneratedInvokers(generatedFor(GreeterJs.class)
                .replace(GreeterJs.class.getName(), "com.example.RenamedJs"));

        classesChanged(GreeterJs.class);

        assertEquals(List.of(GreeterJs.class.getName()), hotswapper.reported);
    }

    @Test
    void noGeneratedFile_reported() {
        classesChanged(GreeterJs.class);

        assertEquals(List.of(GreeterJs.class.getName()), hotswapper.reported,
                "without a generated file nothing carries the declarations");
    }

    @Test
    void frontendDevServerRunning_fileWrittenAgainInsteadOfReported()
            throws IOException {
        writeGeneratedInvokers(generatedFor(GreeterJs.class)
                .replace("window.alert($0); this.focus()", "window.alert($0)"));
        withFrontendDevServer(GreeterJs.class);

        classesChanged(GreeterJs.class);

        assertTrue(hotswapper.reported.isEmpty(),
                "with the dev server the change is applied, not reported: "
                        + hotswapper.reported);
        assertTrue(
                readGeneratedInvokers()
                        .contains("window.alert($0); this.focus()"),
                "the file should hold what the interface declares now");
        assertTrue(readGeneratedInvokers().contains("import.meta.hot.accept()"),
                "the file should accept its own update, so the dev server replaces just this module");
    }

    @Test
    void frontendDevServerRunningWithoutTheFile_fileWritten()
            throws IOException {
        withFrontendDevServer(GreeterJs.class);

        classesChanged(GreeterJs.class);

        assertTrue(hotswapper.reported.isEmpty());
        assertTrue(readGeneratedInvokers().contains(GreeterJs.class.getName()));
    }

    @Test
    void noInvokerChanged_nothingReported() throws IOException {
        writeGeneratedInvokers(generatedFor(GreeterJs.class));

        classesChanged(NotAnInvoker.class);

        assertTrue(hotswapper.reported.isEmpty(),
                "a class that declares no JavaScript is not a frontend change");
    }
}
