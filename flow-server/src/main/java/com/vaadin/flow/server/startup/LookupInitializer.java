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
package com.vaadin.flow.server.startup;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.IOUtils;

import com.vaadin.flow.component.template.internal.DeprecatedPolymerPublishedEventHandler;
import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.frontend.EndpointGeneratorTaskFactory;

/**
 * Standard servlet initializer for collecting all SPI implementations.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
@HandlesTypes({ ResourceProvider.class, InstantiatorFactory.class,
        DeprecatedPolymerPublishedEventHandler.class,
        EndpointGeneratorTaskFactory.class })
public class LookupInitializer
        implements ClassLoaderAwareServletContainerInitializer {

    private static final String SPI = " SPI: ";

    private static final String ONE_IMPL_REQUIRED = ". Only one implementation should be registered. "
            + "Use lookupAll to get all instances of the given type.";

    private static final String SEVERAL_IMPLS = "Found several implementations in the classpath for ";

    /**
     * This class is private because it's an implementation detail/one of the
     * possible implementation and is explicitly made non-overridable because it
     * provides the way to override everything.
     */
    private static class LookupImpl implements Lookup {

        private final Map<Class<?>, Collection<Object>> serviceMap;

        private LookupImpl(Map<Class<?>, Collection<Object>> initialServices) {
            serviceMap = Collections
                    .unmodifiableMap(new HashMap<>(initialServices));
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

    private static class CachedStreamData {

        private final byte[] data;
        private final IOException exception;

        private CachedStreamData(byte[] data, IOException exception) {
            this.data = data;
            this.exception = exception;
        }
    }

    /**
     * This class is private because it's an implementation detail/one of the
     * possible implementation and should not be available as public because
     * {@link LookupInitializer} will find it in the classpath and it will be
     * always used instead custom {@link ResourceProvider} implementation.
     */
    private static class ResourceProviderImpl implements ResourceProvider {

        private Map<String, CachedStreamData> cache = new ConcurrentHashMap<>();

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

    @Override
    public void process(Set<Class<?>> classSet, ServletContext servletContext)
            throws ServletException {
        VaadinServletContext vaadinContext = new VaadinServletContext(
                servletContext);
        initStandardLookup(classSet, servletContext);

        DeferredServletContextInitializers initializers;
        synchronized (servletContext) {
            initializers = vaadinContext
                    .getAttribute(DeferredServletContextInitializers.class);
            vaadinContext
                    .removeAttribute(DeferredServletContextInitializers.class);
        }

        if (initializers != null) {
            initializers.runInitializers(servletContext);
        }
    }

    @Override
    public boolean requiresLookup() {
        return false;
    }

    /**
     * Creates a lookup based on provided {@code services}.
     * 
     * @param services
     *            the service objects mapped to the service type to create a
     *            lookup
     * @return the lookup instance created with provided services
     */
    protected Lookup createLookup(Map<Class<?>, Collection<Object>> services) {
        return new LookupImpl(services);
    }

    /**
     * Gets the service types that are used to set services into the
     * {@link Lookup} based on found subtypes by the
     * {@link ServletContainerInitializer}.
     * <p>
     * {@link LookupInitializer} uses {@link ServletContainerInitializer}
     * classes discovering mechanism based on {@link HandlesTypes} annotation.
     * The method may be overridden to return the service types which should be
     * put into the {@link Lookup} instance if another mechanism of class
     * searching is used (e.g. Spring boot case).
     * <p>
     * The set of classes (passed into the {@link #process(Set, ServletContext)}
     * method) will be filtered via checking whether they are assignable to the
     * service types and the resulting classes will be instantiated via
     * reflection.
     * 
     * @return a collection of service types which should be available via
     *         Lookup
     * @see LookupInitializer#createLookup(Map)
     */
    protected Collection<Class<?>> getServiceTypes() {
        HandlesTypes annotation = getClass().getAnnotation(HandlesTypes.class);
        if (annotation == null) {
            throw new IllegalStateException(
                    "Cannot collect service types based on "
                            + HandlesTypes.class.getSimpleName()
                            + " annotation. The default 'getServiceTypes' method implementation can't be used.");
        }
        return Stream.of(annotation.value()).collect(Collectors.toSet());
    }

    private void initStandardLookup(Set<Class<?>> classSet,
            ServletContext servletContext) {
        VaadinServletContext vaadinContext = new VaadinServletContext(
                servletContext);

        Map<Class<?>, Collection<Object>> services = new HashMap<>();

        for (Class<?> serviceType : getServiceTypes()) {
            if (ResourceProvider.class.equals(serviceType)) {
                collectResourceProviders(classSet, services);
            } else {
                collectSubclasses(serviceType, classSet, services);
            }
        }

        vaadinContext.setAttribute(Lookup.class, createLookup(services));
    }

    private void collectSubclasses(Class<?> clazz, Set<Class<?>> classSet,
            Map<Class<?>, Collection<Object>> services) {
        services.put(clazz,
                filterSubClasses(clazz, classSet).stream()
                        .map(ReflectTools::createInstance)
                        .collect(Collectors.toList()));
    }

    private void collectResourceProviders(Set<Class<?>> classSet,
            Map<Class<?>, Collection<Object>> services) {
        Set<Class<?>> providers = filterResourceProviders(classSet);
        if (providers.isEmpty()) {
            services.put(ResourceProvider.class,
                    Collections.singletonList(new ResourceProviderImpl()));
        } else if (providers.size() > 1) {
            throw new IllegalStateException(
                    SEVERAL_IMPLS + ResourceProvider.class.getSimpleName() + SPI
                            + classSet + ONE_IMPL_REQUIRED);
        } else {
            Class<?> clazz = providers.iterator().next();
            services.put(ResourceProvider.class, Collections
                    .singletonList(ReflectTools.createInstance(clazz)));
        }
    }

    private Set<Class<?>> filterResourceProviders(Set<Class<?>> classes) {
        Set<Class<?>> resourceProviders = filterSubClasses(
                ResourceProvider.class, classes);
        resourceProviders.remove(ResourceProviderImpl.class);
        return resourceProviders;
    }

    private Set<Class<?>> filterSubClasses(Class<?> clazz,
            Set<Class<?>> classes) {
        return classes == null ? Collections.emptySet()
                : classes.stream().filter(clazz::isAssignableFrom)
                        .filter(cls -> !cls.isInterface() && !cls.isSynthetic()
                                && !Modifier.isAbstract(cls.getModifiers()))
                        .filter(cls -> !clazz.equals(cls))
                        .collect(Collectors.toSet());
    }
}
