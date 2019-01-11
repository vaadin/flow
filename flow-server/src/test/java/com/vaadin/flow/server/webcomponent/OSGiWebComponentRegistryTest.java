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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.osgi.OSGiAccess;
import com.vaadin.flow.server.startup.EnableOSGiRunner;

@RunWith(EnableOSGiRunner.class)
public class OSGiWebComponentRegistryTest extends WebComponentRegistryTest {

    @After
    public void cleanUp() {
        if (OSGiAccess.getInstance().getOsgiServletContext() != null) {
            WebComponentRegistry.getInstance(
                    OSGiAccess.getInstance().getOsgiServletContext());
        }
    }

    @Test
    public void assertWebComponentRegistry() {
        Assert.assertEquals(OSGiWebComponentRegistry.class.getName(),
                registry.getClass().getName());
    }

    @Test
    public void assertOsgiRegistryIsServedAsASingleton() {
        Assert.assertEquals(OSGiWebComponentRegistry.class.getName(),
                registry.getClass().getName());

        Assert.assertEquals(registry,
                WebComponentRegistry.getInstance(Mockito.mock(ServletContext.class)));
    }

    @Override
    public void setWebComponentsTwice_expectedSetIsStoredToRegistry() {
        Map<String, Class<? extends Component>> webComponents = mapWebComponents(
                MyComponent.class);

        Assert.assertTrue("Registry should have accepted the WebComponents",
                registry.setWebComponents(webComponents));

        Assert.assertTrue(
                "OSGi registry should have accept the second set of WebComponents.",
                registry.setWebComponents(mapWebComponents(UserBox.class)));

        Assert.assertNotEquals(
                "Stored WebComponents should be the latest set.",
                webComponents, registry.getWebComponents());
        Assert.assertEquals(mapWebComponents(UserBox.class), registry.getWebComponents());
    }

    @Override
    public void setSameRouteValueFromDifferentThreads_ConcurrencyTest()
            throws InterruptedException, ExecutionException {
        final int THREADS = 10;

        ExecutorService executorService = Executors.newFixedThreadPool(THREADS);

        List<Callable<AtomicBoolean>> callables = IntStream.range(0, THREADS)
                .mapToObj(i -> {
                    Callable<AtomicBoolean> callable = () -> {
                        // Add random sleep for better possibility to run at same time
                        Thread.sleep(new Random().nextInt(200));
                        return new AtomicBoolean(registry.setWebComponents(
                                mapWebComponents(MyComponent.class)));
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
        Assert.assertEquals("All threads should have updated for OSGi", 0,
                results.stream().filter(result -> result.get() == false)
                        .count());
    }
}
