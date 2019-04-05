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
import org.mockito.Mockito;

import com.vaadin.flow.server.osgi.OSGiAccess;
import com.vaadin.flow.server.startup.EnableOSGiRunner;

@NotThreadSafe
@RunWith(EnableOSGiRunner.class)
public class OSGiWebComponentConfigurationRegistryTest extends WebComponentConfigurationRegistryTest {

    @After
    public void cleanUpOSGi() {
        if (OSGiAccess.getInstance().getOsgiServletContext() != null) {
            OSGiWebComponentConfigurationRegistry.getInstance(
                    OSGiAccess.getInstance().getOsgiServletContext());
        }
    }

    @Test
    public void assertWebComponentRegistry() {
        Assert.assertEquals(OSGiWebComponentConfigurationRegistry.class.getName(),
                registry.getClass().getName());
    }

    @Test
    public void assertOsgiRegistryIsServedAsASingleton() {
        Assert.assertEquals(registry, WebComponentConfigurationRegistry
                .getInstance(Mockito.mock(ServletContext.class)));
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
        Assert.assertTrue("Registry should have accepted the " +
                        "WebComponentExporters",
                registry.setConfigurations(createConfigurations(MyComponentExporter.class)));

        Assert.assertTrue(
                "OSGi registry should have accept the second set of " +
                        "WebComponentExporters.",
                registry.setConfigurations(createConfigurations(UserBoxExporter.class)));

        Assert.assertEquals("Registry should contain only one builder",
                1, registry.getConfigurations().size());

        Assert.assertEquals("Builder should be linked to UserBox.class",
                UserBox.class, registry.getConfiguration("user-box").get()
                        .getComponentClass());
    }

    @Override
    public void hasConfigurations() {
        // being a singleton caused this test to fail when inherited

        OSGiWebComponentConfigurationRegistry registry =
                new OSGiWebComponentConfigurationRegistry();

        Assert.assertFalse("Should have no exporters", registry.hasConfigurations());

        registry.setConfigurations(Collections.emptySet());

        Assert.assertTrue("Should have exporters, albeit empty",
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
                results.stream().filter(result -> !result.get())
                        .count());
    }
}
