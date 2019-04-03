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

package com.vaadin.flow.server.webcomponent;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.internal.CurrentInstance;
import com.vaadin.flow.server.MockInstantiator;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;

import static org.mockito.Mockito.mock;

@NotThreadSafe
public class WebComponentConfigurationRegistryTest {

    private static final String MY_COMPONENT_TAG = "my-component";
    private static final String USER_BOX_TAG = "user-box";

    @Mock
    protected VaadinService service;

    @Mock
    protected VaadinSession session;

    protected WebComponentConfigurationRegistry registry;

    @Before
    public void init() {
        registry = WebComponentConfigurationRegistry
                .getInstance(mock(ServletContext.class));

        MockitoAnnotations.initMocks(this);
        VaadinService.setCurrent(service);
        Mockito.when(service.getInstantiator())
                .thenReturn(new MockInstantiator());
    }

    @After
    public void cleanUp() {
        CurrentInstance.clearAll();
    }

    @Test
    public void assertWebComponentRegistry() {
        Assert.assertEquals(WebComponentConfigurationRegistry.class,
                registry.getClass());
    }

    @Test
    public void setExporters_allCanBeFoundInRegistry() {
        Assert.assertTrue("Registry should have accepted the webComponents",
                registry.setExporters(asSet(MyComponentExporter.class,
                        UserBoxExporter.class)));

        Assert.assertEquals("Expected two targets to be registered", 2,
                registry.getConfigurations().size());

        Assert.assertEquals(
                "Tag 'my-component' should have returned "
                        + "'WebComponentBuilder' matching MyComponent",
                MyComponent.class,
                registry.getConfiguration(MY_COMPONENT_TAG).get()
                        .getComponentClass());
        Assert.assertEquals(
                "Tag 'user-box' should have returned 'WebComponentBuilder' "
                        + "matching UserBox",
                UserBox.class, registry.getConfiguration(USER_BOX_TAG).get()
                        .getComponentClass());
    }

    @Test
    public void setExporters_gettingBuildersDoesNotAllowAddingMore() {
        registry.setExporters(asSet(MyComponentExporter.class));

        WebComponentConfiguration<? extends Component> conf1 = registry
                .getConfiguration("my-component").get();

        Assert.assertNotNull(conf1);

        Assert.assertFalse(registry.setExporters(asSet(UserBoxExporter.class)));

        WebComponentConfiguration<? extends Component> conf2 = registry
                .getConfiguration("my-component").get();

        Assert.assertEquals(conf1, conf2);
    }

    @Test
    public void getWebComponentConfigurationsForComponent() {
        registry.setExporters(asSet(MyComponentExporter.class,
                MyComponentExporter2.class, UserBoxExporter.class));

        Set<WebComponentConfiguration<MyComponent>> set = registry
                .getConfigurationsByComponentType(MyComponent.class);

        Assert.assertEquals("Builder set should contain two builders", 2,
                set.size());

        Assert.assertTrue(
                "Both builders should have exporter " + "MyComponent.class",
                set.stream().map(WebComponentConfiguration::getComponentClass)
                        .allMatch(clazz -> clazz.equals(MyComponent.class)));
    }

    @Test
    public void setConfigurationsTwice_onlyFirstSetIsAccepted() {
        Set<Class<? extends WebComponentExporter<? extends Component>>> exporters1st = asSet(
                MyComponentExporter.class);

        Set<Class<? extends WebComponentExporter<? extends Component>>> exporters2nd = asSet(
                UserBoxExporter.class);

        Assert.assertTrue("Registry should have accepted the WebComponents",
                registry.setExporters(exporters1st));

        Assert.assertFalse(
                "Registry should not accept a second set of WebComponents.",
                registry.setExporters(exporters2nd));

        Assert.assertEquals(
                "Builders from the first Set should have been added",
                MyComponent.class,
                registry.getConfiguration("my" + "-component").get()
                        .getComponentClass());

        Assert.assertFalse(
                "Components from the second Set should not have been added",
                registry.getConfiguration("user-box").isPresent());
    }

    @Test
    public void getConfigurations_uninitializedReturnsEmptySet() {
        WebComponentConfigurationRegistry uninitializedRegistry = new WebComponentConfigurationRegistry();

        Set<?> set = uninitializedRegistry.getConfigurations();

        Assert.assertEquals("Configuration set should be empty", 0, set.size());
    }

    @Test
    public void hasExporters() {
        Assert.assertFalse("Should have no exporters", registry.hasConfigurations());

        registry.setExporters(Collections.emptySet());

        Assert.assertTrue("Should have exporters, albeit empty",
                registry.hasConfigurations());
    }

    @Test
    public void setSameRouteValueFromDifferentThreads_ConcurrencyTest()
            throws InterruptedException, ExecutionException {
        final int THREADS = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(THREADS);

        List<Callable<AtomicBoolean>> callables = IntStream.range(0, THREADS)
                .mapToObj(i -> {
                    Callable<AtomicBoolean> callable = () -> {
                        // Add random sleep for better possibility to run at
                        // same time
                        Thread.sleep(new Random().nextInt(200));
                        return new AtomicBoolean(registry.setExporters(
                                asSet(MyComponentExporter.class)));
                    };
                    return callable;
                }).collect(Collectors.toList());

        List<Future<AtomicBoolean>> futures = executorService
                .invokeAll(callables);

        executorService.shutdown();

        Assert.assertEquals("Expected a result for all threads", THREADS,
                futures.size());

        List<AtomicBoolean> results = new ArrayList<>();
        for (Future<AtomicBoolean> resultFuture : futures) {
            results.add(resultFuture.get());
        }

        Assert.assertEquals("Expected all except one thread to return false",
                THREADS - 1,
                results.stream().filter(result -> !result.get()).count());

    }
    
    protected Set<Class<? extends WebComponentExporter<? extends Component>>> asSet(
            Class<?>... things) {
        return Stream.of(things)
                .map(thing -> (Class<? extends WebComponentExporter<?
                        extends Component>>) thing)
                .collect(Collectors.toSet());
    }

    protected class MyComponent extends Component {
    }

    protected class UserBox extends Component {
    }

    /*
     * These exporters have to be public, or Instantiator won't find their
     * constructors
     */

    public static class MyComponentExporter
            extends WebComponentExporter<MyComponent> {
        public MyComponentExporter() {
            super("my-component");
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent, MyComponent component) {

        }
    }

    public static class MyComponentExporter2
            extends WebComponentExporter<MyComponent> {
        
        public MyComponentExporter2() {
            super("my-component-2");
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent, MyComponent component) {

        }
    }

    public static class UserBoxExporter
            extends WebComponentExporter<UserBox> {

        public UserBoxExporter() {
            super("user-box");
        }

        @Override
        public void configureInstance(WebComponent<UserBox> webComponent, UserBox component) {

        }
    }
}
