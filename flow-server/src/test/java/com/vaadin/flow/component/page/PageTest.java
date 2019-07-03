/*
 * Copyright 2000-2018 Vaadin Ltd.
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
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.function.SerializableConsumer;
import com.vaadin.flow.internal.JsonUtils;
import com.vaadin.flow.shared.Registration;
import com.vaadin.tests.util.MockUI;

import elemental.json.Json;
import elemental.json.JsonValue;

public class PageTest {

    private class TestUI extends UI {
        @Override
        public Page getPage() {
            return page;
        }
    }

    private class TestPage extends Page {

        public TestPage() {
            super(new TestUI());
        }

        private int count = 0;

        private String expression;

        private Serializable firstParam;

        @Override
        public PendingJavaScriptResult executeJs(String expression,
                Serializable... parameters) {
            this.expression = expression;
            firstParam = parameters[0];
            count++;
            return null;
        }
    }

    private TestPage page = new TestPage();

    private BrowserWindowResizeListener listener = event -> {
    };

    @Test(expected = NullPointerException.class)

    public void addNullAsAListener_trows() {
        page.addBrowserWindowResizeListener(null);
    }

    @Test
    public void addListener_executeInitJs() {
        page.addBrowserWindowResizeListener(listener);

        Assert.assertThat(page.expression,
                CoreMatchers.allOf(CoreMatchers.containsString("init"),
                        CoreMatchers.containsString("resize")));

        Assert.assertTrue(page.firstParam instanceof Component);
    }

    @Test
    public void addTwoListeners_jsIsExecutedOnce() {
        page.addBrowserWindowResizeListener(listener);
        page.addBrowserWindowResizeListener(event -> {
        });

        Assert.assertEquals(1, page.count);
    }

    @Test
    public void addTwoListeners_unregisterOneListener_jsListenerIsNotRemoved() {
        page.addBrowserWindowResizeListener(listener);
        Registration registration = page
                .addBrowserWindowResizeListener(event -> {
                });

        registration.remove();

        Assert.assertEquals(1, page.count);

        // remove the same listener one more time
        registration.remove();

        Assert.assertEquals(1, page.count);
    }

    @Test
    public void addTwoListeners_unregisterTwoListeners_jsListenerIsRemoved() {
        Registration registration1 = page
                .addBrowserWindowResizeListener(listener);
        Registration registration2 = page
                .addBrowserWindowResizeListener(event -> {
                });

        registration1.remove();
        registration2.remove();

        Assert.assertEquals(2, page.count);

        Assert.assertEquals("$0.resizeRemove()", page.expression);

        Assert.assertTrue(page.firstParam instanceof Component);
    }

    @Test
    public void addListener_unregisterListener_addListener_jsListenerIsRemovedAndInitialized() {
        Registration registration = page
                .addBrowserWindowResizeListener(listener);

        registration.remove();
        // remove several times
        registration.remove();

        Assert.assertEquals(2, page.count);

        Assert.assertEquals("$0.resizeRemove()", page.expression);

        page.addBrowserWindowResizeListener(listener);

        Assert.assertEquals(3, page.count);

        Assert.assertThat(page.expression,
                CoreMatchers.allOf(CoreMatchers.containsString("init"),
                        CoreMatchers.containsString("resize")));

        Assert.assertTrue(page.firstParam instanceof Component);
    }

    @Test
    public void retrieveExtendedClientDetails_twice_jsOnceAndCallbackTwice() {
        // given
        final UI mockUI = new MockUI();
        final Page page = new Page(mockUI) {
            @Override
            public PendingJavaScriptResult executeJs(String expression,
                                                     Serializable... params) {
                super.executeJs(expression,params);

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
                    public void then(SerializableConsumer<JsonValue> resultHandler,
                                     SerializableConsumer<String> errorHandler) {
                        final HashMap<String,String> params = new HashMap<>();
                        params.put("v-sw","2560");
                        params.put("v-sh","1450");
                        params.put("v-tzo","-270");
                        params.put("v-rtzo","-210");
                        params.put("v-dstd","60");
                        params.put("v-dston","true");
                        params.put("v-tzid","Asia/Tehran");
                        params.put("v-curdate","1555000000000");
                        params.put("v-td","false");
                        params.put("v-wn","ROOT-1234567-0.1234567");
                        resultHandler.accept(JsonUtils.createObject(
                                params, Json::create));
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
        final int jsInvocations =
                mockUI.getInternals().dumpPendingJavaScriptInvocations().size();
        Assert.assertEquals(1, jsInvocations);
        Assert.assertEquals(2, callbackInvocations.get());
    }
}
