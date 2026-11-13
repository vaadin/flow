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

import java.io.Serializable;
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
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.DomEvent;
import com.vaadin.flow.component.EventData;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.KeyModifier;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.page.Page;
import com.vaadin.flow.component.webcomponent.WebComponentUI;
import com.vaadin.flow.dom.DomEventData;
import com.vaadin.flow.dom.DomEventFilter;
import com.vaadin.flow.dom.DomListenerRegistration;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.dom.JsFunction;
import com.vaadin.flow.internal.CspViolationReporter;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import com.vaadin.flow.js.JsCalledWith;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsSnippets;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.JsonConstants;
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

    @JsDefinition
    interface LoggerJs extends Serializable {
        @JsExpression("console.log($0, value);")
        @JsCalledWith("value")
        JsFunction createLogger(String prefix);
    }

    @JsDefinition
    interface EventJs extends Serializable {
        @JsExpression("return event.x == $0;")
        DomEventFilter isX(int x);

        @JsExpression("return event.x * $0;")
        DomEventData<Integer> scaledX(int factor);
    }

    @JsDefinition
    interface LocatorJs extends Serializable {
        @JsExpression("return this.firstElementChild;")
        JsFunction locateFirstChild();
    }

    record AsciiDetail(int x, String label) {
    }

    record NonAsciiDetail(int größe) {
    }

    @Tag("div")
    private static class Clickable extends Component
            implements ClickNotifier<Clickable> {
    }

    @Route("csp")
    @Tag("div")
    private static class RouteView extends Component {
    }

    @DomEvent("custom-event")
    public static class KeyedEvent extends ComponentEvent<UI> {
        public KeyedEvent(UI source, boolean fromClient,
                @EventData("key") String key) {
            super(source, fromClient);
        }
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
                // Reported once, by the JsFunction the initializer runs
                call(JsFunction.class, "of",
                        ui -> ui.getElement().addJsInitializer("return 1")),
                call(Page.class, "executeJs",
                        ui -> ui.getPage().executeJs("return 1")),
                call(Page.class, "addDynamicImport",
                        ui -> ui.getPage().addDynamicImport("return 1")),
                call(JsFunction.class, "of", ui -> JsFunction.of("return 1")),
                call(DomListenerRegistration.class, "addEventData",
                        ui -> ui.getElement().addEventListener("click", e -> {
                        }).addEventData("event.x * 2")),
                call(DomListenerRegistration.class, "addEventData",
                        ui -> ElementListenerMap.addEventData(
                                ui.getElement().addEventListener("click", e -> {
                                }),
                                List.of(JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN
                                        + "value", "event.x[0]"))),
                call(DomListenerRegistration.class, "addEventData",
                        ui -> ui.getElement().addEventListener("click", e -> {
                        }).addEventDataElement("event.composedPath()[0]")),
                call(DomListenerRegistration.class, "addEventData",
                        ui -> ui.getElement().addEventListener("click", e -> {
                        }).addEventData("window.location")),
                call(DomListenerRegistration.class, "addEventData",
                        ui -> ui.getElement().addEventListener("click", e -> {
                        }).addEventData("event??.x")),
                call(DomListenerRegistration.class, "addEventData",
                        ui -> ui.getElement().addEventListener("click", e -> {
                        }).addEventData("event.?x")),
                call(DomListenerRegistration.class, "addEventData",
                        ui -> ui.getElement().addEventListener("click", e -> {
                        }).addEventDataElement("document.head")),
                // Not a property path the browser reads without compiling
                call(DomListenerRegistration.class, "addEventData",
                        ui -> ui.getElement().addEventListener("click", e -> {
                        }).addEventDetail(NonAsciiDetail.class)),
                call(DomListenerRegistration.class, "addEventData",
                        ui -> ui.getElement().addEventListener("click", e -> {
                        }).addEventData(AsciiDetail.class)),
                call(DomListenerRegistration.class, "setFilter",
                        ui -> ui.getElement().addEventListener("click", e -> {
                        }).setFilter("event.x == 1")),
                // The deprecated locator is an expression for the browser
                call(JsFunction.class, "of",
                        CspModeTest::delegateShortcutToStringLocator),
                // A key of client-built data only for Flow's own events
                call(DomListenerRegistration.class, "addEventData",
                        ui -> ComponentUtil.addListener(ui, KeyedEvent.class,
                                e -> {
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
            ElementListenerMap.addEventData(registration, List.of());
            ElementListenerMap.addEventData(registration,
                    List.of(JsonConstants.SYNCHRONIZE_PROPERTY_TOKEN + "value",
                            JsonConstants.MAP_STATE_NODE_EVENT_DATA));
            registration.synchronizeProperty("value");
            registration.mapEventTargetElement();
            // The client reads property paths without compiling them
            registration.addEventData("event.detail.value");
            registration.addEventData("event.detail?.value");
            ElementListenerMap.addEventData(registration,
                    List.of("element.value", "event"));
            registration.addEventDataPath("event.detail?.value");
            registration.addEventDataElementPath("event.target.parentElement");
            registration.addEventDetail();
            registration.addEventDetail(AsciiDetail.class);
            registration.addEventDataElement("event.target.parentElement");
            registration.setFilter("event.isTrusted");
            // The client runs these without compiling them, also when Flow
            // adds the always-true filter for the debounce
            registration.preventDefault();
            registration.stopPropagation();
            registration.debounce(300);
            // Its JavaScript is in the bundle rather than in a string
            JsSnippets.of(LoggerJs.class).createLogger("Greeting:");
            EventJs eventJs = JsSnippets.of(EventJs.class);
            registration.setFilterExpression(eventJs.isX(2))
                    .addEventData(eventJs.scaledX(2));
            // The click count filters are declared snippets too
            Clickable clickable = new Clickable();
            clickable.addSingleClickListener(e -> {
            });
            clickable.addDoubleClickListener(e -> {
            });
            // Loads the chunk of the route with a function from the bundle
            ui.getInternals().addComponentDependencies(RouteView.class);
            // Shortcuts send their keys and options as data to JavaScript in
            // the bundle, also when they listen on a browser-only element
            ui.addShortcutListener(() -> {
            }, Key.KEY_A, KeyModifier.CONTROL);
            Clickable owner = new Clickable();
            ui.add(owner);
            Shortcuts.setShortcutListenOnElementLocator(
                    JsSnippets.of(LocatorJs.class).locateFirstChild(), owner);
            owner.addClickShortcut(Key.ENTER).listenOn(owner);
            ui.getInternals().getStateTree()
                    .runExecutionsBeforeClientResponse();
        });
    }

    @Test
    void strict_uiStartsWithoutViolation() {
        setup.getDeploymentConfiguration().setCspMode(CspMode.STRICT);

        // The constructor of Page adds the detail of an event, and the events
        // of the navigation and of connecting a web component carry keys of
        // data that client code builds, not expressions
        assertDoesNotThrow(() -> {
            new MockUI(setup.getSession()).doInit(null, 1, "app");
            WebComponentUI webComponentUI = new WebComponentUI();
            webComponentUI.getInternals().setSession(setup.getSession());
            webComponentUI.doInit(null, 2, "app");
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

    @SuppressWarnings("removal")
    private static void delegateShortcutToStringLocator(UI ui) {
        Clickable owner = new Clickable();
        ui.add(owner);
        Shortcuts.setShortcutListenOnElement("this.firstElementChild", owner);
        owner.addClickShortcut(Key.ENTER).listenOn(owner);
        ui.getInternals().getStateTree().runExecutionsBeforeClientResponse();
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
