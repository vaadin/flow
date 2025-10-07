/*
 * Copyright 2000-2025 Vaadin Ltd.
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
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import tools.jackson.databind.JsonNode;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JacksonUtils;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;
import com.vaadin.tests.util.MockUI;

public class PageTest {

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

    @Test(expected = NullPointerException.class)

    public void addNullAsAListener_trows() {
        page.addBrowserWindowResizeListener(null);
    }

    @Test
    public void retrieveExtendedClientDetails_twice_theSecondResultComesDifferentBeforeCachedValueIsSet() {
        // given
        final UI mockUI = new MockUI();
        List<Runnable> invocations = new ArrayList<>();
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
                        if (invocations.isEmpty()) {
                            params.put("v-wn", "ROOT-1234567-0.1234567");
                        } else {
                            params.put("v-wn", "foo");
                        }
                        invocations.add(() -> resultHandler
                                .accept(JacksonUtils.createObject(params,
                                        JacksonUtils::createNode)));
                    }
                };
            }
        };
        final AtomicInteger callbackInvocations = new AtomicInteger();
        final Page.ExtendedClientDetailsReceiver receiver = details -> {
            callbackInvocations.incrementAndGet();
        };

        // when
        page.retrieveExtendedClientDetails(receiver);
        page.retrieveExtendedClientDetails(receiver);

        // then : before cached value is set the second retrieve is requested
        invocations.forEach(Runnable::run);

        Assert.assertEquals(2, callbackInvocations.get());

        Assert.assertEquals("ROOT-1234567-0.1234567", mockUI.getInternals()
                .getExtendedClientDetails().getWindowName());
    }

    @Test
    public void retrieveExtendedClientDetails_twice_jsOnceAndCallbackTwice() {
        // given
        final UI mockUI = new MockUI();
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
        Assert.assertEquals(1, jsInvocations);
        Assert.assertEquals(2, callbackInvocations.get());
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
                Assert.assertEquals(
                        "Expected javascript for fetching location is wrong.",
                        "return window.location.href", expression);

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
        Assert.assertEquals("Returned URL was wrong",
                "http://localhost:8080/home",
                callbackInvocations.get().toString());
    }

    @Test
    public void fetchCurrentUrl_passNullCallback_throwsNullPointerException() {
        Assert.assertThrows(NullPointerException.class, () -> {
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

        Assert.assertEquals("There should be 4 dependencies added.", 4,
                pendingSendToClient.size());

        for (Dependency dependency : pendingSendToClient) {
            Assert.assertEquals("Dependency should be a JSModule",
                    Dependency.Type.JS_MODULE, dependency.getType());
            Assert.assertEquals("JS module dependency should be EAGER",
                    LoadMode.EAGER, dependency.getLoadMode());

            Assert.assertTrue(
                    "Dependency " + dependency.getUrl()
                            + " is not found in the source list.",
                    urls.contains(dependency.getUrl()));

            urls.remove(dependency.getUrl());
        }

        Assert.assertEquals("Not all urls were added as dependencies", 0,
                urls.size());
    }

    @Test
    public void addJsModule_rejects_files() {
        try {
            page.addJsModule("mod.js");

            Assert.fail(
                    "Adding a file without starting \"/\" is not to be allowed.");
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
                Assert.assertNull("There should be no old expression",
                        oldExpression);

                Object[] oldParams = invokedParams.getAndSet(parameters);
                Assert.assertNull("There should be no old params", oldParams);

                return null;
            }
        };

        PendingJavaScriptResult executionCanceler = page.executeJs("foo", 1,
                true);

        Assert.assertNull(executionCanceler);

        Assert.assertEquals("foo", invokedExpression.get());
        Assert.assertEquals(Integer.valueOf(1), invokedParams.get()[0]);
        Assert.assertEquals(Boolean.TRUE, invokedParams.get()[1]);
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
        Assert.assertEquals("_self", params.get(1));

        MatcherAssert.assertThat(capture.get(), CoreMatchers
                .startsWith("if ($1 == '_self') this.stopApplication();"));
    }
}
