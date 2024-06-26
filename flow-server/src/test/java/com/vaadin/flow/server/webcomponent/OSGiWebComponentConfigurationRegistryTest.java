/**
 * Copyright (C) 2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See {@literal <https://vaadin.com/commercial-license-and-service-terms>}  for the full
 * license.
 */

package com.vaadin.flow.server.webcomponent;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.vaadin.flow.server.osgi.OSGiAccess;
import com.vaadin.flow.server.startup.EnableOSGiRunner;

@NotThreadSafe
@RunWith(EnableOSGiRunner.class)
public class OSGiWebComponentConfigurationRegistryTest
        extends WebComponentConfigurationRegistryTest {

    @Override
    protected WebComponentConfigurationRegistry createRegistry() {
        return new OSGiWebComponentConfigurationRegistry();
    }

    @Test
    @Override
    public void assertRegistryIsSingleton() {
        Assert.assertSame(
                "OSGiWebComponentConfigurationRegistry instance should be singleton",
                registry,
                OSGiWebComponentConfigurationRegistry.getInstance(context));
    }

    @After
    public void cleanUpOSGi() {
        OSGiAccess.getInstance().getOsgiServletContext().removeAttribute(
                WebComponentConfigurationRegistry.class.getName());
    }

    @Override
    @Test
    public void assertWebComponentRegistry() {
        Assert.assertEquals(
                OSGiWebComponentConfigurationRegistry.class.getName(),
                registry.getClass().getName());
    }

    @Override
    public void setConfigurationsTwice_onlyFirstSetIsAccepted() {
        // OSGi accepts setting the web components multiple times.
        // NO-OP
    }

    @Override
    public void setConfigurations_getConfigurationsCallDoesNotChangeSetProtection() {
        // OSGi accepts setting the web components multiple times.
        // NO-OP
    }

    @Test
    public void setBuildersTwice_allSetsAcceptedLastSetValid() {
        Assert.assertFalse("Registry should have no configurations",
                registry.hasConfigurations());

        Assert.assertTrue(
                "Registry should have accepted the " + "WebComponentExporters",
                registry.setConfigurations(
                        createConfigurations(MyComponentExporter.class)));

        Assert.assertTrue(
                "OSGi registry should have accept the second set of "
                        + "WebComponentExporters.",
                registry.setConfigurations(
                        createConfigurations(UserBoxExporter.class)));

        Assert.assertEquals("Registry should contain only one builder", 1,
                registry.getConfigurations().size());

        Assert.assertEquals("Builder should be linked to UserBox.class",
                UserBox.class, registry.getConfiguration("user-box").get()
                        .getComponentClass());

        Assert.assertTrue("Registry should have configurations",
                registry.hasConfigurations());
    }

    @Override
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

        Assert.assertEquals("Expected a result for all threads", THREADS,
                futures.size());

        List<AtomicBoolean> results = new ArrayList<>();
        for (Future<AtomicBoolean> resultFuture : futures) {
            results.add(resultFuture.get());
        }
        Assert.assertEquals("All threads should have updated for OSGi", 0,
                results.stream().filter(result -> !result.get()).count());
    }
}
