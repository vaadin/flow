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
package com.vaadin.flow.internal;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

import com.vaadin.flow.component.internal.BundledEventData;
import com.vaadin.flow.component.page.PendingJavaScriptResult;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.change.MapPutChange;
import com.vaadin.flow.internal.change.NodeChange;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsFunctionParameterNames;
import com.vaadin.flow.js.JsStringFallback;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.CspMode;
import com.vaadin.flow.server.CspViolationException;
import com.vaadin.flow.server.MockServletServiceSessionSetup;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsStringFallbacksTest {

    private static final String FILTER = "event.key === 'Escape'";

    private static final String EVENT_DATA = "event.target.value.trim().length";

    private static final String IMPORT = "return Promise.resolve()";

    @JsDefinition
    @JsStringFallback(filters = FILTER, eventData = EVENT_DATA)
    interface AddonFallbacks {
        @JsExpression("return this.scrollTop")
        PendingJavaScriptResult scrollTop();

        @JsExpression("document.title = $0 + ' (' + $1 + ')'")
        void setTitle(Object title, Object count);
    }

    @JsDefinition
    @JsStringFallback
    interface FunctionFallbacks {
        @JsExpression("this.title = $0")
        JsFunction setTitle(Object title);

        @JsExpression(IMPORT)
        JsFunction load();
    }

    @JsDefinition
    @JsStringFallback
    interface NamedFunctionFallbacks {
        @JsExpression("return value * 2")
        @JsFunctionParameterNames("value")
        JsFunction twice();
    }

    @JsDefinition
    @JsStringFallback(filters = "event.detail")
    interface PathFilterFallbacks {
    }

    @JsDefinition
    @JsStringFallback
    interface FocusFallbacks {
        @JsExpression("this.focus()")
        void focus();
    }

    @JsDefinition
    @JsStringFallback
    interface OverloadedFallbacks {
        @JsExpression("console.log('text', $0)")
        void log(String text);

        @JsExpression("console.log('number', $0)")
        void log(Integer number);
    }

    @JsDefinition
    @JsStringFallback
    interface DuplicateFallbacks {
        @JsExpression("return this.scrollTop")
        PendingJavaScriptResult readScrollTop();
    }

    @JsDefinition
    @JsStringFallback
    interface VariadicFallbacks {
        @JsExpression("console.log(...$0)")
        void log(Object... values);
    }

    @TempDir
    File resources;

    private MockServletServiceSessionSetup setup;
    private MockUI ui;

    @BeforeEach
    void setUp() throws Exception {
        setup = new MockServletServiceSessionSetup();
        ui = new MockUI(setup.getSession());
        listFallbacks("[\"" + AddonFallbacks.class.getName() + "\", \""
                + FunctionFallbacks.class.getName() + "\", \""
                + NamedFunctionFallbacks.class.getName() + "\"]");
    }

    @AfterEach
    void tearDown() {
        setup.cleanup();
        CurrentInstance.clearAll();
    }

    @ParameterizedTest
    @EnumSource(value = CspMode.class, names = { "WARN", "STRICT" })
    void reportingMode_matchingElementCall_sendsDeclaredCall(CspMode mode) {
        setup.getDeploymentConfiguration().setCspMode(mode);

        ui.getElement().executeJs("return this.scrollTop");

        JsCall call = ui.onlyScheduledJsCall();
        assertEquals(AddonFallbacks.class, call.definitionType());
        assertEquals("scrollTop", call.methodName());
        assertEquals(List.of(), call.arguments());
    }

    @Test
    void strict_matchingPageCall_sendsDeclaredCallWithParameters() {
        setup.getDeploymentConfiguration().setCspMode(CspMode.STRICT);

        ui.getPage().executeJs("document.title = $0 + ' (' + $1 + ')'", "Inbox",
                3);

        JsCall call = ui.onlyScheduledJsCall();
        assertEquals("setTitle", call.methodName());
        assertEquals(List.of("Inbox", 3), call.arguments());
    }

    @Test
    void strict_otherTextOrParameterCount_isRefused() {
        setup.getDeploymentConfiguration().setCspMode(CspMode.STRICT);

        Element element = ui.getElement();
        assertThrows(CspViolationException.class,
                () -> element.executeJs("return  this.scrollTop"));
        assertThrows(CspViolationException.class,
                () -> element.executeJs("return this.scrollTop", 1));
    }

    @Test
    void off_matchingCall_sendsString() {
        setup.getDeploymentConfiguration().setCspMode(CspMode.OFF);

        ui.getElement().executeJs("return this.scrollTop");

        assertNull(ui.onlyScheduledInvocation().getJsCall());
    }

    @Test
    void strict_listNamesMissingClass_throwsNamingIt() throws IOException {
        setup.getDeploymentConfiguration().setCspMode(CspMode.STRICT);
        listFallbacks("[\"com.example.RemovedFallbacks\"]");

        Element element = ui.getElement();
        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> element.executeJs("return this.scrollTop"));

        assertTrue(
                exception.getMessage().contains("com.example.RemovedFallbacks"),
                exception.getMessage());
    }

    @Test
    void strict_reloadAfterListChanged_matchesTheNewList() throws IOException {
        setup.getDeploymentConfiguration().setCspMode(CspMode.STRICT);
        ui.getElement().executeJs("return this.scrollTop");
        ui.dumpPendingJsInvocations();
        listFallbacks("[\"" + FocusFallbacks.class.getName() + "\"]");

        JsStringFallbacks.reloadFallbacks(setup.getService());

        ui.getElement().executeJs("this.focus()");
        assertEquals(FocusFallbacks.class,
                ui.onlyScheduledJsCall().definitionType());
        Element element = ui.getElement();
        assertThrows(CspViolationException.class,
                () -> element.executeJs("return this.scrollTop"));
    }

    @Test
    void strict_matchingFunctionBody_usesDeclaredFunction() {
        setup.getDeploymentConfiguration().setCspMode(CspMode.STRICT);

        JsFunction function = JsFunction.of("this.title = $0", "tip");

        JsCall call = function.getDeclaredCall();
        assertEquals(FunctionFallbacks.class, call.definitionType());
        assertEquals("setTitle", call.methodName());
        assertEquals(List.of("tip"), call.arguments());
        // An initializer and a dynamic import are functions made from strings
        assertDoesNotThrow(() -> {
            ui.getElement().addJsInitializer("this.title = $0", "tip");
            ui.getPage().addDynamicImport(IMPORT);
        });
        assertThrows(CspViolationException.class,
                () -> JsFunction.of("this.title = $0"));
    }

    @Test
    void strict_matchingFilterAndEventData_useBundledFunctions() {
        setup.getDeploymentConfiguration().setCspMode(CspMode.STRICT);
        DomListenerRegistration registration = ui.getElement()
                .addEventListener("keydown", event -> {
                });

        registration.setFilter(FILTER);
        registration.addEventData(EVENT_DATA);

        assertEquals(FILTER, registration.getFilter());
        // The client runs the function of the bundle instead of the string
        JsonNode eventData = eventSettings(ui.getElement(), "keydown")
                .get(EVENT_DATA);
        assertEquals(BundledEventData.getFunctionId(EVENT_DATA), eventData
                .get(JsonConstants.EVENT_SETTINGS_FUNCTION).asString());
        assertThrows(CspViolationException.class,
                () -> registration.setFilter("event.key === 'Enter'"));
        assertThrows(CspViolationException.class, () -> registration
                .addEventData("event.target.value.length + 1"));
    }

    private static JsonNode eventSettings(Element element, String eventType) {
        List<NodeChange> changes = new ArrayList<>();
        element.getNode().getFeature(ElementListenerMap.class)
                .collectChanges(changes::add);
        ConstantPoolKey key = (ConstantPoolKey) changes.stream()
                .filter(MapPutChange.class::isInstance)
                .map(MapPutChange.class::cast)
                .filter(change -> eventType.equals(change.getKey())).findFirst()
                .orElseThrow().getValue();
        ObjectNode constants = JacksonUtils.createObjectNode();
        key.export(constants);
        return constants.get(key.getId());
    }

    @Test
    void strict_functionWithDeclaredNames_withTheSameArguments_usesDeclaredFunction() {
        setup.getDeploymentConfiguration().setCspMode(CspMode.STRICT);

        JsFunction function = JsFunction.of("return value * 2")
                .withArguments("value");

        assertEquals(NamedFunctionFallbacks.class,
                function.getDeclaredCall().definitionType());
        assertEquals("twice", function.getDeclaredCall().methodName());
    }

    @Test
    void strict_functionWithoutDeclaredNames_withArguments_isRefusedWithTheNames() {
        setup.getDeploymentConfiguration().setCspMode(CspMode.STRICT);
        JsFunction function = JsFunction.of("this.title = $0", "tip");

        CspViolationException exception = assertThrows(
                CspViolationException.class,
                () -> function.withArguments("event"));

        assertTrue(
                exception.getMessage()
                        .contains("@JsFunctionParameterNames({ \"event\" })"),
                exception.getMessage());
    }

    @Test
    @SuppressWarnings("removal")
    void warn_functionWithOtherNames_withArguments_sendsTheString() {
        setup.getDeploymentConfiguration().setCspMode(CspMode.WARN);

        JsFunction function = JsFunction.of("return value * 2")
                .withArguments("other");

        assertNull(function.getDeclaredCall());
        assertEquals("return value * 2", function.getBody());
        assertEquals(List.of("other"), function.getArgumentNames());
    }

    @Test
    void collectFallbacks_pathFilter_throws() {
        List<Class<?>> types = List.of(PathFilterFallbacks.class);
        IllegalArgumentException path = assertThrows(
                IllegalArgumentException.class,
                () -> JsStringFallbacks.collectFallbacks(types));
        assertTrue(path.getMessage().contains("event.detail"),
                path.getMessage());
    }

    @Test
    void collectFallbacks_sameNameAndParameterCount_throws() {
        List<Class<?>> types = List.of(OverloadedFallbacks.class);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JsStringFallbacks.collectFallbacks(types));

        assertTrue(exception.getMessage().contains("same name"),
                exception.getMessage());
        assertTrue(exception.getMessage().contains("log"),
                exception.getMessage());
    }

    @Test
    void collectFallbacks_variadicMethod_throws() {
        List<Class<?>> types = List.of(VariadicFallbacks.class);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JsStringFallbacks.collectFallbacks(types));

        assertTrue(exception.getMessage().contains("log"),
                exception.getMessage());
        assertTrue(exception.getMessage().contains("variadic"),
                exception.getMessage());
    }

    @Test
    void collectFallbacks_sameJavaScriptTwice_throws() {
        List<Class<?>> types = List.of(AddonFallbacks.class,
                DuplicateFallbacks.class);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> JsStringFallbacks.collectFallbacks(types));

        assertTrue(exception.getMessage().contains("scrollTop"),
                exception.getMessage());
        assertTrue(exception.getMessage().contains("readScrollTop"),
                exception.getMessage());
    }

    /**
     * Gives the service a class loader that finds the list the build writes,
     * with the given content.
     */
    private void listFallbacks(String json) throws IOException {
        File list = new File(resources, Constants.VAADIN_SERVLET_RESOURCES
                + Constants.JS_STRING_FALLBACKS_FILE);
        list.getParentFile().mkdirs();
        Files.writeString(list.toPath(), json);
        setup.getService()
                .setClassLoader(new URLClassLoader(
                        new URL[] { resources.toURI().toURL() },
                        getClass().getClassLoader()));
    }
}
