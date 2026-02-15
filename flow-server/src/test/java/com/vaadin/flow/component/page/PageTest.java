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

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.tests.util.MockUI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

class PageTest {

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

    public void addNullAsAListener_trows() {
        assertThrows(NullPointerException.class, () -> {
            page.addBrowserWindowResizeListener(null);
        });
    }

    @Test
    public void retrieveExtendedClientDetails_twice_jsOnceAndCallbackTwice() {
        // given
        final MockUI mockUI = new MockUI();
        final Page page = new Page(mockUI) {
            @Override
            public PendingJavaScriptResult executeJs(String expression,
                    Object... params) {
                super.executeJs(expression, params);

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
    public void fetchCurrentUrl_consumerReceivesCorrectURL() {
        // given
        final UI mockUI = new MockUI();
        final Page page = new Page(mockUI) {
            @Override
            public PendingJavaScriptResult executeJs(String expression,
                    Object... params) {
                super.executeJs(expression, params);
                assertEquals("return window.location.href", expression,
                        "Expected javascript for fetching location is wrong.");

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
                        resultHandler.accept(JacksonUtils
                                .createNode("http://localhost:8080/home"));
                    }
                };
            }
        };
        final AtomicReference<URL> callbackInvocations = new AtomicReference<>();
        final SerializableConsumer<URL> receiver = details -> {
            callbackInvocations.compareAndSet(null, details);
        };

        // when
        page.fetchCurrentURL(receiver);

        // then
        assertEquals("http://localhost:8080/home",
                callbackInvocations.get().toString(), "Returned URL was wrong");
    }

    @Test
    public void fetchCurrentUrl_passNullCallback_throwsNullPointerException() {
        assertThrows(NullPointerException.class, () -> {
            final UI mockUI = new MockUI();
            Page page = new Page(mockUI);
            page.fetchCurrentURL(null);
        });
    }

    @Test
    public void addJsModule_accepts_onlyExternalAndStartingSlash() {
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
    public void addJsModule_rejects_files() {
        try {
            page.addJsModule("mod.js");

            fail("Adding a file without starting \"/\" is not to be allowed.");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void executeJavaScript_delegatesToExecJs() {
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
    public void open_openInSameWindow_closeTheClientApplication() {
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
    public void setLocation_dispatchesRedirectPendingEvent() {
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
    public void open_dispatchesRedirectPendingEventBeforeRedirect() {
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
    public void setColorScheme_setsStyleProperty() {
        AtomicReference<String> capturedExpression = new AtomicReference<>();
        AtomicReference<Object[]> capturedParams = new AtomicReference<>();
        MockUI mockUI = new MockUI();
        Page page = new Page(mockUI) {
            @Override
            public PendingJavaScriptResult executeJs(String expression,
                    Object... parameters) {
                capturedExpression.set(expression);
                capturedParams.set(parameters);
                return Mockito.mock(PendingJavaScriptResult.class);
            }
        };

        page.setColorScheme(ColorScheme.Value.DARK);

        String js = capturedExpression.get();
        assertTrue(js.contains("setAttribute('theme', $0)"),
                "Should set theme attribute");
        assertTrue(js.contains("style.colorScheme = $1"),
                "Should set color-scheme property");
        Object[] params = capturedParams.get();
        assertEquals("dark", params[0], "Theme attribute should be 'dark'");
        assertEquals("dark", params[1],
                "Color scheme property should be 'dark'");
    }

    @Test
    public void setColorScheme_lightDark_setsCorrectValues() {
        AtomicReference<String> capturedExpression = new AtomicReference<>();
        AtomicReference<Object[]> capturedParams = new AtomicReference<>();
        MockUI mockUI = new MockUI();
        Page page = new Page(mockUI) {
            @Override
            public PendingJavaScriptResult executeJs(String expression,
                    Object... parameters) {
                capturedExpression.set(expression);
                capturedParams.set(parameters);
                return Mockito.mock(PendingJavaScriptResult.class);
            }
        };

        page.setColorScheme(ColorScheme.Value.LIGHT_DARK);

        String js = capturedExpression.get();
        assertTrue(js.contains("setAttribute('theme', $0)"),
                "Should set theme attribute");
        assertTrue(js.contains("style.colorScheme = $1"),
                "Should set color-scheme property");
        Object[] params = capturedParams.get();
        assertEquals("light-dark", params[0],
                "Theme attribute should use hyphen");
        assertEquals("light dark", params[1],
                "Color scheme property should use space");
    }

    @Test
    public void setColorScheme_null_clearsProperty() {
        MockUI mockUI = new MockUI();

        AtomicReference<String> capturedExpression = new AtomicReference<>();
        Page page = new Page(mockUI) {
            @Override
            public PendingJavaScriptResult executeJs(String expression,
                    Object... parameters) {
                capturedExpression.set(expression);
                return Mockito.mock(PendingJavaScriptResult.class);
            }
        };

        page.setColorScheme(null);

        String js = capturedExpression.get();
        assertTrue(js.contains("removeAttribute('theme')"),
                "Should remove theme attribute");
        assertTrue(js.contains("style.colorScheme = ''"),
                "Should clear inline style");
        assertEquals(ColorScheme.Value.NORMAL, page.getColorScheme());
    }

    @Test
    public void setColorScheme_normal_clearsProperty() {
        MockUI mockUI = new MockUI();

        AtomicReference<String> capturedExpression = new AtomicReference<>();
        Page page = new Page(mockUI) {
            @Override
            public PendingJavaScriptResult executeJs(String expression,
                    Object... parameters) {
                capturedExpression.set(expression);
                return Mockito.mock(PendingJavaScriptResult.class);
            }
        };

        page.setColorScheme(ColorScheme.Value.NORMAL);

        String js = capturedExpression.get();
        assertTrue(js.contains("style.colorScheme = ''"));
        assertEquals(ColorScheme.Value.NORMAL, page.getColorScheme());
    }

    @Test
    public void getColorScheme_returnsNormal_whenNotSet() {
        Page page = new Page(new MockUI());
        assertEquals(ColorScheme.Value.NORMAL, page.getColorScheme());
    }

    @Test
    public void getColorScheme_returnsCachedValue() {
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
    public void setColorScheme_updatesGetColorScheme() {
        MockUI mockUI = new MockUI();
        // Set up ExtendedClientDetails
        ExtendedClientDetails details = new ExtendedClientDetails(mockUI, null,
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null);
        mockUI.getInternals().setExtendedClientDetails(details);

        Page page = new Page(mockUI) {
            @Override
            public PendingJavaScriptResult executeJs(String expression,
                    Object... parameters) {
                return Mockito.mock(PendingJavaScriptResult.class);
            }
        };

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
