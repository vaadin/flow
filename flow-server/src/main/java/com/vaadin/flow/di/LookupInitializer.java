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
package com.vaadin.flow.di;

import jakarta.servlet.ServletException;

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
import com.vaadin.flow.router.DefaultRoutePathProvider;
import com.vaadin.flow.router.RoutePathProvider;
import com.vaadin.flow.server.StaticFileHandler;
import com.vaadin.flow.server.StaticFileHandlerFactory;
import com.vaadin.flow.server.StaticFileServer;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.AppShellPredicate;
import com.vaadin.flow.server.startup.ApplicationConfigurationFactory;
import com.vaadin.flow.server.startup.DefaultApplicationConfigurationFactory;

/**
 * Default implementation of {@link AbstractLookupInitializer}.
 *
 * @author Vaadin Ltd
 *
 * @see AbstractLookupInitializer
 */
public class LookupInitializer implements AbstractLookupInitializer {

    protected static final String SPI = " SPI: ";

    protected static final String ONE_IMPL_REQUIRED = ". Only one implementation should be registered. "
            + "Use lookupAll to get all instances of the given type.";

    protected static final String SEVERAL_IMPLS = "Found several implementations in the classpath for ";

    /**
     * Default implementation of {@link Lookup}.
     *
     * @author Vaadin Ltd
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

    private static class RegularOneTimeInitializerPredicate
            implements OneTimeInitializerPredicate {

        @Override
        public boolean runOnce() {
            return true;
        }

    }

    private static class StaticFileHandlerFactoryImpl
            implements StaticFileHandlerFactory {
        @Override
        public StaticFileHandler createHandler(VaadinService service) {
            return new StaticFileServer(service);
        }
    }

    /**
     * Default implementation of {@link AppShellPredicate}.
     *
     * @author Vaadin Ltd
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

    @Override
    public void initialize(VaadinContext context,
            Map<Class<?>, Collection<Class<?>>> services,
            VaadinApplicationInitializationBootstrap bootstrap)
            throws ServletException {
        services.put(OneTimeInitializerPredicate.class, Collections
                .singleton(RegularOneTimeInitializerPredicate.class));
        ensureService(services, ResourceProvider.class,
                ResourceProviderImpl.class);
        ensureService(services, AppShellPredicate.class,
                AppShellPredicateImpl.class);
        ensureService(services, ApplicationConfigurationFactory.class,
                DefaultApplicationConfigurationFactory.class);
        ensureService(services, StaticFileHandlerFactory.class,
                StaticFileHandlerFactoryImpl.class);
        ensureService(services, RoutePathProvider.class,
                DefaultRoutePathProvider.class);
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

    /**
     * Instantiates service {@code implementation} type with the given
     * {@code serviceClass} .
     *
     * @param <T>
     *            service type
     * @param serviceClass
     *            service class
     * @param implementation
     *            service implementation class
     * @return an instantiated service implementation object
     */
    protected <T> T instantiate(Class<T> serviceClass,
            Class<?> implementation) {
        if (RegularOneTimeInitializerPredicate.class.equals(implementation)) {
            return serviceClass.cast(new RegularOneTimeInitializerPredicate());
        } else if (StaticFileHandlerFactoryImpl.class.equals(implementation)) {
            return serviceClass.cast(new StaticFileHandlerFactoryImpl());
        }
        return serviceClass.cast(ReflectTools.createInstance(implementation));
    }

    /**
     * Returns the default implementation classes included.
     * <p>
     * This method is public only for internal purposes.
     *
     * @return a set of classes
     */
    public static Set<Class<?>> getDefaultImplementations() {
        return Set.of(RegularOneTimeInitializerPredicate.class,
                StaticFileHandlerFactoryImpl.class, LookupImpl.class,
                ResourceProviderImpl.class, AppShellPredicateImpl.class,
                DefaultRoutePathProvider.class,
                DefaultApplicationConfigurationFactory.class);
    }
}
