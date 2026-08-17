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

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the {@link JavaScript} dependencies that
 * {@link UIInternals#addComponentDependencies(Class)} adds to the page at
 * runtime instead of leaving them to the bundler.
 */
@NotThreadSafe
class RuntimeJavaScriptDependencyTest {

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
    @JsModule("https://example.net/module.js")
    private static class ExternalJsModule extends Component {
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
    void externalJsModule_stillAddedAsRuntimeJsModule() {
        assertSingleDependency(ExternalJsModule.class,
                Dependency.Type.JS_MODULE, "https://example.net/module.js",
                LoadMode.EAGER);
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

    private List<Dependency> runtimeDependencies(
            Class<? extends Component> componentType) {
        UIInternals internals = new MockUI().getInternals();
        internals.addComponentDependencies(componentType);

        Collection<Dependency> pending = internals.getDependencyList()
                .getPendingSendToClient();
        // Chunk loading adds a dynamic import for every component class
        return pending.stream()
                .filter(dependency -> dependency
                        .getType() != Dependency.Type.DYNAMIC_IMPORT)
                .collect(Collectors.toList());
    }
}
