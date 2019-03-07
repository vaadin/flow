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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class WebComponentRegistry2Test {

    private static final String MY_COMPONENT_TAG = "my-component";
    private static final String USER_BOX_TAG = "user-box";

    protected WebComponentRegistry2 registry;

    protected WebComponentExporter<MyComponent> myComponentExporter;
    protected WebComponentBuilder<MyComponent> myComponentBuilder;

    protected WebComponentExporter<UserBox> userBoxExporter;
    protected WebComponentBuilder<UserBox> userBoxBuilder;


    @Before
    public void init() {
        registry = WebComponentRegistry2
                .getInstance(mock(ServletContext.class));

        myComponentExporter = mock(WebComponentExporter.class);
        when(myComponentExporter.getTag()).thenReturn(MY_COMPONENT_TAG);

        userBoxExporter = mock(WebComponentExporter.class);
        when(userBoxExporter.getTag()).thenReturn(USER_BOX_TAG);

        myComponentBuilder = new WebComponentBuilder<>(myComponentExporter);
        userBoxBuilder = new WebComponentBuilder<>(userBoxExporter);
    }

    @Test
    public void assertWebComponentRegistry() {
        Assert.assertEquals(WebComponentRegistry2.class, registry.getClass());
    }

    @Test
    public void setWebComponents_allCanBeFoundInRegistry() {
        Set<WebComponentBuilder<? extends Component>> webComponents =
                asSet(myComponentBuilder, userBoxBuilder);

        Assert.assertTrue("Registry should have accepted the webComponents",
                registry.setWebComponentBuilders(webComponents));

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
    public void setWebComponentsTwice_onlyFirstSetIsAccepted() {
        Set<WebComponentBuilder<? extends Component>> webComponents1st =
                asSet(myComponentBuilder);

        Set<WebComponentBuilder<? extends Component>> webComponents2nd =
                asSet(userBoxBuilder);

        Assert.assertTrue("Registry should have accepted the WebComponents",
                registry.setWebComponentBuilders(webComponents1st));

        Assert.assertFalse(
                "Registry should not accept a second set of WebComponents.",
                registry.setWebComponentBuilders(webComponents2nd));

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

    private <T> Set<T> asSet(T... things) {
        return new HashSet<>(Arrays.asList(things));
    }

    public class MyComponent extends Component {
    }

    public class UserBox extends Component {
    }
}
