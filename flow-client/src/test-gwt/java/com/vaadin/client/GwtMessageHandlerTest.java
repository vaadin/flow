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
package com.vaadin.client;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.Timer;

import com.vaadin.client.communication.MessageHandler;
import com.vaadin.client.communication.RequestResponseTracker;
import com.vaadin.client.flow.StateTree;
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
                        new ApplicationConfiguration());
                set(EventsOrder.class, new EventsOrder());
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

        delayTestFinish(500);

        new Timer() {
            @Override
            public void run() {
                assertTrue(getResourceLoader().scriptUrls.contains("foo"));

                EventsOrder eventsOrder = registry.get(EventsOrder.class);
                assertTrue(eventsOrder.sources.size() >= 2);
                // the first one is resource loaded which means dependency
                // processing
                assertEquals(ResourceLoader.class.getName(),
                        eventsOrder.sources.get(0));
                // the second one is applying changes to StatTree
                assertEquals(StateTree.class.getName(),
                        eventsOrder.sources.get(1));
                finishTest();
            }
        }.schedule(100);
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

        delayTestFinish(500);

        new Timer() {
            @Override
            public void run() {
                assertEquals("test-dependency", getInternalEvent(0));
                assertEquals(StateTree.class.getName(), getInternalEvent(1));
                finishTest();
            }
        }.schedule(100);
    }

    private TestResourceLoader getResourceLoader() {
        return (TestResourceLoader) registry.getResourceLoader();
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

}
