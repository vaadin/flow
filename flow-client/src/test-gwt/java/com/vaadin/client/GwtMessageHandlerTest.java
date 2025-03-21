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
package com.vaadin.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Timer;
import com.vaadin.client.communication.MessageHandler;
import com.vaadin.client.communication.MessageSender;
import com.vaadin.client.communication.RequestResponseTracker;
import com.vaadin.client.flow.ExecuteJavaScriptProcessor;
import com.vaadin.client.flow.StateTree;
import com.vaadin.flow.shared.JsonConstants;
import com.vaadin.flow.shared.ui.Dependency;
import com.vaadin.flow.shared.ui.LoadMode;

import elemental.json.Json;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

/**
 * This class is used to test {@link MessageHandler} GWT functionality, that is
 * required to process all dependencies earlier any other message processing.
 */
public class GwtMessageHandlerTest extends ClientEngineTestBase {

    private Registry registry;
    private TestMessageHandler handler;

    private static class EventsOrder {

        private List<String> sources = new ArrayList<>();
    }

    private static class TestMessageHandler extends MessageHandler {

        public TestMessageHandler(Registry registry) {
            super(registry);
        }

        @Override
        protected void handleJSON(ValueMap valueMap) {
            super.handleJSON(valueMap);
        }

    }

    private static class TestRequestResponseTracker
            extends RequestResponseTracker {
        public TestRequestResponseTracker(Registry registry) {
            super(registry);
        }

        @Override
        public void endRequest() {
        }
    }

    private static class TestUriResolver extends URIResolver {
        public TestUriResolver(Registry registry) {
            super(registry);
        }

        @Override
        public String resolveVaadinUri(String uri) {
            return uri;
        }
    }

    private static class TestResourceLoader extends ResourceLoader {

        private Set<String> scriptUrls = new HashSet<>();

        private Registry registry;

        public TestResourceLoader(Registry registry) {
            super(registry, false);
            this.registry = registry;
        }

        @Override
        public void loadJsModule(String scriptUrl,
                ResourceLoadListener resourceLoadListener, boolean async,
                boolean defer) {
            scriptUrls.add(scriptUrl);
            resourceLoadListener.onLoad(new ResourceLoadEvent(this, scriptUrl));
            registry.get(EventsOrder.class).sources
                    .add(ResourceLoader.class.getName());
            addInternalEvent(ResourceLoader.class.getName());
        }

    }

    private static class TestStateTree extends StateTree {

        public TestStateTree(Registry registry) {
            super(registry);
        }

        @Override
        public void setUpdateInProgress(boolean updateInProgress) {
            getRegistry().get(EventsOrder.class).sources
                    .add(StateTree.class.getName());
            addInternalEvent(StateTree.class.getName());
        }

    }

    private static class TestMessageSender extends MessageSender {

        public TestMessageSender(Registry registry) {
            super(registry);
        }

        @Override
        public void resynchronize() {
            setResyncState(true);
        }
    }

    private static class TestExecuteJavaScriptProcessor
            extends ExecuteJavaScriptProcessor {

        public TestExecuteJavaScriptProcessor(Registry registry) {
            super(registry);
        }
    }

    private static class TestSystemErrorHandler extends SystemErrorHandler {

        private boolean sessionExpiredMessageHandled = false;
        private boolean unrecoverableErrorHandled = false;

        public TestSystemErrorHandler(Registry registry) {
            super(registry);
        }

        @Override
        public void handleSessionExpiredError(String details) {
            sessionExpiredMessageHandled = true;
        }

        @Override
        public void handleUnrecoverableError(String caption, String message,
                String details, String url, String querySelector) {
            unrecoverableErrorHandled = true;
        }
    }

    private static class TestApplicationConfiguration
            extends ApplicationConfiguration {
        @Override
        public String getApplicationId() {
            return "test-application-id";
        }
    }

    private static class TestUILifecycle extends UILifecycle {
    }

    @Override
    protected void gwtSetUp() throws Exception {
        super.gwtSetUp();
        registry = new Registry() {
            {
                set(RequestResponseTracker.class,
                        new TestRequestResponseTracker(this));
                set(DependencyLoader.class, new DependencyLoader(this));
                set(ResourceLoader.class, new TestResourceLoader(this));
                set(URIResolver.class, new TestUriResolver(this));
                set(StateTree.class, new TestStateTree(this));
                set(ApplicationConfiguration.class,
                        new TestApplicationConfiguration());
                set(EventsOrder.class, new EventsOrder());
                set(MessageSender.class, new TestMessageSender(this));
                set(SystemErrorHandler.class, new TestSystemErrorHandler(this));
                set(ExecuteJavaScriptProcessor.class,
                        new TestExecuteJavaScriptProcessor(this));
                set(UILifecycle.class, new TestUILifecycle());
            }
        };
        handler = new TestMessageHandler(registry);
    }

    public void testMessageProcessing_moduleDependencyIsHandledBeforeApplyingChangesToTree() {
        resetInternalEvents();

        JavaScriptObject object = JavaScriptObject.createObject();
        JsonObject obj = object.cast();

        // make an empty changes list. It will initiate changes processing
        // anyway
        // Any changes processing should happen AFTER dependencies are loaded
        obj.put("changes", Json.createArray());

        JsonArray array = Json.createArray();

        // create a dependency
        JsonObject dep = Json.createObject();
        dep.put(Dependency.KEY_URL, "foo");
        dep.put(Dependency.KEY_TYPE, Dependency.Type.JS_MODULE.toString());
        array.set(0, dep);

        obj.put(LoadMode.EAGER.toString(), array);
        handler.handleJSON(object.cast());

        doAssert(() -> {
            assertTrue(getResourceLoader().scriptUrls.contains("foo"));

            EventsOrder eventsOrder = registry.get(EventsOrder.class);
            assertTrue(eventsOrder.sources.size() >= 2);
            // the first one is resource loaded which means dependency
            // processing
            assertEquals(ResourceLoader.class.getName(),
                    eventsOrder.sources.get(0));
            // the second one is applying changes to StatTree
            assertEquals(StateTree.class.getName(), eventsOrder.sources.get(1));
        });
    }

    public void testMessageProcessing_dynamicDependencyIsHandledBeforeApplyingChangesToTree() {
        resetInternalEvents();

        JavaScriptObject object = JavaScriptObject.createObject();
        JsonObject obj = object.cast();

        // make an empty changes list. It will initiate changes processing
        // anyway
        // Any changes processing should happen AFTER dependencies are loaded
        obj.put("changes", Json.createArray());

        JsonArray array = Json.createArray();

        // create a dependency
        JsonObject dep = Json.createObject();
        dep.put(Dependency.KEY_TYPE, Dependency.Type.DYNAMIC_IMPORT.toString());
        dep.put(Dependency.KEY_URL, "return new Promise(function(resolve){ "
                + "window.testEvents.push('test-dependency'); resolve(); });");
        array.set(0, dep);

        obj.put(LoadMode.LAZY.toString(), array);

        handler.handleJSON(object.cast());

        doAssert(() -> {
            assertEquals("test-dependency", getInternalEvent(0));
            assertEquals(StateTree.class.getName(), getInternalEvent(1));
        });
    }

    public void testForceHandleMessage_resyncIsRequested() {
        resetInternalEvents();
        setResyncState(false);

        // given a max message suspension time of 200 ms
        registry.getApplicationConfiguration().setMaxMessageSuspendTimeout(200);

        // when two out-of-order messages happen
        JavaScriptObject object1 = JavaScriptObject.createObject();
        JsonObject obj1 = object1.cast();
        obj1.put("syncId", 1);
        handler.handleJSON(object1.cast());

        JavaScriptObject object2 = JavaScriptObject.createObject();
        JsonObject obj2 = object2.cast();
        obj2.put("syncId", 3);
        handler.handleJSON(object2.cast());

        // then a re-sync message is sent within 300 ms
        doAssert(() -> assertTrue(getResyncState()), 300);
    }

    public void testHandleJSON_uiTerminated_sessionExpiredMessageNotShown() {
        resetInternalEvents();

        // when: payload message from server contains session expired flag
        JavaScriptObject messagePayloadJS = JavaScriptObject.createObject();
        JsonObject messagePayload = messagePayloadJS.cast();

        JsonObject meta = Json.createObject();
        meta.put(JsonConstants.META_SESSION_EXPIRED, true);
        messagePayload.put("meta", meta);

        // when: UI has been terminated (for instance, as a result of redirect
        // JS caused by Page::setLocation)
        getUILifecycle().setState(UILifecycle.UIState.RUNNING);
        getUILifecycle().setState(UILifecycle.UIState.TERMINATED);

        // when: payload message is handled by message handler on client side
        handler.handleJSON(messagePayloadJS.cast());

        doAssert(() -> {
            // then: no session expire and unrecoverable error handling expected
            assertFalse(
                    "Session Expired Message handling is not expected "
                            + "when the page is being redirected",
                    getSystemErrorHandler().sessionExpiredMessageHandled);
            assertFalse(
                    "Unrecoverable Error Message handling was not "
                            + "expected when the page is being redirected",
                    getSystemErrorHandler().unrecoverableErrorHandled);
            assertEquals(UILifecycle.UIState.TERMINATED,
                    getUILifecycle().getState());
        });
    }

    public void testHandleJSON_uiTerminated_unrecoverableErrorMessageNotShown() {
        resetInternalEvents();

        // when: payload message from server contains error message flag
        JavaScriptObject messagePayloadJS = JavaScriptObject.createObject();
        JsonObject messagePayload = messagePayloadJS.cast();

        JsonObject meta = Json.createObject();
        meta.put("appError", true);
        messagePayload.put("meta", meta);

        // when: UI has been terminated (for instance, as a result of redirect
        // JS caused by Page::setLocation)
        getUILifecycle().setState(UILifecycle.UIState.RUNNING);
        getUILifecycle().setState(UILifecycle.UIState.TERMINATED);

        // when: payload message is handled by message handler on client side
        handler.handleJSON(messagePayloadJS.cast());

        doAssert(() -> {
            // then: no session expire and unrecoverable error handling expected
            assertFalse(
                    "Session Expired Message handling is not expected "
                            + "when the page is being redirected",
                    getSystemErrorHandler().sessionExpiredMessageHandled);
            assertFalse(
                    "Unrecoverable Error Message handling was not "
                            + "expected when the page is being redirected",
                    getSystemErrorHandler().unrecoverableErrorHandled);
            assertEquals(UILifecycle.UIState.TERMINATED,
                    getUILifecycle().getState());
        });
    }

    public void testHandleJSON_sessionExpiredAndUIRunning_sessionExpiredMessageShown() {
        resetInternalEvents();

        // when: UI is running
        getUILifecycle().setState(UILifecycle.UIState.RUNNING);

        // when: payload message from server contains session expired flag
        JavaScriptObject messagePayloadJS = JavaScriptObject.createObject();
        JsonObject messagePayload = messagePayloadJS.cast();

        JsonObject meta = Json.createObject();
        meta.put(JsonConstants.META_SESSION_EXPIRED, true);
        messagePayload.put("meta", meta);

        // when: payload message is handled by message handler on client side
        handler.handleJSON(messagePayloadJS.cast());

        doAssert(() -> {
            // then: session expire handling expected
            assertTrue("Session Expire handling expected",
                    getSystemErrorHandler().sessionExpiredMessageHandled);
            assertFalse("No Error message handling expected",
                    getSystemErrorHandler().unrecoverableErrorHandled);
            assertEquals(UILifecycle.UIState.TERMINATED,
                    getUILifecycle().getState());
        }, 300);
    }

    public void testHandleJSON_unrecoverableErrorAndUIRunning_unrecoverableErrorMessageShown() {
        resetInternalEvents();

        // when: UI is running
        getUILifecycle().setState(UILifecycle.UIState.RUNNING);

        // when: payload message from server contains error message flag
        JavaScriptObject messagePayloadJS = JavaScriptObject.createObject();
        JsonObject messagePayload = messagePayloadJS.cast();

        JsonObject meta = Json.createObject();
        meta.put("appError", true);
        messagePayload.put("meta", meta);

        // when: payload message is handled by message handler on client side
        handler.handleJSON(messagePayloadJS.cast());

        doAssert(() -> {
            // then: unrecoverable error handling expected
            assertFalse("No Session Expire handling expected",
                    getSystemErrorHandler().sessionExpiredMessageHandled);
            assertTrue("Error Message handling expected",
                    getSystemErrorHandler().unrecoverableErrorHandled);
            assertEquals(UILifecycle.UIState.TERMINATED,
                    getUILifecycle().getState());
        });
    }

    private TestResourceLoader getResourceLoader() {
        return (TestResourceLoader) registry.getResourceLoader();
    }

    private TestSystemErrorHandler getSystemErrorHandler() {
        return (TestSystemErrorHandler) registry.getSystemErrorHandler();
    }

    private TestUILifecycle getUILifecycle() {
        return (TestUILifecycle) registry.getUILifecycle();
    }

    private void doAssert(Runnable assertions) {
        doAssert(assertions, 100);
    }

    private void doAssert(Runnable assertions, int assertDelayInMillis) {
        delayTestFinish(500);
        new Timer() {
            @Override
            public void run() {
                assertions.run();
                finishTest();
            }
        }.schedule(assertDelayInMillis);
    }

    private static native void resetInternalEvents()
    /*-{
         window.testEvents = [];
    }-*/;

    private static native void addInternalEvent(String eventKey)
    /*-{
         window.testEvents.push(eventKey);
    }-*/;

    private static native String getInternalEvent(int index)
    /*-{
         return window.testEvents[index];
    }-*/;

    private static native void setResyncState(boolean resync)
    /*-{
        window.resynchronizing = resync;
    }-*/;

    private static native boolean getResyncState()
    /*-{
        return window.resynchronizing;
    }-*/;

}
