/*
 * Copyright 2000-2025 Vaadin Ltd.
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
package com.vaadin.cdi;

import jakarta.annotation.Resource;
import jakarta.enterprise.concurrent.ManagedExecutorService;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.inject.Inject;

import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import org.slf4j.LoggerFactory;

import com.vaadin.cdi.annotation.VaadinServiceEnabled;

/**
 * Utility class responsible for selecting an {@link Executor} for task
 * execution within a Vaadin-based application.
 * <p>
 * The class attempts to provide a custom {@link Executor} that is marked with
 * the {@link VaadinServiceEnabled} qualifier, leveraging the CDI (Contexts and
 * Dependency Injection) mechanism for bean resolution. If such a custom
 * executor is resolvable, it is returned. Otherwise, a potential container
 * provided {@link ManagedExecutorService} is used as a fallback executor.
 */
class VaadinTaskExecutorSelector {

    /*
     * Workaround for environments that aren't fully Jakarta EE compliant (e.g.,
     * Tomcat with Weld).
     *
     * This inner class allows us to check at runtime whether
     * ManagedExecutorService is available without causing deployment failures.
     * By programmatically resolving this bean through CDI, we can gracefully
     * handle two potential issues: 1. Missing ManagedExecutorService in the
     * classpath 2. javax.naming.NameNotFoundException when JNDI lookup fails
     *
     * This approach prevents hard deployment failures and allows the
     * application to fall back to alternative execution strategies when
     * necessary.
     *
     * See https://github.com/vaadin/cdi/issues/476 for more details.
     */
    static class FromResource {
        @Resource
        ManagedExecutorService managedExecutor;
    }

    @Inject
    BeanManager beanManager;

    @Inject
    @VaadinServiceEnabled
    Instance<Executor> customExecutor;

    /**
     * Provides an {@link Optional} containing an {@link Executor}, based on the
     * resolvability of a custom {@link Executor} annotated with
     * {@code @VaadinServiceEnabled}. If a custom executor is not resolvable,
     * the method falls back to a default {@link ManagedExecutorService}.
     *
     * @return an {@link Optional} containing the selected {@link Executor}, or
     *         an empty {@link Optional} if no executor is available.
     */
    Optional<Executor> getExecutor() {
        if (customExecutor.isResolvable()) {
            LoggerFactory.getLogger(VaadinTaskExecutorSelector.class).debug(
                    "Using custom Vaadin Executor {}",
                    customExecutor.getHandle().getBean());
            return Optional.of(customExecutor.get());
        } else if (customExecutor.isAmbiguous()) {
            String candidates = customExecutor.handlesStream()
                    .map(handle -> handle.getBean().toString())
                    .collect(Collectors.joining(", ", "[", "]"));
            String message = String.format(
                    "Multiple Executor beans annotated with @%1$s found: %2$s. "
                            + "Please make sure a single instance is resolvable.",
                    VaadinServiceEnabled.class.getSimpleName(), candidates);
            throw new IllegalStateException(message);
        }
        Instance<FromResource> managerFromResource = beanManager
                .createInstance().select(FromResource.class);

        ExecutorService resolvedManagedExecutor = null;
        if (managerFromResource.isResolvable()) {
            resolvedManagedExecutor = managerFromResource.get().managedExecutor;
        }
        if (resolvedManagedExecutor != null) {
            LoggerFactory.getLogger(VaadinTaskExecutorSelector.class)
                    .debug("Using container Managed Executor");
        }
        return Optional.ofNullable(resolvedManagedExecutor);
    }
}
