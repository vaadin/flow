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

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.component.webcomponent.WebComponentConfiguration;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import net.jcip.annotations.NotThreadSafe;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;
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
import java.util.stream.Stream;

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;

@NotThreadSafe
public class WebComponentConfigurationRegistryTest {

    private static final String MY_COMPONENT_TAG = "my-component";
    private static final String USER_BOX_TAG = "user-box";

    protected WebComponentConfigurationRegistry registry;

    protected VaadinContext context;

    protected WebComponentConfigurationRegistry createRegistry() {
        return new WebComponentConfigurationRegistry(){};
    }

    @Before
    public void init() {
        VaadinService service = mock(VaadinService.class);
        context = mock(VaadinContext.class);
        VaadinService.setCurrent(service);
        Mockito.when(service.getContext()).thenReturn(context);
        WebComponentConfigurationRegistry instance = createRegistry();
        Mockito.when(context.getAttribute(WebComponentConfigurationRegistry.class)).thenReturn(instance);
        Mockito.when(context.getAttribute(eq(WebComponentConfigurationRegistry.class), anyObject())).thenReturn(instance);
        registry = WebComponentConfigurationRegistry.getInstance(context);
    }

    @Test
    public void assertWebComponentRegistry() {
        Assert.assertNotNull(registry);
    }

    @Test
    public void assertRegistryIsSingleton() {
        Assert.assertSame("WebComponentConfigurationRegistry instance should be singleton",
            registry, WebComponentConfigurationRegistry.getInstance(context));
    }

    @Test
    public void setConfigurations_allCanBeFoundInRegistry() {
        Assert.assertTrue("Registry should have accepted the webComponents",
                registry.setConfigurations(createConfigurations(MyComponentExporter.class,
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
    public void setConfigurations_getConfigurationsCallDoesNotChangeSetProtection() {
        registry.setConfigurations(createConfigurations(MyComponentExporter.class));

        WebComponentConfiguration<? extends Component> conf1 = registry
                .getConfiguration("my-component").get();

        Assert.assertNotNull(conf1);

        Assert.assertFalse(registry.setConfigurations(createConfigurations(UserBoxExporter.class)));

        WebComponentConfiguration<? extends Component> conf2 = registry
                .getConfiguration("my-component").get();

        Assert.assertEquals(conf1, conf2);
    }

    @Test
    public void getWebComponentConfigurationsForComponent() {
        registry.setConfigurations(createConfigurations(MyComponentExporter.class,
                MyComponentExporter2.class, UserBoxExporter.class));

        Set<WebComponentConfiguration<MyComponent>> set = registry
                .getConfigurationsByComponentType(MyComponent.class);

        Assert.assertEquals("Set should contain two configurations", 2,
                set.size());

        Assert.assertTrue(
                "Both configurations should have component class " +
                        "MyComponent.class",
                set.stream().map(WebComponentConfiguration::getComponentClass)
                        .allMatch(clazz -> clazz.equals(MyComponent.class)));
    }

    @Test
    public void setConfigurationsTwice_onlyFirstSetIsAccepted() {
        Set<WebComponentConfiguration<? extends Component>> configs1st =
                createConfigurations(MyComponentExporter.class);

        Set<WebComponentConfiguration<? extends Component>> configs2nd =
                createConfigurations(UserBoxExporter.class);

        Assert.assertTrue("Registry should have accepted the configurations",
                registry.setConfigurations(configs1st));

        Assert.assertFalse(
                "Registry should not accept a second set of configurations.",
                registry.setConfigurations(configs2nd));

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
    public void hasConfigurations() {
        registry.setConfigurations(createConfigurations(MyComponentExporter.class,
                MyComponentExporter2.class, UserBoxExporter.class));

        Assert.assertTrue("Should have configurations, when 3 were set",
                registry.hasConfigurations());
    }

    @Test
    public void hasConfigurations_noConfigurations() {
        Assert.assertFalse("New registry should have no configurations",
                registry.hasConfigurations());

        registry.setConfigurations(Collections.emptySet());

        Assert.assertFalse("Should not have configurations, when empty set is" +
                        " given",
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
                        return new AtomicBoolean(registry.setConfigurations(
                                createConfigurations(MyComponentExporter.class)));
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

    protected Set<WebComponentConfiguration<? extends Component>> createConfigurations(
            Class<? extends WebComponentExporter<? extends Component>>... exporters) {
        WebComponentExporter.WebComponentConfigurationFactory factory =
                new WebComponentExporter.WebComponentConfigurationFactory();
        return Stream.of(exporters).map(factory::create)
                .collect(Collectors.toSet());
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
        public void configureInstance(WebComponent<MyComponent> webComponent, MyComponent component) {

        }
    }

    protected static class MyComponentExporter2
            extends WebComponentExporter<MyComponent> {

        public MyComponentExporter2() {
            super("my-component-2");
        }

        @Override
        public void configureInstance(WebComponent<MyComponent> webComponent, MyComponent component) {

        }
    }

    protected static class UserBoxExporter
            extends WebComponentExporter<UserBox> {

        public UserBoxExporter() {
            super("user-box");
        }

        @Override
        public void configureInstance(WebComponent<UserBox> webComponent, UserBox component) {

        }
    }
}
