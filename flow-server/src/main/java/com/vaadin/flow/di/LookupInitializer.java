/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.di;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.function.VaadinApplicationInitializationBootstrap;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.startup.AppShellPredicate;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;
import com.vaadin.flow.server.startup.DefaultApplicationConfigurationFactory;
import com.vaadin.flow.server.startup.LookupServletContainerInitializer;

/**
 * SPI for customizing lookup in applications inside Servlet 3.0 containers.
 * <p>
 * There are two ways of customizing Lookup in various servlet containers:
 * <ul>
 * <li>Use {@link LookupInitializer} SPI via providing an implementation for the
 * framework which doesn't prevent {@link LookupServletContainerInitializer}
 * execution.
 * <li>Completely disable {@link LookupServletContainerInitializer} and
 * implement own way to set up {@link Lookup} and make it available via
 * {@link VaadinContext#getAttribute(Class)}.
 * </ul>
 * 
 * The first case allows to customize {@link Lookup} creation and initialization
 * in case when it's not possible to prevent
 * {@link LookupServletContainerInitializer} execution (any container which
 * completely supports Servlet 3.0 specification). In this case it's possible to
 * implement {@link LookupInitializer} for the framework.
 * <p>
 * The second case is only possible when a servlet container doesn't run
 * {@link ServletContainerInitializer}s out of the box (e.g. OSGi or Spring boot
 * executed as a Jar) at all. Otherwise you may not disable an existing
 * {@link ServletContainerInitializer} and it will be executed anyway.
 * <p>
 * This is SPI for {@link Lookup} SPI. The difference is:
 * <ul>
 * <li>{@link Lookup} allows to override services per Web application (by the
 * application developer). For some service interfaces there can be several
 * implementations available in {@link Lookup}.
 * <li>{@link LookupInitializer} allows to override how the {@link Lookup} works
 * per framework. The default implementation available if no framework is used.
 * Only one service implementation (excluding the default one) may be available
 * in the web application classpath and it's provided by the developers for the
 * framework support (the main usecase here is Spring add-on).
 * </ul>
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
public class LookupInitializer {

    protected static final String SPI = " SPI: ";

    protected static final String ONE_IMPL_REQUIRED = ". Only one implementation should be registered. "
            + "Use lookupAll to get all instances of the given type.";

    protected static final String SEVERAL_IMPLS = "Found several implementations in the classpath for ";

    /**
     * Default implementation of {@link Lookup}.
     * 
     * @author Vaadin Ltd
     * @since
     *
     */
    protected static class LookupImpl implements Lookup {

        protected final Map<Class<?>, Collection<Object>> serviceMap;

        /**
         * Creates a new instance of {@link Lookup} with services found in the
         * application classpath.
         * 
         * @param initialServices
         *            map of initial services with their implementations
         * @param factory
         *            a factory to create a service object instance
         */
        protected LookupImpl(
                Map<Class<?>, Collection<Class<?>>> initialServices,
                BiFunction<Class<?>, Class<?>, Object> factory) {
            serviceMap = new HashMap<>();
            initialServices.forEach((serviceClass,
                    impls) -> serviceMap.put(serviceClass, impls.stream()
                            .map(impl -> factory.apply(serviceClass, impl))
                            .filter(Objects::nonNull)
                            .collect(Collectors.toList())));
        }

        @Override
        public <T> T lookup(Class<T> serviceClass) {
            Collection<Object> registered = serviceMap.get(serviceClass);
            if (registered == null || registered.isEmpty()) {
                ServiceLoader<T> loader = ServiceLoader.load(serviceClass);
                List<T> services = new ArrayList<>();
                for (Iterator<T> iterator = loader.iterator(); iterator
                        .hasNext();) {
                    services.add(iterator.next());
                }
                if (services.size() > 1) {
                    throw new IllegalStateException(SEVERAL_IMPLS + serviceClass
                            + SPI + services + ONE_IMPL_REQUIRED);
                } else if (services.size() == 1) {
                    return services.get(0);
                }
                return null;
            } else if (registered.size() > 1) {
                throw new IllegalStateException(SEVERAL_IMPLS + serviceClass
                        + SPI + registered + ONE_IMPL_REQUIRED);
            } else {
                return serviceClass.cast(registered.iterator().next());
            }
        }

        @Override
        public <T> Collection<T> lookupAll(Class<T> serviceClass) {
            List<T> result = new ArrayList<>();
            Collection<Object> registered = serviceMap.get(serviceClass);

            Set<?> registeredClasses = registered == null
                    ? Collections.emptySet()
                    : registered.stream().map(Object::getClass)
                            .collect(Collectors.toSet());
            if (registered != null) {
                registered.forEach(
                        service -> result.add(serviceClass.cast(service)));
            }
            ServiceLoader<T> loader = ServiceLoader.load(serviceClass);
            for (Iterator<T> iterator = loader.iterator(); iterator
                    .hasNext();) {
                T next = iterator.next();
                if (!registeredClasses.contains(next.getClass())) {
                    result.add(next);
                }
            }
            return result;
        }

    }

    /**
     * Default implementation of {@link ResourceProvider}.
     * 
     * @author Vaadin Ltd
     * @since
     *
     */
    protected static class ResourceProviderImpl implements ResourceProvider {

        private Map<String, CachedStreamData> cache = new ConcurrentHashMap<>();

        /**
         * Creates a new instance.
         */
        public ResourceProviderImpl() {
        }

        @Override
        public URL getApplicationResource(String path) {
            return ResourceProviderImpl.class.getClassLoader()
                    .getResource(path);
        }

        @Override
        public List<URL> getApplicationResources(String path)
                throws IOException {
            return Collections.list(ResourceProviderImpl.class.getClassLoader()
                    .getResources(path));
        }

        @Override
        public URL getClientResource(String path) {
            return getApplicationResource(path);
        }

        @Override
        public InputStream getClientResourceAsStream(String path)
                throws IOException {
            // the client resource should be available in the classpath, so
            // its content is cached once. If an exception is thrown then
            // something is broken and it's also cached and will be rethrown on
            // every subsequent access
            CachedStreamData cached = cache.computeIfAbsent(path, key -> {
                URL url = getClientResource(key);
                try (InputStream stream = url.openStream()) {
                    ByteArrayOutputStream tempBuffer = new ByteArrayOutputStream();
                    IOUtils.copy(stream, tempBuffer);
                    return new CachedStreamData(tempBuffer.toByteArray(), null);
                } catch (IOException e) {
                    return new CachedStreamData(null, e);
                }
            });

            IOException exception = cached.exception;
            if (exception == null) {
                return new ByteArrayInputStream(cached.data);
            }
            throw exception;
        }

    }

    /**
     * Default implementation of {@link AppShellPredicate}.
     * 
     * @author Vaadin Ltd
     * @since
     *
     */
    protected static class AppShellPredicateImpl implements AppShellPredicate {

        /**
         * Creates a new instance.
         */
        public AppShellPredicateImpl() {
        }

        @Override
        public boolean isShell(Class<?> clz) {
            return AppShellConfigurator.class.isAssignableFrom(clz);
        }
    }

    private static class CachedStreamData {

        private final byte[] data;
        private final IOException exception;

        private CachedStreamData(byte[] data, IOException exception) {
            this.data = data;
            this.exception = exception;
        }
    }

    /**
     * Creates a new {@link Lookup} instance, initializes it and passes it to
     * the provided {@code bootstrap}.
     * <p>
     * The method should creates a new initialized {@link Lookup} instance. In
     * some cases it's not possible to create the instance right away when the
     * method is called. To be able to support this usecase the method contract
     * doesn't require to return the {@link Lookup} instance. Instead the
     * created instance should be passed to the provided {@code bootstrap}
     * consumer once the instance is created and completely initialized. The
     * {@code bootstrap} will start the application initialization which
     * otherwise is postponed until a {@link Lookup} becomes available.
     * <p>
     * The implementation must use the provided {@code bootstrap} to pass the
     * {@link Lookup} instance otherwise the web application based on this
     * {@link LookupInitializer} will never be bootstrapped.
     * <p>
     * The provided {@code services} map contains service implementations found
     * in application classpath using {@code @HandlesTypes} annotation declared
     * for {@link LookupServletContainerInitializer}.
     * 
     * @param context
     *            a Vaadin context to run initialization for
     * @param services
     *            the map of internal services with their implementations found
     *            in the application classpath
     * @param bootstrap
     *            the web application bootstrap
     * @throws ServletException
     *             if initialization failed
     */
    public void initialize(VaadinContext context,
            Map<Class<?>, Collection<Class<?>>> services,
            VaadinApplicationInitializationBootstrap bootstrap)
            throws ServletException {
        ensureService(services, ResourceProvider.class,
                ResourceProviderImpl.class);
        ensureService(services, AppShellPredicate.class,
                AppShellPredicateImpl.class);
        ensureService(services, ApplicationConfigurationFactory.class,
                DefaultApplicationConfigurationFactory.class);
        bootstrap.bootstrap(createLookup(context, services));
    }

    /**
     * Creates a lookup based on provided {@code services}.
     * 
     * @param context
     *            a Vaadin context to create a lookup for
     * 
     * @param services
     *            the service objects mapped to the service type to create a
     *            lookup
     * @return the lookup instance created with provided services
     */
    protected Lookup createLookup(VaadinContext context,
            Map<Class<?>, Collection<Class<?>>> services) {
        return new LookupImpl(services, this::instantiate);
    }

    /**
     * Ensures that provided {@code services} contain implementation for
     * {@code serviceType} SPI.
     * <p>
     * The default {@code  serviceImpl} implementation will be set as the
     * service into {@code services} if there is no other services available.
     * 
     * @param services
     *            map of internal services
     * @param serviceType
     *            SPI type
     * @param serviceImpl
     *            the default SPI implementation
     */
    protected <T> void ensureService(
            Map<Class<?>, Collection<Class<?>>> services, Class<T> serviceType,
            Class<? extends T> serviceImpl) {
        Collection<Class<?>> impls = services.get(serviceType);
        if (impls == null) {
            impls = Collections.emptyList();
        }
        impls = impls.stream().filter(clazz -> !clazz.equals(serviceImpl))
                .collect(Collectors.toList());
        if (impls.isEmpty()) {
            services.put(serviceType, Collections.singletonList(serviceImpl));
        } else if (impls.size() > 1) {
            throw new IllegalStateException(
                    SEVERAL_IMPLS + serviceType.getSimpleName() + SPI + impls
                            + ONE_IMPL_REQUIRED);
        } else {
            services.put(serviceType, impls);
        }
    }

    private <T> T instantiate(Class<T> serviceClass, Class<?> implementation) {
        if (ResourceProviderImpl.class.equals(implementation)) {
            return serviceClass.cast(new ResourceProviderImpl());
        } else {
            return serviceClass
                    .cast(ReflectTools.createInstance(implementation));
        }
    }

}
