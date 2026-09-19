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
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvoker;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.frontend.TaskGenerateJsInvokers;
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

    @JsInvoker
    interface CounterJs extends Serializable {
        @JsExpression("this.count = ($0 || 0) + 1")
        void count(Integer from);
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

        MockDeploymentConfiguration deploymentConfiguration = new MockDeploymentConfiguration();
        deploymentConfiguration.setProjectFolder(projectFolder);
        service = new MockVaadinServletService(deploymentConfiguration);
        // Resolved the way the production code resolves it, so a case writes
        // the file where the hotswapper looks for it
        frontendFolder = FrontendUtils
                .getProjectFrontendDir(deploymentConfiguration);

        configuration = Mockito.mock(ApplicationConfiguration.class);
        // Where the file is written is asked of the configuration, the same
        // way a build asks for it
        Mockito.when(configuration.getProjectFolder())
                .thenReturn(projectFolder);
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
     * Puts the frontend dev server in play, which is what can replace the
     * generated file in a running browser.
     */
    private void withFrontendDevServer() {
        Mockito.when(configuration.getMode())
                .thenReturn(Mode.DEVELOPMENT_FRONTEND_LIVERELOAD);
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
                TaskGenerateJsInvokers.renderInvokerLines(invoker));
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
        withFrontendDevServer();

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
        withFrontendDevServer();

        classesChanged(GreeterJs.class);

        assertTrue(hotswapper.reported.isEmpty());
        assertTrue(readGeneratedInvokers().contains(GreeterJs.class.getName()));
    }

    @Test
    void invokerTheFileNeverHeldOf_writtenBesideTheOnesItHolds()
            throws IOException {
        // What annotating an interface that the file was generated without
        // looks like: nothing has scanned for it, and the interfaces the file
        // does hold have to stay in it.
        writeGeneratedInvokers(generatedFor(CounterJs.class));
        withFrontendDevServer();

        classesChanged(GreeterJs.class);

        assertTrue(hotswapper.reported.isEmpty(),
                "the file can hold both, so there is nothing to report: "
                        + hotswapper.reported);
        String written = readGeneratedInvokers();
        assertTrue(written.contains(GreeterJs.class.getName()),
                "the interface that changed should be in the file: " + written);
        assertTrue(written.contains(CounterJs.class.getName()),
                "the interface the file held should still be in it: "
                        + written);
    }

    @Test
    void frontendDevServerRunningButFileNotWritable_reported()
            throws IOException {
        // A directory where the file belongs: nothing can be written, so the
        // change is reported rather than passing as applied.
        new File(FrontendUtils.getFrontendGeneratedFolder(frontendFolder),
                FrontendUtils.JS_INVOKERS_FILE_NAME).mkdirs();
        withFrontendDevServer();

        classesChanged(GreeterJs.class);

        assertEquals(List.of(GreeterJs.class.getName()), hotswapper.reported);
    }

    @Test
    void noInvokerChanged_nothingReported() throws IOException {
        writeGeneratedInvokers(generatedFor(GreeterJs.class));

        classesChanged(NotAnInvoker.class);

        assertTrue(hotswapper.reported.isEmpty(),
                "a class that declares no JavaScript is not a frontend change");
    }
}
