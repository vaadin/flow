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
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.ByteBuddyAgent;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.dynamic.loading.ClassReloadingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.vaadin.base.devserver.hotswap.HotswapClassEvent;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.ReflectionCache;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionImport;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsFunctionParameterNames;
import com.vaadin.flow.js.JsInvokers;
import com.vaadin.flow.js.JsSnippet;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.js.JsStringFallback;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.MockVaadinServletService;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.frontend.Options;
import com.vaadin.flow.server.frontend.TaskGenerateJsDefinitions;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.MockDeploymentConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsDefinitionHotswapperTest {

    @JsDefinition
    interface GreeterJs extends JsInvokers {
        @JsExpression("window.alert($0); this.focus()")
        void showGreeting(String greeting);
    }

    @JsDefinition
    @JsDefinitionImport(from = "./greeting.js", exports = "greet")
    interface ImportingGreeterJs extends JsInvokers {
        @JsExpression("greet(this, $0)")
        void showGreeting(String greeting);
    }

    @JsDefinition
    interface CounterJs extends JsInvokers {
        @JsExpression("this.count = ($0 || 0) + 1")
        void count(Integer from);
    }

    @JsDefinition
    @JsStringFallback
    interface FallbackJs extends JsInvokers {
        @JsExpression("this.focus()")
        void focus();
    }

    @JsFunctionParameterNames({ "item" })
    interface TemplateSnippet extends JsSnippet {
    }

    @JsDefinition
    interface TemplateJs extends JsSnippets {
        @JsExpression("return item.name + $0;")
        TemplateSnippet label(String suffix);
    }

    // Only extended, so not a definition itself
    interface ScrollableJs extends JsInvokers {
        @JsExpression("this.scrollTop = $0")
        void scrollTo(int top);
    }

    // What ScrollableJs is redefined to
    interface EditedScrollableJs extends JsInvokers {
        @JsExpression("this.scrollLeft = $0")
        void scrollTo(int top);
    }

    // Only extended, and declares JavaScript without @JsExpression
    interface PopoverJs extends JsInvokers {
        void showPopover();
    }

    @JsDefinition
    interface PopoverMenuJs extends PopoverJs {
        @JsExpression("this.scrollTop = 0")
        void scrollToTop();
    }

    @JsDefinition
    interface ScrollingListJs extends ScrollableJs {
        @JsExpression("this.scrollTop = this.scrollHeight")
        void scrollToEnd();
    }

    // Annotations are not inherited, so it needs its own
    @JsDefinition
    interface DetailedListJs extends ScrollingListJs {
        @JsExpression("this.dataset.detail = $0")
        void showDetail(String detail);
    }

    static class NotADefinition {
    }

    // Records what a change is warned about instead of logging it.
    private static class TestHotswapper extends JsDefinitionHotswapper {
        private final List<String> reported = new ArrayList<>();

        @Override
        void warnAboutMissingDefinitions(List<Class<?>> definitions) {
            definitions.stream().map(Class::getName).forEach(reported::add);
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

        configuration = mock(ApplicationConfiguration.class);
        // What a browser runs without the frontend dev server is a bundle,
        // which a case has to opt out of to get the file written again.
        when(configuration.getMode()).thenReturn(Mode.DEVELOPMENT_BUNDLE);
        when(service.getLookup().lookup(ApplicationConfigurationFactory.class))
                .thenReturn(context -> configuration);
    }

    /**
     * Puts the frontend dev server in play, which is what can replace the
     * generated file in a running browser.
     */
    private void withFrontendDevServer() {
        when(configuration.getMode())
                .thenReturn(Mode.DEVELOPMENT_FRONTEND_LIVERELOAD);
    }

    private File generatedFile() {
        return new File(
                FrontendUtils.getFrontendGeneratedFolder(frontendFolder),
                FrontendUtils.JS_DEFINITIONS_FILE_NAME);
    }

    private String readGeneratedDefinitions() throws IOException {
        return Files.readString(generatedFile().toPath(),
                StandardCharsets.UTF_8);
    }

    /**
     * Writes the file as a build writes it for the given interface, where the
     * hotswapper looks for it, which is what a browser would be running.
     */
    private void writeGeneratedDefinitionsFor(Class<?> definition) {
        Options options = new Options(mock(Lookup.class),
                new ClassFinder.DefaultClassFinder(getClass().getClassLoader()),
                null).withFrontendDirectory(frontendFolder);
        TaskGenerateJsDefinitions.updateJsDefinitions(options,
                List.of(definition));
    }

    /**
     * Edits the written file, to make it the older version of the declarations
     * that a browser would still be running.
     */
    private void editGeneratedDefinitions(String declared, String previously)
            throws IOException {
        Files.writeString(generatedFile().toPath(),
                readGeneratedDefinitions().replace(declared, previously),
                StandardCharsets.UTF_8);
    }

    private void classesChanged(Class<?>... classes) {
        hotswapper.onClassesChange(
                new HotswapClassEvent(service, Set.of(classes), true));
    }

    @Test
    void fileCarriesTheDeclarations_nothingReported() {
        writeGeneratedDefinitionsFor(GreeterJs.class);

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
        writeGeneratedDefinitionsFor(GreeterJs.class);
        editGeneratedDefinitions("window.alert($0); this.focus()",
                "window.alert($0)");
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
    void importingDefinitionChanged_frontendDevServerRunning_appliedWithItsImports()
            throws IOException {
        writeGeneratedDefinitionsFor(ImportingGreeterJs.class);
        editGeneratedDefinitions("greet(this, $0)", "greet($0)");
        withFrontendDevServer();

        classesChanged(ImportingGreeterJs.class);

        assertTrue(hotswapper.reported.isEmpty(),
                "the module of the import resolves as the build resolves it: "
                        + hotswapper.reported);
        String generated = readGeneratedDefinitions();
        assertTrue(generated.contains("greet(this, $0)"), generated);
        assertTrue(generated.contains("import(\"Frontend/greeting.js\")"),
                generated);
    }

    @Test
    void frontendDevServerRunningAndFileUpToDate_fileLeftAlone() {
        writeGeneratedDefinitionsFor(GreeterJs.class);
        // A moment in the past, so that a write of the same content shows
        generatedFile().setLastModified(System.currentTimeMillis() - 60_000);
        long untouched = generatedFile().lastModified();
        withFrontendDevServer();

        classesChanged(GreeterJs.class);

        assertTrue(hotswapper.reported.isEmpty(),
                "the browser is running what the interface declares: "
                        + hotswapper.reported);
        assertEquals(untouched, generatedFile().lastModified(),
                "a redefinition that changes no JavaScript should leave the file alone, or the dev server replaces the module in every browser for nothing");
    }

    @Test
    void frontendDevServerRunningButNothingCanBeWritten_reported() {
        // A directory where the file belongs: the change cannot be applied, so
        // it is reported rather than passing as applied
        generatedFile().mkdirs();
        withFrontendDevServer();

        classesChanged(GreeterJs.class);

        assertEquals(List.of(GreeterJs.class.getName()), hotswapper.reported);
    }

    @Test
    void snippetTypeChanged_definitionsReturningItReportedAndCacheCleared()
            throws IOException {
        writeGeneratedDefinitionsFor(GreeterJs.class);
        writeGeneratedDefinitionsFor(TemplateJs.class);
        // What the file holds for the definition when the type declared
        // other names than it does now
        editGeneratedDefinitions("return function (item) {",
                "return function (row) {");
        // A definition that is no longer on the class path, which is skipped
        Files.writeString(generatedFile().toPath(),
                "window.Vaadin.Flow.jsDefinitionNames[\"gone\"] = \"com.example.GoneJs.run/0\";"
                        + System.lineSeparator(),
                StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        List<Class<?>> cleared = new ArrayList<>();
        Registration registration = ReflectionCache
                .addClearAllAction(cleared::add);

        try {
            classesChanged(TemplateSnippet.class);

            assertEquals(List.of(TemplateJs.class.getName()),
                    hotswapper.reported,
                    "the definition returning the type now declares other JavaScript than the bundle carries");
            assertTrue(cleared.contains(TemplateJs.class),
                    "what was cached of the definition depends on the type: "
                            + cleared);
            assertFalse(cleared.contains(GreeterJs.class),
                    "a definition not returning the type is left alone");
        } finally {
            registration.remove();
        }
    }

    @Test
    void snippetTypeChangedWithADefinitionReturningIt_reportedOnce()
            throws IOException {
        writeGeneratedDefinitionsFor(TemplateJs.class);
        editGeneratedDefinitions("return function (item) {",
                "return function (row) {");

        classesChanged(TemplateSnippet.class, TemplateJs.class);

        assertEquals(List.of(TemplateJs.class.getName()), hotswapper.reported);
    }

    @Test
    void snippetTypeChanged_frontendDevServerRunning_definitionsReturningItApplied()
            throws IOException {
        writeGeneratedDefinitionsFor(TemplateJs.class);
        editGeneratedDefinitions("return function (item) {",
                "return function (row) {");
        withFrontendDevServer();

        classesChanged(TemplateSnippet.class);

        assertTrue(hotswapper.reported.isEmpty(),
                hotswapper.reported::toString);
        assertTrue(
                readGeneratedDefinitions().contains("return function (item) {"),
                "what the browser reloads is called with the names the type declares now");
    }

    @Test
    void extendedInterfaceChanged_definitionsExtendingItReported()
            throws IOException {
        writeGeneratedDefinitionsFor(GreeterJs.class);
        writeGeneratedDefinitionsFor(ScrollingListJs.class);
        // What the file holds when the extended interface declared other
        // JavaScript than it does now
        editGeneratedDefinitions("this.scrollTop = $0", "this.scrollLeft = $0");

        List<Class<?>> cleared = new ArrayList<>();
        Registration registration = ReflectionCache
                .addClearAllAction(cleared::add);

        try {
            classesChanged(ScrollableJs.class);

            assertEquals(List.of(ScrollingListJs.class.getName()),
                    hotswapper.reported,
                    "the definition extending the interface now declares other JavaScript than the bundle carries");
            assertTrue(cleared.contains(ScrollingListJs.class),
                    "what was cached of the definition depends on the interface: "
                            + cleared);
            assertFalse(cleared.contains(GreeterJs.class),
                    "a definition not extending the interface is left alone");
        } finally {
            registration.remove();
        }
    }

    @Test
    void extendedInterfaceWithoutExpressions_definitionsExtendingItReported()
            throws IOException {
        writeGeneratedDefinitionsFor(PopoverMenuJs.class);
        // What the file holds when the extended interface declared another
        // method than it does now
        editGeneratedDefinitions("return this.showPopover();",
                "return this.hidePopover();");

        classesChanged(PopoverJs.class);

        assertEquals(List.of(PopoverMenuJs.class.getName()),
                hotswapper.reported,
                "a method without @JsExpression declares JavaScript too");
    }

    @Test
    void extendedInterfaceRedefinedInPlace_definitionsExtendingItSeeTheNewJavaScript()
            throws IOException {
        // Copies, so that the redefinition leaves the classes of the other
        // cases alone. Defined without a code source, so a coverage agent does
        // not add members that the redefinition would then remove.
        Map<String, Class<?>> copies = new ByteBuddy()
                .redefine(ScrollableJs.class).make()
                .include(new ByteBuddy().redefine(ScrollingListJs.class).make())
                .load(new URLClassLoader(new URL[0],
                        getClass().getClassLoader()),
                        ClassLoadingStrategy.Default.CHILD_FIRST)
                .getAllLoaded().values().stream()
                .collect(Collectors.toMap(Class::getName, type -> type));
        Class<?> parent = copies.get(ScrollableJs.class.getName());
        Class<?> definition = copies.get(ScrollingListJs.class.getName());
        // What the bundle was built from, which also has the definition list
        // its methods before the redefinition
        writeGeneratedDefinitionsFor(definition);
        new JsCall(definition, "scrollTo", List.of(1)).getFunctionId();

        // In place, as the JVM redefines a class: the definition keeps the
        // copies of the inherited methods it made before
        ByteBuddyAgent.install();
        new ByteBuddy().redefine(EditedScrollableJs.class)
                .name(ScrollableJs.class.getName()).make()
                .load(parent.getClassLoader(),
                        ClassReloadingStrategy.fromInstalledAgent());
        // Done by ReflectionCacheHotswapper before the hotswappers run
        ReflectionCache.clearAll(parent);

        // The loader of the application, which is what the definitions the
        // generated file names are loaded with
        Thread thread = Thread.currentThread();
        ClassLoader contextClassLoader = thread.getContextClassLoader();
        thread.setContextClassLoader(parent.getClassLoader());
        try {
            classesChanged(parent);
        } finally {
            thread.setContextClassLoader(contextClassLoader);
        }

        assertEquals(List.of(definition.getName()), hotswapper.reported,
                "the definition now inherits other JavaScript than the bundle carries");
        assertEquals(JsCall.functionId("this.scrollLeft = $0", 1, false),
                new JsCall(definition, "scrollTo", List.of(1)).getFunctionId(),
                "a call should name the function of the JavaScript the interface declares now");
    }

    @Test
    void extendedDefinitionChanged_definitionsExtendingItReported()
            throws IOException {
        writeGeneratedDefinitionsFor(ScrollingListJs.class);
        writeGeneratedDefinitionsFor(DetailedListJs.class);
        editGeneratedDefinitions("this.scrollTop = this.scrollHeight",
                "this.scrollTop = 0");

        classesChanged(ScrollingListJs.class);

        assertEquals(
                Set.of(ScrollingListJs.class.getName(),
                        DetailedListJs.class.getName()),
                Set.copyOf(hotswapper.reported),
                "an annotated interface carries its JavaScript into the definitions extending it too");
    }

    @Test
    void noDefinitionChanged_nothingReported() {
        classesChanged(NotADefinition.class);

        assertTrue(hotswapper.reported.isEmpty(),
                "a class that declares no JavaScript is not a frontend change");
    }

    @Test
    void stringFallbackChanged_listedForTheServer() throws IOException {
        classesChanged(FallbackJs.class);

        File list = Path.of(projectFolder.getPath(), "target", "classes",
                Constants.VAADIN_SERVLET_RESOURCES,
                Constants.JS_STRING_FALLBACKS_FILE).toFile();
        assertTrue(Files.readString(list.toPath())
                .contains(FallbackJs.class.getName()), list::toString);
    }

    @Test
    void stringFallbackChanged_gradleProject_listedWithGradleResources()
            throws IOException {
        ((MockDeploymentConfiguration) service.getDeploymentConfiguration())
                .setApplicationOrSystemProperty(InitParameters.BUILD_FOLDER,
                        "build");
        // What Gradle has on the class path of the running application
        File gradleResources = Path
                .of(projectFolder.getPath(), "build", "resources", "main")
                .toFile();
        gradleResources.mkdirs();

        classesChanged(FallbackJs.class);

        File list = new File(gradleResources, Constants.VAADIN_SERVLET_RESOURCES
                + Constants.JS_STRING_FALLBACKS_FILE);
        assertTrue(Files.readString(list.toPath())
                .contains(FallbackJs.class.getName()), list::toString);
    }
}
