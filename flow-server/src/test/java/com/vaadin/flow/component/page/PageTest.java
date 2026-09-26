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
package com.vaadin.flow.component.page;

import java.io.Serializable;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.Direction;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.internal.PendingJavaScriptInvocation;
import com.vaadin.flow.component.internal.UIInternals.JavaScriptInvocation;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.js.JsCall;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionProxy;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.tests.util.MockDeploymentConfiguration;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class PageTest {

    @JsDefinition
    interface TestPageJs extends Serializable {
        @JsExpression("window.alert($0)")
        void showGreeting(String greeting);

        @JsExpression("return navigator.clipboard.readText()")
        PendingJavaScriptResult readText();
    }

    @Test
    void executeJsWithDefinition_schedulesTheCallOnNothingInParticular() {
        MockUI mockUI = new MockUI();

        mockUI.getPage().executeJs(TestPageJs.class).showGreeting("Hello");

        JavaScriptInvocation invocation = mockUI.onlyScheduledInvocation();
        assertEquals(
                new JsCall(TestPageJs.class, "showGreeting", List.of("Hello")),
                invocation.getJsCall());
        assertEquals(Arrays.asList("Hello", null), invocation.getParameters(),
                "the arguments should be followed by nothing to run the function on");
    }

    @Test
    void executeJsWithDefinition_methodDeclaringAResult_answersWithTheExecution() {
        MockUI mockUI = new MockUI();

        PendingJavaScriptResult result = mockUI.getPage()
                .executeJs(TestPageJs.class).readText();
        List<String> values = new ArrayList<>();
        result.then(String.class, values::add);

        List<PendingJavaScriptInvocation> invocations = mockUI.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(1, invocations.size());
        assertSame(result, invocations.get(0),
                "the scheduled invocation is what the method answers with");
        assertTrue(invocations.get(0).isSubscribed(),
                "and the return value should be asked for from the client");

        invocations.get(0).complete(JacksonUtils.createNode("text"));
        assertEquals(List.of("text"), values);
    }

    private class TestUI extends UI {
        @Override
        public Page getPage() {
            return page;
        }
    }

    private class TestPage extends Page {

        private int count = 0;

        private String expression;

        private Object firstParam;

        public TestPage(UI ui) {
            super(ui);
        }

        @Override
        public PendingJavaScriptResult executeJs(String expression,
                Object... parameters) {
            this.expression = expression;
            firstParam = parameters[0];
            count++;
            return null;
        }
    }

    private UI ui = new TestUI();

    private TestPage page = new TestPage(ui);

    private BrowserWindowResizeListener listener = event -> {
    };

    @Test

    void addNullAsAListener_trows() {
        assertThrows(NullPointerException.class, () -> {
            page.addBrowserWindowResizeListener(null);
        });
    }

    @Test
    void retrieveExtendedClientDetails_twice_jsOnceAndCallbackTwice() {
        // given
        final MockUI mockUI = new MockUI();
        final Page page = new Page(mockUI) {
            @Override
            public <T> T executeJs(Class<T> definitionType) {
                // The details are asked for through declared JavaScript, so
                // the stub has to answer that call rather than an expression
                return JsDefinitionProxy.create(definitionType, call -> {
                    super.executeJs(call.getExpression());
                    return answerWithDetails();
                });
            }

            @Override
            public PendingJavaScriptResult executeJs(String expression,
                    Object... params) {
                super.executeJs(expression, params);
                return answerWithDetails();
            }

            private PendingJavaScriptResult answerWithDetails() {
                return new PendingJavaScriptResult() {

                    @Override
                    public boolean cancelExecution() {
                        return false;
                    }

                    @Override
                    public boolean isSentToBrowser() {
                        return false;
                    }

                    @Override
                    public void then(
                            SerializableConsumer<JsonNode> resultHandler,
                            SerializableConsumer<String> errorHandler) {
                        final HashMap<String, String> params = new HashMap<>();
                        params.put("v-sw", "2560");
                        params.put("v-sh", "1450");
                        params.put("v-tzo", "-270");
                        params.put("v-rtzo", "-210");
                        params.put("v-dstd", "60");
                        params.put("v-dston", "true");
                        params.put("v-tzid", "Asia/Tehran");
                        params.put("v-curdate", "1555000000000");
                        params.put("v-td", "false");
                        params.put("v-wn", "ROOT-1234567-0.1234567");
                        resultHandler.accept(JacksonUtils.createObject(params,
                                JacksonUtils::createNode));
                    }
                };
            }
        };
        mockUI.setPage(page);
        final AtomicInteger callbackInvocations = new AtomicInteger();
        final Page.ExtendedClientDetailsReceiver receiver = details -> {
            callbackInvocations.incrementAndGet();
        };

        // when
        page.retrieveExtendedClientDetails(receiver);
        page.retrieveExtendedClientDetails(receiver);

        // then
        final int jsInvocations = mockUI.getInternals()
                .dumpPendingJavaScriptInvocations().size();
        assertEquals(1, jsInvocations);
        assertEquals(2, callbackInvocations.get());
    }

    @Test
    void reload_runsTheDeclaredJavaScript() {
        MockUI mockUI = new MockUI();

        mockUI.getPage().reload();

        List<PendingJavaScriptInvocation> invocations = mockUI.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(1, invocations.size());
        assertEquals(new JsCall(Page.PageJs.class, "reload", List.of()),
                invocations.get(0).getInvocation().getJsCall());
    }

    @Test
    void fetchPageDirection_consumerReceivesTheDirection() {
        MockUI mockUI = new MockUI();
        AtomicReference<Direction> received = new AtomicReference<>();

        mockUI.getPage().fetchPageDirection(received::set);

        List<PendingJavaScriptInvocation> invocations = mockUI.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(1, invocations.size());
        assertEquals(new JsCall(Page.PageJs.class, "readDirection", List.of()),
                invocations.get(0).getInvocation().getJsCall(),
                "the direction should be asked for through the declared JavaScript");

        invocations.get(0).complete(JacksonUtils.createNode("rtl"));
        assertEquals(Direction.RIGHT_TO_LEFT, received.get());
    }

    @Test
    void fetchCurrentUrl_consumerReceivesCorrectURL() {
        // given
        final UI mockUI = new MockUI();
        final AtomicReference<URL> callbackInvocations = new AtomicReference<>();
        final SerializableConsumer<URL> receiver = details -> {
            callbackInvocations.compareAndSet(null, details);
        };

        // when
        mockUI.getPage().fetchCurrentURL(receiver);

        // then
        List<PendingJavaScriptInvocation> invocations = mockUI.getInternals()
                .dumpPendingJavaScriptInvocations();
        assertEquals(1, invocations.size());
        assertEquals(new JsCall(Page.PageJs.class, "getHref", List.of()),
                invocations.get(0).getInvocation().getJsCall(),
                "the address should be asked for through the declared JavaScript");

        invocations.get(0).complete(
                JacksonUtils.createNode("http://localhost:8080/home"));
        assertEquals("http://localhost:8080/home",
                callbackInvocations.get().toString(), "Returned URL was wrong");
    }

    @Test
    void fetchCurrentUrl_passNullCallback_throwsNullPointerException() {
        final UI mockUI = new MockUI();
        Page page = new Page(mockUI);
        assertThrows(NullPointerException.class,
                () -> page.fetchCurrentURL(null));
    }

    @Test
    void addJsModule_accepts_onlyExternalAndStartingSlash() {
        List<String> urls = new LinkedList<>();
        urls.add("http://sample.com/mod.js");
        urls.add("https://sample.com/mod.js");
        urls.add("//sample.com/mod.js");
        urls.add("/mod.js");

        for (String url : urls) {
            page.addJsModule(url);
        }

        Collection<Dependency> pendingSendToClient = ui.getInternals()
                .getDependencyList().getPendingSendToClient();

        assertEquals(4, pendingSendToClient.size(),
                "There should be 4 dependencies added.");

        for (Dependency dependency : pendingSendToClient) {
            assertEquals(Dependency.Type.JS_MODULE, dependency.getType(),
                    "Dependency should be a JSModule");
            assertEquals(LoadMode.EAGER, dependency.getLoadMode(),
                    "JS module dependency should be EAGER");

            assertTrue(urls.contains(dependency.getUrl()),
                    "Dependency " + dependency.getUrl()
                            + " is not found in the source list.");

            urls.remove(dependency.getUrl());
        }

        assertEquals(0, urls.size(), "Not all urls were added as dependencies");
    }

    @Test
    void addJsModule_rejects_files() {
        try {
            page.addJsModule("mod.js");

            fail("Adding a file without starting \"/\" is not to be allowed.");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    void executeJavaScript_delegatesToExecJs() {
        AtomicReference<String> invokedExpression = new AtomicReference<>();
        AtomicReference<Object[]> invokedParams = new AtomicReference<>();

        Page page = new Page(new MockUI()) {
            @Override
            public PendingJavaScriptResult executeJs(String expression,
                    Object... parameters) {
                String oldExpression = invokedExpression.getAndSet(expression);
                assertNull(oldExpression, "There should be no old expression");

                Object[] oldParams = invokedParams.getAndSet(parameters);
                assertNull(oldParams, "There should be no old params");

                return null;
            }
        };

        PendingJavaScriptResult executionCanceler = page.executeJs("foo", 1,
                true);

        assertNull(executionCanceler);

        assertEquals("foo", invokedExpression.get());
        assertEquals(Integer.valueOf(1), invokedParams.get()[0]);
        assertEquals(Boolean.TRUE, invokedParams.get()[1]);
    }

    @Test
    void open_openInSameWindow_closeTheClientApplication() {
        AtomicReference<String> capture = new AtomicReference<>();
        List<Object> params = new ArrayList<>();
        Page page = new Page(new MockUI()) {
            @Override
            public PendingJavaScriptResult executeJs(String expression,
                    Object... parameters) {
                capture.set(expression);
                params.addAll(Arrays.asList(parameters));
                return Mockito.mock(PendingJavaScriptResult.class);
            }
        };

        page.setLocation("foo");

        // self check
        assertEquals("_self", params.get(1));

        MatcherAssert.assertThat(capture.get(),
                CoreMatchers.containsString("this.stopApplication();"));
    }

    @Test
    void setLocation_dispatchesRedirectPendingEvent() {
        AtomicReference<String> capture = new AtomicReference<>();
        List<Object> params = new ArrayList<>();
        Page page = new Page(new MockUI()) {
            @Override
            public PendingJavaScriptResult executeJs(String expression,
                    Object... parameters) {
                capture.set(expression);
                params.addAll(Arrays.asList(parameters));
                return Mockito.mock(PendingJavaScriptResult.class);
            }
        };

        page.setLocation("/logout-landing");

        String expression = capture.get();
        assertTrue(expression.contains("vaadin-redirect-pending"),
                "Should dispatch vaadin-redirect-pending event");
        assertTrue(expression.contains("window.open"),
                "Should call window.open");
        assertEquals("/logout-landing", params.get(0),
                "URL parameter should be passed");
    }

    @Test
    void open_dispatchesRedirectPendingEventBeforeRedirect() {
        AtomicReference<String> capture = new AtomicReference<>();
        Page page = new Page(new MockUI()) {
            @Override
            public PendingJavaScriptResult executeJs(String expression,
                    Object... parameters) {
                capture.set(expression);
                return Mockito.mock(PendingJavaScriptResult.class);
            }
        };

        page.open("https://example.com", "_blank");

        String expression = capture.get();
        // Verify event dispatch comes before window.open
        int eventDispatchIndex = expression.indexOf("vaadin-redirect-pending");
        int windowOpenIndex = expression.indexOf("window.open");
        assertTrue(eventDispatchIndex >= 0, "Event dispatch should be present");
        assertTrue(windowOpenIndex >= 0, "window.open should be present");
        assertTrue(eventDispatchIndex < windowOpenIndex,
                "Event dispatch should come before window.open in the script");
    }

    @Test
    void open_unsafeScheme_throws() {
        Page page = new Page(new MockUI()) {
            @Override
            public PendingJavaScriptResult executeJs(String expression,
                    Object... parameters) {
                return fail("Unsafe URL should not reach the client");
            }
        };

        assertThrows(IllegalArgumentException.class,
                () -> page.open("javascript:alert(1)"));
        assertThrows(IllegalArgumentException.class,
                () -> page.open("javascript:alert(1)", "_blank"));
    }

    @Test
    void setLocation_unsafeScheme_throws() {
        Page page = new Page(new MockUI()) {
            @Override
            public PendingJavaScriptResult executeJs(String expression,
                    Object... parameters) {
                return fail("Unsafe URL should not reach the client");
            }
        };

        assertThrows(IllegalArgumentException.class,
                () -> page.setLocation("javascript:alert(1)"));
    }

    @Test
    void open_nullUrl_throwsWithUsefulMessage() {
        Page page = new Page(new MockUI()) {
            @Override
            public PendingJavaScriptResult executeJs(String expression,
                    Object... parameters) {
                return fail("Null URL should not reach the client");
            }
        };

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> page.open(null, "_blank"));
        assertEquals("URL must not be null", ex.getMessage());
    }

    @Test
    void open_unsafeInApplicationConfiguration_usesConfigurationOfOwnUi() {
        // The UI is always known, so the configuration of the right application
        // is used even though the URL is safe according to the framework
        // default
        Page page = new Page(createUI("https")) {
            @Override
            public PendingJavaScriptResult executeJs(String expression,
                    Object... parameters) {
                return fail("Unsafe URL should not reach the client");
            }
        };

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> page.open("http://example.com"));
        assertTrue(exception.getMessage().contains("http://example.com"));
    }

    @Test
    void open_safeInApplicationConfiguration_opens() {
        AtomicReference<String> capture = new AtomicReference<>();
        Page page = new Page(createUI("https")) {
            @Override
            public PendingJavaScriptResult executeJs(String expression,
                    Object... parameters) {
                capture.set(expression);
                return Mockito.mock(PendingJavaScriptResult.class);
            }
        };

        page.open("https://example.com");

        assertTrue(capture.get().contains("window.open"));
    }

    /**
     * Creates a UI that belongs to an application configured to only allow the
     * given URL schemes, without making the service available through
     * {@link VaadinService#getCurrent()}.
     */
    private UI createUI(String safeUrlSchemes) {
        MockDeploymentConfiguration configuration = new MockDeploymentConfiguration();
        configuration.setApplicationOrSystemProperty(
                InitParameters.URL_SAFE_SCHEMES, safeUrlSchemes);

        VaadinService service = Mockito.mock(VaadinService.class);
        Mockito.when(service.getDeploymentConfiguration())
                .thenReturn(configuration);
        VaadinSession session = Mockito.mock(VaadinSession.class);
        Mockito.when(session.getService()).thenReturn(service);

        UI ui = new UI();
        ui.getInternals().setSession(session);
        return ui;
    }

    @Test
    void openUnsafe_unsafeScheme_opensWithoutValidation() {
        AtomicReference<String> capture = new AtomicReference<>();
        List<Object> params = new ArrayList<>();
        Page page = new Page(new MockUI()) {
            @Override
            public PendingJavaScriptResult executeJs(String expression,
                    Object... parameters) {
                capture.set(expression);
                params.addAll(Arrays.asList(parameters));
                return Mockito.mock(PendingJavaScriptResult.class);
            }
        };

        page.openUnsafe("javascript:alert(1)");

        assertTrue(capture.get().contains("window.open"),
                "Should call window.open");
        assertEquals("javascript:alert(1)", params.get(0));
    }

    @Test
    void openUnsafe_twoArg_opensWithoutValidation() {
        AtomicReference<String> capture = new AtomicReference<>();
        List<Object> params = new ArrayList<>();
        Page page = new Page(new MockUI()) {
            @Override
            public PendingJavaScriptResult executeJs(String expression,
                    Object... parameters) {
                capture.set(expression);
                params.addAll(Arrays.asList(parameters));
                return Mockito.mock(PendingJavaScriptResult.class);
            }
        };

        page.openUnsafe("javascript:alert(1)", "_blank");

        assertTrue(capture.get().contains("window.open"),
                "Should call window.open");
        assertEquals("javascript:alert(1)", params.get(0));
        assertEquals("_blank", params.get(1));
    }

    @Test
    void setColorScheme_setsStyleProperty() {
        MockUI mockUI = new MockUI();

        mockUI.getPage().setColorScheme(ColorScheme.Value.DARK);

        assertEquals(
                new JsCall(Page.PageJs.class, "setColorScheme",
                        List.of("dark", "dark")),
                mockUI.onlyScheduledJsCall(),
                "the theme and the color scheme should be set to 'dark'");
        assertEquals(ColorScheme.Value.DARK, mockUI.getPage().getColorScheme());
    }

    @Test
    void setColorScheme_lightDark_setsCorrectValues() {
        MockUI mockUI = new MockUI();

        mockUI.getPage().setColorScheme(ColorScheme.Value.LIGHT_DARK);

        // The theme attribute uses a hyphen where the color scheme property
        // uses a space
        assertEquals(
                new JsCall(Page.PageJs.class, "setColorScheme",
                        List.of("light-dark", "light dark")),
                mockUI.onlyScheduledJsCall());
    }

    @Test
    void setColorScheme_nullOrNormal_clearsProperty() {
        // Both mean the same thing: let the document follow the browser again
        for (ColorScheme.Value value : new ColorScheme.Value[] { null,
                ColorScheme.Value.NORMAL }) {
            MockUI mockUI = new MockUI();

            mockUI.getPage().setColorScheme(value);

            assertEquals(
                    new JsCall(Page.PageJs.class, "resetColorScheme",
                            List.of()),
                    mockUI.onlyScheduledJsCall(), "for " + value);
            assertEquals(ColorScheme.Value.NORMAL,
                    mockUI.getPage().getColorScheme());
        }
    }

    @Test
    void settingAndClearingTheColorScheme_declareTheirOwnJavaScript() {
        // The two are told apart by the method that was called, so what each
        // declares is what is left to get wrong
        MockUI mockUI = MockUI.createUI();

        mockUI.getPage().setColorScheme(ColorScheme.Value.DARK);
        assertTrue(mockUI.onlyScheduledJsCall().getExpression()
                .contains("setAttribute('theme', $0)"));

        mockUI.getPage().setColorScheme(null);
        assertTrue(mockUI.onlyScheduledJsCall().getExpression()
                .contains("removeAttribute('theme')"));
    }

    @Test
    void getColorScheme_returnsNormal_whenNotSet() {
        Page page = new Page(new MockUI());
        assertEquals(ColorScheme.Value.NORMAL, page.getColorScheme());
    }

    @Test
    void getColorScheme_returnsCachedValue() {
        MockUI mockUI = new MockUI();
        // Set up ExtendedClientDetails with color scheme
        ExtendedClientDetails details = new ExtendedClientDetails(mockUI, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, "dark", null);
        mockUI.getInternals().setExtendedClientDetails(details);

        Page page = new Page(mockUI);
        assertEquals(ColorScheme.Value.DARK, page.getColorScheme());
    }

    @Test
    void setColorScheme_updatesGetColorScheme() {
        MockUI mockUI = new MockUI();
        // Set up ExtendedClientDetails
        ExtendedClientDetails details = new ExtendedClientDetails(mockUI, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
        mockUI.getInternals().setExtendedClientDetails(details);

        Page page = mockUI.getPage();

        assertEquals(ColorScheme.Value.NORMAL, page.getColorScheme());

        page.setColorScheme(ColorScheme.Value.DARK);
        assertEquals(ColorScheme.Value.DARK, page.getColorScheme());

        page.setColorScheme(ColorScheme.Value.LIGHT);
        assertEquals(ColorScheme.Value.LIGHT, page.getColorScheme());

        page.setColorScheme(null);
        assertEquals(ColorScheme.Value.NORMAL, page.getColorScheme());

        page.setColorScheme(ColorScheme.Value.NORMAL);
        assertEquals(ColorScheme.Value.NORMAL, page.getColorScheme());
    }
}
