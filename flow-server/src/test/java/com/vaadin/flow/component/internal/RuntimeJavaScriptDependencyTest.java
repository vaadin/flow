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
package com.vaadin.flow.component.internal;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.tests.util.MockUI;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link JavaScript} dependencies that
 * {@link UIInternals#addComponentDependencies(Class)} adds to the page at
 * runtime instead of leaving them to the bundler.
 */
@NotThreadSafe
class RuntimeJavaScriptDependencyTest {

    private static final String MISSING_FROM_BUNDLE_ERROR = "was not included when creating the production bundle";

    @Tag("div")
    @JavaScript(value = "module.js", type = JavaScript.Type.MODULE)
    private static class RelativeModule extends Component {
    }

    @Tag("div")
    @JavaScript("script.js")
    private static class RelativeScript extends Component {
    }

    @Tag("div")
    @JavaScript("https://example.net/script.js")
    private static class ExternalScript extends Component {
    }

    @Tag("div")
    @JavaScript("https://example.net/pkg/../dist/script.js")
    private static class ExternalScriptWithParentPath extends Component {
    }

    @Tag("div")
    @JavaScript(value = "context://module.js", type = JavaScript.Type.MODULE)
    private static class ContextModule extends Component {
    }

    @Tag("div")
    @JavaScript(value = "module.js", type = JavaScript.Type.MODULE, loadMode = LoadMode.LAZY)
    private static class LazyModule extends Component {
    }

    @Tag("div")
    @JavaScript(value = "module.js", type = JavaScript.Type.MODULE, loadMode = LoadMode.INLINE)
    private static class InlineModule extends Component {
    }

    @Tag("div")
    @JavaScript(value = "../outside.js", type = JavaScript.Type.MODULE)
    private static class ParentPathModule extends Component {
    }

    @Tag("div")
    @JavaScript(value = "../outside.js", type = JavaScript.Type.MODULE, loadMode = LoadMode.INLINE)
    private static class InlineParentPathModule extends Component {
    }

    @Tag("div")
    @JsModule("https://example.net/module.js")
    private static class ExternalJsModule extends Component {
    }

    @Tag("div")
    @JavaScript(value = "devtools.js", type = JavaScript.Type.MODULE, developmentOnly = true)
    private static class DevelopmentOnlyModule extends Component {
    }

    @Tag("div")
    @JavaScript(value = "https://example.net/devtools.js", developmentOnly = true)
    private static class DevelopmentOnlyExternalScript extends Component {
    }

    @Tag("div")
    @JsModule(value = "https://example.net/devtools.js", developmentOnly = true)
    private static class DevelopmentOnlyExternalJsModule extends Component {
    }

    @Tag("div")
    @JavaScript(value = "not-bundled-module.js", type = JavaScript.Type.MODULE)
    private static class ProductionRelativeModule extends Component {
    }

    @Tag("div")
    @JavaScript("not-bundled-script.js")
    private static class ProductionRelativeScript extends Component {
    }

    @AfterEach
    void after() {
        UI.setCurrent(null);
    }

    @Test
    void relativeModule_addedAsRuntimeJsModuleFromContextRoot() {
        assertSingleDependency(RelativeModule.class, Dependency.Type.JS_MODULE,
                "context://module.js", LoadMode.EAGER);
    }

    @Test
    void relativeScript_notAddedAtRuntime() {
        // A bare relative @JavaScript value keeps the legacy interpretation of
        // being bundled, so it must not show up as a page dependency
        assertEquals(List.of(), runtimeDependencies(RelativeScript.class));
    }

    @Test
    void externalScript_addedAsRuntimeJavaScript() {
        assertSingleDependency(ExternalScript.class, Dependency.Type.JAVASCRIPT,
                "https://example.net/script.js", LoadMode.EAGER);
    }

    @Test
    void externalScriptWithParentPath_urlPassedThroughUntouched() {
        // External URLs are not normalized, so a '..' segment inside one must
        // not cause the dependency to be dropped
        assertSingleDependency(ExternalScriptWithParentPath.class,
                Dependency.Type.JAVASCRIPT,
                "https://example.net/pkg/../dist/script.js", LoadMode.EAGER);
    }

    @Test
    void contextModule_urlPassedThroughUntouched() {
        assertSingleDependency(ContextModule.class, Dependency.Type.JS_MODULE,
                "context://module.js", LoadMode.EAGER);
    }

    @Test
    void lazyModule_loadModePassedThrough() {
        assertSingleDependency(LazyModule.class, Dependency.Type.JS_MODULE,
                "context://module.js", LoadMode.LAZY);
    }

    @Test
    void inlineModule_throwsNamingTheComponent() {
        UIInternals internals = new MockUI().getInternals();

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> internals.addComponentDependencies(InlineModule.class));

        assertTrue(
                exception.getMessage().contains(InlineModule.class.getName()),
                "Exception message should name the offending component, was: "
                        + exception.getMessage());
    }

    @Test
    void parentPathModule_rejectedAndSkipped() {
        assertEquals(List.of(), runtimeDependencies(ParentPathModule.class));
    }

    @Test
    void inlineParentPathModule_throwsEvenThoughValueWouldBeRejected() {
        // The unsupported load mode is a programming error regardless of
        // whether the value itself survives normalization
        UIInternals internals = new MockUI().getInternals();

        assertThrows(IllegalArgumentException.class, () -> internals
                .addComponentDependencies(InlineParentPathModule.class));
    }

    @Test
    void productionMode_relativeModule_noMissingBundleImportError()
            throws Exception {
        String log = productionModeLog(ProductionRelativeModule.class);

        assertFalse(log.contains(MISSING_FROM_BUNDLE_ERROR),
                "A type=MODULE value is deliberately kept out of the bundle, so it must not be reported as missing. Log was: "
                        + log);
    }

    @Test
    void productionMode_relativeScript_stillReportsMissingBundleImport()
            throws Exception {
        // Control for the test above: proves the assertion there is not
        // vacuous, i.e. that this log capture does observe the error
        String log = productionModeLog(ProductionRelativeScript.class);

        assertTrue(log.contains(MISSING_FROM_BUNDLE_ERROR),
                "A bundled type=SCRIPT value missing from the bundle should still be reported. Log was: "
                        + log);
    }

    @Test
    void externalJsModule_stillAddedAsRuntimeJsModule() {
        assertSingleDependency(ExternalJsModule.class,
                Dependency.Type.JS_MODULE, "https://example.net/module.js",
                LoadMode.EAGER);
    }

    @Test
    void developmentOnlyModule_addedInDevelopmentMode() {
        assertSingleDependency(DevelopmentOnlyModule.class,
                Dependency.Type.JS_MODULE, "context://devtools.js",
                LoadMode.EAGER);
    }

    @Test
    void developmentOnlyModule_notAddedInProductionMode() throws Exception {
        // Runtime dependencies bypass the bundle, so nothing else keeps a
        // development only value out of a production application
        assertEquals(List.of(),
                productionModeDependencies(DevelopmentOnlyModule.class));
    }

    @Test
    void developmentOnlyExternalScript_notAddedInProductionMode()
            throws Exception {
        assertEquals(List.of(), productionModeDependencies(
                DevelopmentOnlyExternalScript.class));
    }

    @Test
    void developmentOnlyExternalJsModule_notAddedInProductionMode()
            throws Exception {
        assertEquals(List.of(), productionModeDependencies(
                DevelopmentOnlyExternalJsModule.class));
    }

    @Test
    void module_stillAddedInProductionMode() throws Exception {
        // Guards against the development only filter dropping everything
        assertEquals(List.of("context://module.js"),
                productionModeDependencies(RelativeModule.class));
    }

    private void assertSingleDependency(
            Class<? extends Component> componentType,
            Dependency.Type expectedType, String expectedUrl,
            LoadMode expectedLoadMode) {
        List<Dependency> dependencies = runtimeDependencies(componentType);

        assertEquals(1, dependencies.size(),
                "Expected exactly one dependency, got " + dependencies);
        Dependency dependency = dependencies.get(0);
        assertEquals(expectedType, dependency.getType(), "Type mismatch");
        assertEquals(expectedUrl, dependency.getUrl(), "URL mismatch");
        assertEquals(expectedLoadMode, dependency.getLoadMode(),
                "LoadMode mismatch");
    }

    /**
     * Runs {@link UIInternals#addComponentDependencies(Class)} in production
     * mode against a pretend production bundle that contains neither of the
     * test files, and returns whatever was logged while doing so.
     */
    private String productionModeLog(Class<? extends Component> componentType)
            throws Exception {
        MockServletServiceSessionSetup mocks = new MockServletServiceSessionSetup();
        mocks.setProductionMode(true);
        UIInternals internals = new MockUI(mocks.getSession()).getInternals();

        Field bundledImports = UIInternals.class
                .getDeclaredField("bundledImports");
        bundledImports.setAccessible(true);
        Object originalImports = bundledImports.get(null);
        Field warnedAboutDeps = UIInternals.class
                .getDeclaredField("warnedAboutDeps");
        warnedAboutDeps.setAccessible(true);
        // Warnings are remembered statically, so clear them to keep this test
        // independent of execution order
        ((Set<?>) warnedAboutDeps.get(null)).clear();

        PrintStream originalErr = System.err;
        ByteArrayOutputStream captured = new ByteArrayOutputStream();
        try {
            bundledImports.set(null, Set.of("some-other-import.js"));
            System.setErr(new PrintStream(captured, true, UTF_8));

            internals.addComponentDependencies(componentType);
        } finally {
            System.setErr(originalErr);
            bundledImports.set(null, originalImports);
            mocks.cleanup();
        }
        return captured.toString(UTF_8);
    }

    /**
     * Same as {@link #runtimeDependencies(Class)} but with a session that runs
     * in production mode, returning just the dependency URLs.
     */
    private List<String> productionModeDependencies(
            Class<? extends Component> componentType) throws Exception {
        MockServletServiceSessionSetup mocks = new MockServletServiceSessionSetup();
        mocks.setProductionMode(true);
        try {
            UIInternals internals = new MockUI(mocks.getSession())
                    .getInternals();
            internals.addComponentDependencies(componentType);

            return internals.getDependencyList().getPendingSendToClient()
                    .stream()
                    .filter(dependency -> dependency
                            .getType() != Dependency.Type.DYNAMIC_IMPORT)
                    .map(Dependency::getUrl).toList();
        } finally {
            mocks.cleanup();
        }
    }

    private List<Dependency> runtimeDependencies(
            Class<? extends Component> componentType) {
        UIInternals internals = new MockUI().getInternals();
        internals.addComponentDependencies(componentType);

        Collection<Dependency> pending = internals.getDependencyList()
                .getPendingSendToClient();
        // Chunk loading adds a dynamic import for every component class
        return pending.stream().filter(dependency -> dependency
                .getType() != Dependency.Type.DYNAMIC_IMPORT).toList();
    }
}
