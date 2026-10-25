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
package com.vaadin.flow.server;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.ClickNotifier;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.CspViolationReporter;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class CspModeTest {

    @Tag("div")
    private static class Clickable extends Component
            implements ClickNotifier<Clickable> {
    }

    private MockServletServiceSessionSetup setup;
    private MockUI ui;

    @BeforeEach
    void setUp() {
        setup = new MockServletServiceSessionSetup();
        ui = new MockUI(setup.getSession());
    }

    @AfterEach
    void tearDown() {
        setup.cleanup();
        CurrentInstance.clearAll();
    }

    static Stream<Arguments> stringJavaScriptCalls() {
        return Stream.of(
                call(Element.class, "executeJs",
                        ui -> ui.getElement().executeJs("return 1")),
                call(Element.class, "addJsInitializer",
                        ui -> ui.getElement().addJsInitializer("return 1")),
                call(Page.class, "executeJs",
                        ui -> ui.getPage().executeJs("return 1")),
                call(Page.class, "addDynamicImport",
                        ui -> ui.getPage().addDynamicImport("return 1")),
                call(JsFunction.class, "of", ui -> JsFunction.of("return 1")),
                call(DomListenerRegistration.class, "addEventData",
                        ui -> ui.getElement().addEventListener("click", e -> {
                        }).addEventData("event.x")),
                call(DomListenerRegistration.class, "addEventData",
                        ui -> ui.getElement().addEventListener("click", e -> {
                        }).addEventData(List.of("event.x"))),
                call(DomListenerRegistration.class, "setFilter",
                        ui -> ui.getElement().addEventListener("click", e -> {
                        }).setFilter("event.x")),
                // Vaadin code sending event data and a filter on its own
                call(DomListenerRegistration.class, "addEventData",
                        ui -> new Clickable().addDoubleClickListener(e -> {
                        })));
    }

    private static Arguments call(Class<?> api, String method,
            Consumer<UI> call) {
        return Arguments.of(api, method, call);
    }

    @ParameterizedTest(name = "{0}.{1}")
    @MethodSource("stringJavaScriptCalls")
    void strict_stringJavaScript_throwsWithTraceFromApiToCaller(Class<?> api,
            String method, Consumer<UI> call) {
        setup.getDeploymentConfiguration().setCspMode(CspMode.STRICT);

        CspViolationException exception = assertThrows(
                CspViolationException.class, () -> call.accept(ui));

        assertTraceLeadsFromApiToTest(exception, api, method);
    }

    @Test
    void strict_javaScriptWithoutString_doesNotThrow() {
        setup.getDeploymentConfiguration().setCspMode(CspMode.STRICT);

        assertDoesNotThrow(() -> {
            ui.getElement().callJsFunction("focus");
            ui.getPage().reload();
            DomListenerRegistration registration = ui.getElement()
                    .addEventListener("click", e -> {
                    });
            registration.setFilter(null);
            registration.addEventData(List.of());
        });
    }

    @Test
    void strict_stringJavaScriptWithoutCurrentService_doesNotThrow() {
        setup.getDeploymentConfiguration().setCspMode(CspMode.STRICT);
        // As for a component built in a background thread before attaching
        CurrentInstance.clearAll();

        assertDoesNotThrow(() -> {
            new Element("div").executeJs("return 1");
            JsFunction.of("return 1");
        });
    }

    @ParameterizedTest
    @CsvSource({ "WARN, 2", "OFF, 0" })
    void warnOrOff_stringJavaScript_runsAndLogsOncePerCallSiteOnlyInWarn(
            CspMode mode, int expectedWarnings) {
        setup.getDeploymentConfiguration().setCspMode(mode);
        Logger logger = mock(Logger.class);
        try (MockedStatic<LoggerFactory> loggerFactory = mockStatic(
                LoggerFactory.class)) {
            loggerFactory.when(
                    () -> LoggerFactory.getLogger(CspViolationReporter.class))
                    .thenReturn(logger);

            // Two callers of one line that calls the API are one call site
            executeJsOnUi();
            executeJsOnUi();
            ui.getElement().executeJs("return 2");
        }

        assertEquals(3, ui.dumpPendingJsInvocations().size());
        ArgumentCaptor<Throwable> warnings = ArgumentCaptor
                .forClass(Throwable.class);
        verify(logger, times(expectedWarnings)).warn(anyString(),
                warnings.capture());
        for (Throwable warning : warnings.getAllValues()) {
            assertTraceLeadsFromApiToTest(warning, Element.class, "executeJs");
        }
    }

    private void executeJsOnUi() {
        ui.getElement().executeJs("return 1");
    }

    private static void assertTraceLeadsFromApiToTest(Throwable violation,
            Class<?> api, String method) {
        StackTraceElement[] stack = violation.getStackTrace();
        assertTrue(api.isAssignableFrom(loadClass(stack[0].getClassName())),
                stack[0].toString());
        assertEquals(method, stack[0].getMethodName());
        assertTrue(
                Stream.of(stack)
                        .noneMatch(frame -> frame.getClassName()
                                .equals(CspViolationReporter.class.getName())),
                Arrays.toString(stack));
    }

    private static Class<?> loadClass(String className) {
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new AssertionError(e);
        }
    }
}
