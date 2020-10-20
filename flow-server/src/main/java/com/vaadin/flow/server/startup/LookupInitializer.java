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
import java.util.stream.Collectors;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.HandlesTypes;

import org.apache.commons.io.IOUtils;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.osgi.OSGiAccess;

/**
 * Standard servlet initializer for collecting all SPI implementations.
 * 
 * @author Vaadin Ltd
 * @since
 *
 */
@HandlesTypes({ ResourceProvider.class, InstantiatorFactory.class })
public class LookupInitializer
        implements ClassLoaderAwareServletContainerInitializer {

    private static final String SPI = " SPI: ";

    private static final String ONE_IMPL_REQUIRED = ". Only one implementation should be registered";

    private static final String SEVERAL_IMPLS = "Found several implementations in the classpath for ";

    private static class LookupImpl implements Lookup {

        private final Map<Class<?>, Collection<Object>> services;

        private LookupImpl(Map<Class<?>, Collection<Object>> initialServices) {
            services = Collections
                    .unmodifiableMap(new HashMap<>(initialServices));
        }

        @Override
        public <T> T lookup(Class<T> serviceClass) {
            Collection<Object> registered = services.get(serviceClass);
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
            Collection<Object> registered = services.get(serviceClass);

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

    private static class OsgiLookupImpl implements Lookup {

        @Override
        public <T> T lookup(Class<T> serviceClass) {
            Bundle bundle = FrameworkUtil.getBundle(LookupInitializer.class);
            ServiceReference<T> reference = bundle.getBundleContext()
                    .getServiceReference(serviceClass);
            if (reference == null) {
                LoggerFactory.getLogger(OsgiLookupImpl.class)
                        .debug("No service found for '{}' SPI", serviceClass);
                return null;
            }
            return bundle.getBundleContext().getService(reference);
        }

        @Override
        public <T> Collection<T> lookupAll(Class<T> serviceClass) {
            Bundle bundle = FrameworkUtil.getBundle(LookupInitializer.class);
            try {
                Collection<ServiceReference<T>> references = bundle
                        .getBundleContext()
                        .getServiceReferences(serviceClass, null);
                List<T> services = new ArrayList<>(references.size());
                for (ServiceReference<T> reference : references) {
                    T service = bundle.getBundleContext().getService(reference);
                    if (service != null) {
                        services.add(service);
                    }
                }
                return services;
            } catch (InvalidSyntaxException e) {
                LoggerFactory.getLogger(OsgiLookupImpl.class).error(
                        "Unexpected exception which is not expected to be thrown",
                        e);
                assert false : "Serveice filter is null so this may not happen";
            }

            return Collections.emptyList();
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

    private static class ResourceProviderImpl implements ResourceProvider {

        private Map<String, CachedStreamData> cache = new ConcurrentHashMap<>();

        @Override
        public URL getResource(Class<?> clazz, String path) {
            return Objects.requireNonNull(clazz).getResource(path);
        }

        @Override
        public URL getResource(Object context, String path) {
            Objects.requireNonNull(context);
            if (context instanceof VaadinService) {
                return ((VaadinService) context).getClassLoader()
                        .getResource(path);
            }
            return getResource(context.getClass(), path);
        }

        @Override
        public URL getClientResource(String path) {
            return getResource(ResourceProviderImpl.class, path);
        }

        @Override
        public InputStream getClientResourceAsStream(String path)
                throws IOException {
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
        OSGiAccess osgiAccess = OSGiAccess.getInstance();
        if (osgiAccess.getOsgiServletContext() == null) {
            initStandardLookup(classSet, servletContext);
        } else {
            VaadinServletContext vaadinContext = new VaadinServletContext(
                    servletContext);
            vaadinContext.setAttribute(Lookup.class, new OsgiLookupImpl());
        }
    }

    private void initStandardLookup(Set<Class<?>> classSet,
            ServletContext servletContext) {
        VaadinServletContext vaadinContext = new VaadinServletContext(
                servletContext);

        Map<Class<?>, Collection<Object>> services = new HashMap<>();

        collectResourceProviders(classSet, services);
        collectInstantiatorFactories(classSet, services);

        LookupImpl lookup = new LookupImpl(services);
        vaadinContext.setAttribute(Lookup.class, lookup);
    }

    private void collectInstantiatorFactories(Set<Class<?>> classSet,
            Map<Class<?>, Collection<Object>> services) {
        services.put(InstantiatorFactory.class,
                filterInstantiatorFactories(classSet).stream()
                        .map(ReflectTools::createInstance)
                        .collect(Collectors.toList()));
    }

    private void collectResourceProviders(Set<Class<?>> classSet,
            Map<Class<?>, Collection<Object>> services) {
        Set<Class<?>> providers = filterResourceProviders(classSet);
        if (classSet.isEmpty()) {
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
        return classes == null ? Collections.emptySet()
                : classes.stream()
                        .filter(ResourceProvider.class::isAssignableFrom)
                        .filter(clazz -> !ResourceProvider.class.equals(clazz)
                                && !ResourceProviderImpl.class.equals(clazz))
                        .collect(Collectors.toSet());
    }

    private Set<Class<?>> filterInstantiatorFactories(Set<Class<?>> classes) {
        return classes == null ? Collections.emptySet()
                : classes.stream()
                        .filter(InstantiatorFactory.class::isAssignableFrom)
                        .filter(clazz -> !InstantiatorFactory.class
                                .equals(clazz))
                        .collect(Collectors.toSet());
    }
}
