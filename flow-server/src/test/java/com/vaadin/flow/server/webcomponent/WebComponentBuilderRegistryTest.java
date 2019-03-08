/*
 * Copyright 2000-2019 Vaadin Ltd.
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
import java.util.Arrays;
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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.webcomponent.WebComponentDefinition;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebComponentBuilderRegistryTest {

    private static final String MY_COMPONENT_TAG = "my-component";
    private static final String USER_BOX_TAG = "user-box";

    protected WebComponentBuilderRegistry registry;

    protected WebComponentBuilder<MyComponent> myComponentBuilder;
    protected WebComponentBuilder<UserBox> userBoxBuilder;


    @Before
    public void init() {
        registry = WebComponentBuilderRegistry
                .getInstance(mock(ServletContext.class));

        myComponentBuilder = new WebComponentBuilder<>(new MyComponentExporter());
        userBoxBuilder = new WebComponentBuilder<>(new UserBoxExporter());
    }

    @Test
    public void assertWebComponentRegistry() {
        Assert.assertEquals(WebComponentBuilderRegistry.class, registry.getClass());
    }

    @Test
    public void setWebComponents_allCanBeFoundInRegistry() {
        Set<WebComponentBuilder<? extends Component>> builders =
                asSet(myComponentBuilder, userBoxBuilder);

        Assert.assertTrue("Registry should have accepted the webComponents",
                registry.setWebComponentBuilders(builders));

        Assert.assertEquals("Expected two targets to be registered", 2,
                registry.getWebComponentBuilders().size());

        Assert.assertEquals(
                "Tag 'my-component' should have returned " +
                        "'WebComponentBuilder' matching MyComponent",
                myComponentBuilder,
                registry.getWebComponentBuilder(MY_COMPONENT_TAG));
        Assert.assertEquals(
                "Tag 'user-box' should have returned 'WebComponentBuilder' " +
                        "matching UserBox",
                userBoxBuilder,
                registry.getWebComponentBuilder(USER_BOX_TAG));

    }

    @Test
    public void getWebComponentBuildersForComponent_findsAllBuildersForAComponent() {
        WebComponentBuilder<MyComponent> myComponentBuilder2nd =
                new WebComponentBuilder<>(new MyComponentExporter2());

        Set<WebComponentBuilder<? extends Component>> builders =
                asSet(myComponentBuilder, myComponentBuilder2nd, userBoxBuilder);

        registry.setWebComponentBuilders(builders);

        Set<WebComponentBuilder<MyComponent>> set =
                registry.getWebComponentBuildersForComponent(MyComponent.class);

        Assert.assertEquals("Builder set should contain two builders", 2,
                set.size());

        Assert.assertTrue("Builder set should contain both MyComponent " +
                "builders", set.containsAll(asSet(myComponentBuilder,
                myComponentBuilder2nd)));
    }

    @Test
    public void setWebComponentsTwice_onlyFirstSetIsAccepted() {
        Set<WebComponentBuilder<? extends Component>> builders1st =
                asSet(myComponentBuilder);

        Set<WebComponentBuilder<? extends Component>> builders2nd =
                asSet(userBoxBuilder);

        Assert.assertTrue("Registry should have accepted the WebComponents",
                registry.setWebComponentBuilders(builders1st));

        Assert.assertFalse(
                "Registry should not accept a second set of WebComponents.",
                registry.setWebComponentBuilders(builders2nd));

        Assert.assertEquals(
                "Builders from the first Set should have been added",
                myComponentBuilder, registry.getWebComponentBuilder("my" +
                        "-component"));

        Assert.assertNull(
                "Components from the second Set should not have been added",
                registry.getWebComponentBuilder("user-box"));
    }

    @Test
    public void setSameRouteValueFromDifferentThreads_ConcurrencyTest()
            throws InterruptedException, ExecutionException {
        final int THREADS = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(THREADS);

        List<Callable<AtomicBoolean>> callables = IntStream.range(0, THREADS)
                .mapToObj(i -> {
                    Callable<AtomicBoolean> callable = () -> {
                        // Add random sleep for better possibility to run at same time
                        Thread.sleep(new Random().nextInt(200));
                        return new AtomicBoolean(registry.setWebComponentBuilders(
                                asSet(myComponentBuilder)));
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
                results.stream().filter(result -> result.get() == false)
                        .count());

    }

    protected  <T> Set<T> asSet(T... things) {
        return new HashSet<>(Arrays.asList(things));
    }

    public class MyComponent extends Component {
    }

    public class UserBox extends Component {
    }

    public static class MyComponentExporter implements WebComponentExporter<MyComponent> {

        @Override
        public String getTag() {
            return "my-component";
        }

        @Override
        public void define(WebComponentDefinition<MyComponent> definition) {}
    }

    public static class MyComponentExporter2 implements WebComponentExporter<MyComponent> {

        @Override
        public String getTag() {
            return "my-component-2";
        }

        @Override
        public void define(WebComponentDefinition<MyComponent> definition) {}
    }

    public static class UserBoxExporter implements WebComponentExporter<UserBox> {

        @Override
        public String getTag() {
            return "user-box";
        }

        @Override
        public void define(WebComponentDefinition<UserBox> definition) {}
    }
}
