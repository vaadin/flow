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
package com.vaadin.flow.dom;

import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.internal.BundleUtils;
import com.vaadin.flow.internal.JacksonCodec;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for referring to the values a class imports from JS modules from a
 * JavaScript expression sent to the client.
 */
class JsImportsTest {

    @JsModule(value = "lit-html", imports = { "render", "html" })
    private static class LitImports {
    }

    @JsModule(value = "lit-html", importAll = true)
    private static class LitNamespace {
    }

    @JsModule("lit-html")
    private static class SideEffectOnly {
    }

    private static class NoAnnotation {
    }

    @Test
    void of_classDeclaringNamedImports_referencesTheClass() {
        assertEquals(LitImports.class.getName(),
                JsImports.of(LitImports.class).getDeclaringClassName());
    }

    @Test
    void of_classDeclaringImportAll_referencesTheClass() {
        assertEquals(LitNamespace.class.getName(),
                JsImports.of(LitNamespace.class).getDeclaringClassName());
    }

    @Test
    void of_classWithoutImportDeclaration_throws() {
        // A plain @JsModule only loads the module for its side effects, so
        // there is nothing to hand to an expression
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JsImports.of(SideEffectOnly.class));
        assertTrue(exception.getMessage().contains("importAll"),
                "The message should say how to declare imports, was: "
                        + exception.getMessage());
    }

    @Test
    void of_classWithoutJsModule_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> JsImports.of(NoAnnotation.class));
    }

    @Test
    void of_null_throws() {
        assertThrows(NullPointerException.class, () -> JsImports.of(null));
    }

    @Test
    void chunkId_matchesDeclaringClassChunk() {
        // Must agree with what the generated loadOnDemand function expects
        assertEquals(BundleUtils.getChunkId(LitImports.class.getName()),
                JsImports.of(LitImports.class).getChunkId());
        assertNotEquals(JsImports.of(LitImports.class).getChunkId(),
                JsImports.of(LitNamespace.class).getChunkId());
    }

    @Test
    void equalsAndHashCode_basedOnDeclaringClass() {
        JsImports imports = JsImports.of(LitImports.class);

        assertEquals(imports, JsImports.of(LitImports.class));
        assertEquals(imports.hashCode(),
                JsImports.of(LitImports.class).hashCode());
        assertNotEquals(imports, JsImports.of(LitNamespace.class));
        assertNotEquals(imports, null);
        assertNotEquals(imports, LitImports.class.getName());
    }

    @Test
    void toString_namesTheDeclaringClass() {
        assertTrue(
                JsImports.of(LitImports.class).toString()
                        .contains(LitImports.class.getName()),
                "toString should help identify the declaration when debugging");
    }

    @Test
    void encodeWithTypeInfo_encodedAsChunkIdReference() {
        JsonNode encoded = JacksonCodec
                .encodeWithTypeInfo(JsImports.of(LitImports.class));

        assertEquals(1, encoded.size());
        assertEquals(JsImports.of(LitImports.class).getChunkId(),
                encoded.get("@v-imports").textValue());
        assertFalse(encoded.toString().contains(LitImports.class.getName()),
                "The Java class name should not be sent to the browser: "
                        + encoded);
    }

    @Test
    void executeJs_requestsTheChunkBeforeTheInvocation() {
        MockUI ui = new MockUI();
        Element element = new Element("div");
        ui.getElement().appendChild(element);

        element.executeJs("$0.render($0.html`<div>${$1}</div>`, this)",
                JsImports.of(LitImports.class), "Hello");
        flush(ui);

        assertEquals(List.of(loadOnDemand(LitImports.class)),
                dynamicImports(ui));
    }

    @Test
    void executeJs_sameImportsTwice_chunkRequestedOnce() {
        MockUI ui = new MockUI();
        Element element = new Element("div");
        ui.getElement().appendChild(element);

        element.executeJs("$0.render(this)", JsImports.of(LitImports.class));
        element.executeJs("$0.html`x`", JsImports.of(LitImports.class));
        flush(ui);

        assertEquals(List.of(loadOnDemand(LitImports.class)),
                dynamicImports(ui));
    }

    @Test
    void executeJs_differentImports_bothChunksRequested() {
        MockUI ui = new MockUI();
        Element element = new Element("div");
        ui.getElement().appendChild(element);

        element.executeJs("$0.render(this); $1.html`x`",
                JsImports.of(LitImports.class),
                JsImports.of(LitNamespace.class));
        flush(ui);

        assertEquals(List.of(loadOnDemand(LitImports.class),
                loadOnDemand(LitNamespace.class)), dynamicImports(ui));
    }

    @Test
    void executeJs_importsCapturedByJsFunction_chunkRequested() {
        MockUI ui = new MockUI();
        Element element = new Element("div");
        ui.getElement().appendChild(element);

        element.executeJs("$0()", JsFunction.of("$0.render(this)",
                JsImports.of(LitImports.class)));
        flush(ui);

        assertEquals(List.of(loadOnDemand(LitImports.class)),
                dynamicImports(ui));
    }

    @Test
    void executeJs_withoutImports_noChunkRequested() {
        MockUI ui = new MockUI();
        Element element = new Element("div");
        ui.getElement().appendChild(element);

        element.executeJs("console.log($0)", "Hello");
        flush(ui);

        assertEquals(List.of(), dynamicImports(ui));
    }

    private static String loadOnDemand(Class<?> declaringClass) {
        return "return window.Vaadin.Flow.loadOnDemand('"
                + BundleUtils.getChunkId(declaringClass.getName()) + "');";
    }

    /**
     * Runs what a response would run before collecting the dependencies, i.e.
     * the point at which the invocations are handed to the UI.
     */
    private static void flush(UI ui) {
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
    }

    private static List<String> dynamicImports(UI ui) {
        return ui.getInternals().getDependencyList().getPendingSendToClient()
                .stream()
                .filter(dependency -> dependency
                        .getType() == Dependency.Type.DYNAMIC_IMPORT)
                .map(Dependency::getUrl).toList();
    }
}
