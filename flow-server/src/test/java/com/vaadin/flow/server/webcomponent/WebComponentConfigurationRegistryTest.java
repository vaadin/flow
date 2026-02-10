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
package com.vaadin.flow.server.webcomponent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
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

import net.jcip.annotations.NotThreadSafe;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory.DefaultWebComponentExporterFactory;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;

@NotThreadSafe
class WebComponentConfigurationRegistryTest {

    private static final String MY_COMPONENT_TAG = "my-component";
    private static final String USER_BOX_TAG = "user-box";

    protected WebComponentConfigurationRegistry registry;

    protected VaadinContext context;

    protected WebComponentConfigurationRegistry createRegistry() {
        return new WebComponentConfigurationRegistry() {
        };
    }

    @BeforeEach
    public void init() {
        VaadinService service = mock(VaadinService.class);
        context = mock(VaadinContext.class);
        VaadinService.setCurrent(service);
        Mockito.when(service.getContext()).thenReturn(context);
        WebComponentConfigurationRegistry instance = createRegistry();
        Mockito.when(
                context.getAttribute(WebComponentConfigurationRegistry.class))
                .thenReturn(instance);
        Mockito.when(context.getAttribute(
                eq(WebComponentConfigurationRegistry.class), any()))
                .thenReturn(instance);
        registry = WebComponentConfigurationRegistry.getInstance(context);
    }

    @Test
    public void assertWebComponentRegistry() {
        Assertions.assertNotNull(registry);
    }

    @Test
    public void assertRegistryIsSingleton() {
        Assertions.assertSame(registry,
                WebComponentConfigurationRegistry.getInstance(context),
                "WebComponentConfigurationRegistry instance should be singleton");
    }

    @Test
    public void setConfigurations_allCanBeFoundInRegistry() {
        Assertions.assertTrue(
                registry.setConfigurations(createConfigurations(
                        MyComponentExporter.class, UserBoxExporter.class)),
                "Registry should have accepted the webComponents");

        Assertions.assertEquals(2, registry.getConfigurations().size(),
                "Expected two targets to be registered");

        Assertions.assertEquals(MyComponent.class,
                registry.getConfiguration(MY_COMPONENT_TAG).get()
                        .getComponentClass(),
                "Tag 'my-component' should have returned "
                        + "'WebComponentBuilder' matching MyComponent");
        Assertions.assertEquals(UserBox.class,
                registry.getConfiguration(USER_BOX_TAG).get()
                        .getComponentClass(),
                "Tag 'user-box' should have returned 'WebComponentBuilder' "
                        + "matching UserBox");
    }

    @Test
    public void setConfigurations_getConfigurationsCallDoesNotChangeSetProtection() {
        registry.setConfigurations(
                createConfigurations(MyComponentExporter.class));

        WebComponentConfiguration<? extends Component> conf1 = registry
                .getConfiguration("my-component").get();

        Assertions.assertNotNull(conf1);

        Assertions.assertFalse(registry.setConfigurations(
                createConfigurations(UserBoxExporter.class)));

        WebComponentConfiguration<? extends Component> conf2 = registry
                .getConfiguration("my-component").get();

        Assertions.assertEquals(conf1, conf2);
    }

    @Test
    public void getWebComponentConfigurationsForComponent() {
        registry.setConfigurations(
                createConfigurations(MyComponentExporter.class,
                        MyComponentExporter2.class, UserBoxExporter.class));

        Set<WebComponentConfiguration<MyComponent>> set = registry
                .getConfigurationsByComponentType(MyComponent.class);

        Assertions.assertEquals(2, set.size(),
                "Set should contain two configurations");

        Assertions.assertTrue(
                set.stream().map(WebComponentConfiguration::getComponentClass)
                        .allMatch(clazz -> clazz.equals(MyComponent.class)),
                "Both configurations should have component class "
                        + "MyComponent.class");
    }

    @Test
    public void setConfigurationsTwice_onlyFirstSetIsAccepted() {
        Set<WebComponentConfiguration<? extends Component>> configs1st = createConfigurations(
                MyComponentExporter.class);

        Set<WebComponentConfiguration<? extends Component>> configs2nd = createConfigurations(
                UserBoxExporter.class);

        Assertions.assertTrue(registry.setConfigurations(configs1st),
                "Registry should have accepted the configurations");

        Assertions.assertFalse(registry.setConfigurations(configs2nd),
                "Registry should not accept a second set of configurations.");

        Assertions.assertEquals(MyComponent.class,
                registry.getConfiguration("my" + "-component").get()
                        .getComponentClass(),
                "Builders from the first Set should have been added");

        Assertions.assertFalse(
                registry.getConfiguration("user-box").isPresent(),
                "Components from the second Set should not have been added");
    }

    @Test
    public void getConfigurations_uninitializedReturnsEmptySet() {
        WebComponentConfigurationRegistry uninitializedRegistry = new WebComponentConfigurationRegistry();

        Set<?> set = uninitializedRegistry.getConfigurations();

        Assertions.assertEquals(0, set.size(),
                "Configuration set should be empty");
    }

    @Test
    public void hasConfigurations() {
        registry.setConfigurations(
                createConfigurations(MyComponentExporter.class,
                        MyComponentExporter2.class, UserBoxExporter.class));

        Assertions.assertTrue(registry.hasConfigurations(),
                "Should have configurations, when 3 were set");
    }

    @Test
    public void hasConfigurations_noConfigurations() {
        Assertions.assertFalse(registry.hasConfigurations(),
                "New registry should have no configurations");

        registry.setConfigurations(Collections.emptySet());

        Assertions.assertFalse(registry.hasConfigurations(),
                "Should not have configurations, when empty set is" + " given");
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
                        return new AtomicBoolean(
                                registry.setConfigurations(createConfigurations(
                                        MyComponentExporter.class)));
                    };
                    return callable;
                }).collect(Collectors.toList());

        List<Future<AtomicBoolean>> futures = executorService
                .invokeAll(callables);

        executorService.shutdown();

        Assertions.assertEquals(THREADS, futures.size(),
                "Expected a result for all threads");

        List<AtomicBoolean> results = new ArrayList<>();
        for (Future<AtomicBoolean> resultFuture : futures) {
            results.add(resultFuture.get());
        }

        Assertions.assertEquals(THREADS - 1,
                results.stream().filter(result -> !result.get()).count(),
                "Expected all except one thread to return false");

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Set<WebComponentConfiguration<? extends Component>> createConfigurations(
            Class<? extends WebComponentExporter<? extends Component>>... exporters) {
        WebComponentExporter.WebComponentConfigurationFactory factory = new WebComponentExporter.WebComponentConfigurationFactory();

        Set<WebComponentConfiguration<? extends Component>> configurations = new HashSet<>();
        for (Class<? extends WebComponentExporter<? extends Component>> exporter : exporters)
            configurations.add(factory.create(
                    new DefaultWebComponentExporterFactory(exporter).create()));
        return configurations;
    }

    protected class MyComponent extends Component {
    }

    protected class UserBox extends Component {
    }

    protected static class MyComponentExporter
            extends WebComponentExporter<MyComponent> {
        public MyComponentExporter() {
            super("my-component");
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {

        }
    }

    protected static class MyComponentExporter2
            extends WebComponentExporter<MyComponent> {

        public MyComponentExporter2() {
            super("my-component-2");
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent,
                MyComponent component) {

        }
    }

    protected static class UserBoxExporter
            extends WebComponentExporter<UserBox> {

        public UserBoxExporter() {
            super("user-box");
        }

        @Override
        public void configureInstance(WebComponent<UserBox> webComponent,
                UserBox component) {

        }
    }
}
