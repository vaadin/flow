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
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Isolated
class PageTest {
    private MockUI ui = new MockUI();
    private Page page = ui.getPage();

    @AfterEach
    void tearDown() {
        UI.setCurrent(null);
    }

    @Test
    void testAddNullStyleSheet() {
        assertThrows(IllegalArgumentException.class, () -> {
            page.addStyleSheet(null);
        });
    }

    @Test
    void addStyleSheet_withLayer_dependencyHasLayer() {
        page.addStyleSheet("styles.css", LoadMode.LAZY, "theme.base");
        // The same URL without a layer keeps the layer it was first added to
        page.addStyleSheet("styles.css", LoadMode.LAZY);

        Collection<Dependency> dependencies = ui.getInternals()
                .getDependencyList().getPendingSendToClient();
        assertEquals(1, dependencies.size());
        Dependency dependency = dependencies.iterator().next();
        assertEquals("styles.css", dependency.getUrl());
        assertEquals("theme.base", dependency.getLayer());
    }

    @Test
    void addStyleSheet_invalidLayer_throws() {
        assertThrows(IllegalArgumentException.class, () -> page
                .addStyleSheet("styles.css", LoadMode.EAGER, "a;} body{"));
    }

    @Test
    void testAddNullJavaScript() {
        assertThrows(IllegalArgumentException.class, () -> {
            page.addJavaScript(null);
        });
    }

    @Test
    void testJavasScriptExecutionCancel() {
        assertEquals(0, countPendingInvocations());

        PendingJavaScriptResult executeJavaScript = page
                .executeJs("window.alert('$0');", "foobar");

        assertEquals(1, countPendingInvocations());

        assertTrue(executeJavaScript.cancelExecution());

        assertEquals(0, countPendingInvocations());
    }

    @Test
    void testJavaScriptExecutionTooLateCancel() {
        assertEquals(0, countPendingInvocations());

        PendingJavaScriptResult executeJavaScript = page
                .executeJs("window.alert('$0');", "foobar");

        assertEquals(1, countPendingInvocations());

        assertEquals(1,
                ui.getInternals().dumpPendingJavaScriptInvocations().size());

        assertEquals(0, countPendingInvocations());

        assertFalse(executeJavaScript.cancelExecution());
    }

    @Test
    void addDynamicImport_dynamicDependencyIsAvaialbleViaGetPendingSendToClient() {
        page.addDynamicImport("foo");

        DependencyList list = ui.getInternals().getDependencyList();
        Collection<Dependency> dependencies = list.getPendingSendToClient();
        assertEquals(1, dependencies.size());
        Dependency dependency = dependencies.iterator().next();
        assertEquals("foo", dependency.getUrl());
    }

    @Test
    void addDynamicImportFunction_sameCapturesLoadedOnce() {
        page.addDynamicImport(JsFunction.of("return import($0)", "a"));
        page.addDynamicImport(JsFunction.of("return import($0)", "a"));
        page.addDynamicImport(JsFunction.of("return import($0)", "b"));

        List<String> captures = ui.getInternals().getDependencyList()
                .getPendingSendToClient().stream()
                .map(dependency -> dependency.getEncodedDynamicImport()
                        .get("@v-fn").get("captures").get(0).asString())
                .toList();
        assertEquals(List.of("a", "b"), captures);
    }

    @Test
    void addDynamicImportFunction_capturedElement_throws() {
        assertCaptureRejected(ui.getElement());
    }

    @Test
    void addDynamicImportFunction_capturedDetachedElement_throws() {
        // Encoded as null, so only the check on the Java value catches it
        Element detached = new Element("div");
        assertFalse(detached.getNode().isAttached());

        assertCaptureRejected(detached);
    }

    @Test
    void addDynamicImportFunction_capturedDetachedComponent_throws() {
        assertCaptureRejected(new TestComponent());
    }

    static Stream<Object> containersWithElement() {
        // Detached, so only the check on the Java values can catch them
        return Stream.of(List.of("a", new Element("div")),
                Map.of("k", new Element("div")),
                new Object[] { new Element("div") });
    }

    @ParameterizedTest
    @MethodSource("containersWithElement")
    void addDynamicImportFunction_elementInContainer_throws(Object capture) {
        assertCaptureRejected(capture);
    }

    @Test
    void addDynamicImportFunction_elementInNestedFunction_throws() {
        assertCaptureRejected(JsFunction.of("return $0", new Element("div")));
    }

    @Test
    void addDynamicImportFunction_attachedElementInBean_throws() {
        // Only seen in the encoded value, as beans are not walked
        assertCaptureRejected(new ElementHolder(ui.getElement()));
    }

    @Test
    void addDynamicImportFunction_detachedElementInBean_isSentAsNull() {
        page.addDynamicImport(JsFunction.of("return import($0)",
                new ElementHolder(new Element("div"))));

        Dependency dependency = ui.getInternals().getDependencyList()
                .getPendingSendToClient().iterator().next();
        assertTrue(dependency.getEncodedDynamicImport().get("@v-fn")
                .get("captures").get(0).get("element").isNull());
    }

    public record ElementHolder(Element element) {
    }

    private void assertCaptureRejected(Object capture) {
        JsFunction function = JsFunction.of("return import($0)", capture);

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> page.addDynamicImport(function));
        assertTrue(exception.getMessage().contains("element"));
        assertTrue(ui.getInternals().getDependencyList()
                .getPendingSendToClient().isEmpty());
    }

    @Tag("div")
    private static class TestComponent extends Component {
    }

    private long countPendingInvocations() {
        return ui.getInternals().getPendingJavaScriptInvocations().count();
    }

}
