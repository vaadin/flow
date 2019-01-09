package com.vaadin.flow.server.webcomponent;

import javax.servlet.ServletContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponent;

public class WebComponentRegistryTest {

    protected WebComponentRegistry registry;

    @Before
    public void init() {
        registry = WebComponentRegistry
                .getInstance(Mockito.mock(ServletContext.class));
    }

    @Test
    public void assertWebComponentRegistry() {
        Assert.assertEquals(WebComponentRegistry.class, registry.getClass());
    }

    @Test
    public void setWebComponents_allCanBeFoundInRegistry() {
        Map<String, Class<? extends Component>> webComponents = mapWebComponents(
                MyComponent.class, UserBox.class);
        Assert.assertTrue("Registry should have accepted the webComponents",
                registry.setWebComponents(webComponents));

        Assert.assertEquals("Expected two targets to be registered", 2,
                registry.getWebComponents().size());

        Assert.assertEquals(
                "Tag 'my-component' should have returned 'MyComponent.class'",
                MyComponent.class,
                registry.getWebComponent("my-component").orElse(null));
        Assert.assertEquals(
                "Tag 'user-box' should have returned 'UserBox.class'",
                UserBox.class,
                registry.getWebComponent("user-box").orElse(null));

    }

    @Test
    public void setWebComponentsTwice_onlyFirstIsStoredAndSecondReceivesFalse() {
        Map<String, Class<? extends Component>> webComponents = mapWebComponents(
                MyComponent.class);

        Assert.assertTrue("Registry should have accepted the WebComponents",
                registry.setWebComponents(webComponents));

        Assert.assertFalse(
                "Registry should not accept a second set of WebComponents.",
                registry.setWebComponents(mapWebComponents(UserBox.class)));

        Assert.assertEquals(
                "Stored WebComponents should be the same as first given.",
                webComponents, registry.getWebComponents());
    }

    @Test
    public void setSameRouteValueFromDifferentThreads_ConcurrencyTest()
            throws InterruptedException, ExecutionException {
        final int THREADS = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(THREADS);

        List<Callable<Result>> callables = IntStream.range(0, THREADS)
                .mapToObj(i -> {
                    Callable<Result> callable = () -> {
                        Thread.sleep(new Random().nextInt(200));
                        return new Result(registry.setWebComponents(mapWebComponents(MyComponent.class)));
                    };
                    return callable;
                }).collect(Collectors.toList());

        List<Future<Result>> futures = executorService.invokeAll(callables);

        executorService.shutdown();

        Assert.assertEquals("Expected a result for all threads", THREADS,
                futures.size());

        List<Result> results = new ArrayList<>();
        for (Future<Result> resultFuture : futures) {
            results.add(resultFuture.get());
        }

        Assert.assertEquals("Expected all except one thread to return false",
                THREADS - 1,
                results.stream().filter(result -> result.value == false)
                        .count());

    }

    private static class Result {
        final boolean value;

        Result(boolean value) {
            this.value = value;
        }
    }

    private Map<String, Class<? extends Component>> mapWebComponents(
            Class<? extends Component>... components) {
        return Stream.of(components).collect(Collectors
                .toMap(c -> c.getAnnotation(WebComponent.class).value(),
                        c -> c));
    }

    @WebComponent("my-component")
    public class MyComponent extends Component {
    }

    @WebComponent("user-box")
    public class UserBox extends Component {
    }
}
