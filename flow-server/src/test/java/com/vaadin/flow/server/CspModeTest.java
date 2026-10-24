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
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.CspViolationReporter;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
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
                call("Element.executeJs(String, Object...)",
                        ui -> ui.getElement().executeJs("return 1")),
                call("Element.addJsInitializer(String, Object...)",
                        ui -> ui.getElement().addJsInitializer("return 1")),
                call("Page.executeJs(String, Object...)",
                        ui -> ui.getPage().executeJs("return 1")),
                call("Page.addDynamicImport(String)",
                        ui -> ui.getPage().addDynamicImport("return 1")),
                call("JsFunction.of(String, Object...)",
                        ui -> JsFunction.of("return 1")),
                call("DomListenerRegistration.addEventData(String)",
                        ui -> ui.getElement().addEventListener("click", e -> {
                        }).addEventData("event.x")),
                call("DomListenerRegistration.addEventData(List)",
                        ui -> ui.getElement().addEventListener("click", e -> {
                        }).addEventData(List.of("event.x"))),
                call("DomListenerRegistration.setFilter(String)",
                        ui -> ui.getElement().addEventListener("click", e -> {
                        }).setFilter("event.x")));
    }

    private static Arguments call(String api, Consumer<UI> call) {
        return Arguments.of(api, call);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("stringJavaScriptCalls")
    void strict_stringJavaScript_throwsNamingApiAndCaller(String api,
            Consumer<UI> call) {
        setup.getDeploymentConfiguration().setCspMode(CspMode.STRICT);

        CspViolationException exception = assertThrows(
                CspViolationException.class, () -> call.accept(ui));

        String message = exception.getMessage();
        assertTrue(message.startsWith(
                api + " called at " + CspModeTest.class.getName() + ".lambda$"),
                message);
        assertFalse(message.contains(" via "), message);
    }

    @Test
    void strict_stringJavaScriptFromFramework_namesFrameworkCallee() {
        setup.getDeploymentConfiguration().setCspMode(CspMode.STRICT);
        Clickable clickable = new Clickable();

        CspViolationException exception = assertThrows(
                CspViolationException.class,
                () -> clickable.addDoubleClickListener(event -> {
                }));

        String message = exception.getMessage();
        assertTrue(message.contains(
                " called at " + CspModeTest.class.getName() + ".lambda$"),
                message);
        assertTrue(message.contains(" via " + ClickNotifier.class.getName()
                + ".addDoubleClickListener("), message);
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

            for (int i = 0; i < 2; i++) {
                ui.getElement().executeJs("return 1");
            }
            ui.getElement().executeJs("return 2");
        }

        assertEquals(3, ui.dumpPendingJsInvocations().size());
        ArgumentCaptor<String> messages = ArgumentCaptor.forClass(String.class);
        verify(logger, times(expectedWarnings)).warn(messages.capture());
        String callSite = "Element.executeJs(String, Object...) called at "
                + CspModeTest.class.getName()
                + ".warnOrOff_stringJavaScript_runsAndLogsOncePerCallSiteOnlyInWarn(CspModeTest.java:";
        assertTrue(
                messages.getAllValues().stream()
                        .allMatch(message -> message.startsWith(callSite)),
                messages.getAllValues().toString());
        assertEquals(expectedWarnings,
                messages.getAllValues().stream().distinct().count());
    }
}
