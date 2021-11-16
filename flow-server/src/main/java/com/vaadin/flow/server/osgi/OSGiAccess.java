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
package com.vaadin.flow.server.osgi;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.HandlesTypes;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.googlecode.gentyref.GenericTypeReflector;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType.Builder;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.internal.AnnotationReader;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.UsageStatistics;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.server.InitParameters;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.startup.ClassLoaderAwareServletContainerInitializer;
import com.vaadin.flow.server.startup.DevModeInitializer;
import com.vaadin.flow.server.startup.LookupServletContainerInitializer;

/**
 * Manages scanned classes inside OSGi container.
 * <p>
 * It doesn't do anything outside of OSGi.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 *
 * @author Vaadin Ltd
 * @since 1.2
 *
 * @see #getInstance()
 */
public final class OSGiAccess {
    private static final OSGiAccess INSTANCE = new OSGiAccess();

    private final ServletContext context = LazyOSGiDetector.IS_IN_OSGI
            ? createOSGiServletContext() : null;

    private final AtomicReference<Collection<Class<? extends ServletContainerInitializer>>> initializerClasses = LazyOSGiDetector.IS_IN_OSGI
            ? new AtomicReference<>() : null;

    private final Map<Long, Collection<Class<?>>> cachedClasses = LazyOSGiDetector.IS_IN_OSGI
            ? new ConcurrentHashMap<>() : null;

    private static final ResourceProvider RESOURCE_PROVIDER = new OSGiResourceProvider();

    private OSGiAccess() {
        // The class is a singleton. Avoid instantiation outside of the class.
    }

    /**
     * OSGi capable implementation of {@link ResourceProvider}.
     *
     */
    private static class OSGiResourceProvider implements ResourceProvider {

        @Override
        public List<URL> getApplicationResources(VaadinContext context,
                String path) throws IOException {
            if (context instanceof VaadinService) {
                return Collections.list(((VaadinService) context)
                        .getClassLoader().getResources(path));
            }
            return Collections.list(
                    context.getClass().getClassLoader().getResources(path));
        }

        @Override
        public URL getApplicationResource(VaadinContext context, String path) {
            Objects.requireNonNull(context);
            if (context instanceof VaadinServletContext) {
                return ((VaadinServletContext) context).getContext()
                        .getClassLoader().getResource(path);
            }
            return null;
        }

        @Override
        public URL getClientResource(String path) {
            Bundle[] bundles = FrameworkUtil
                    .getBundle(OSGiResourceProvider.class).getBundleContext()
                    .getBundles();
            for (Bundle bundle : bundles) {
                if ("com.vaadin.flow.client".equals(bundle.getSymbolicName())) {
                    return bundle.getResource(path);
                }
            }
            return null;
        }

        @Override
        public InputStream getClientResourceAsStream(String path)
                throws IOException {
            // No any caching !: flow-client may be reinstalled at any moment
            return getClientResource(path).openStream();
        }

    }

    private static class OsgiLookupImpl implements Lookup {

        @Override
        public <T> T lookup(Class<T> serviceClass) {
            if (ResourceProvider.class.equals(serviceClass)) {
                return serviceClass.cast(RESOURCE_PROVIDER);
            }
            Bundle bundle = FrameworkUtil.getBundle(OSGiAccess.class);
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
            Bundle bundle = FrameworkUtil.getBundle(OSGiAccess.class);
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
                LoggerFactory.getLogger(OsgiLookupImpl.class)
                        .error("Unexpected invalid filter expression", e);
                assert false : "Implementation error: Unexpected invalid filter exception is "
                        + "thrown even though the service filter is null. Check the exception and update the impl";
            }

            return Collections.emptyList();
        }

    }

    /**
     * This is internal class and is not intended to be used.
     * <p>
     * It's public only because it needs to be proxied.
     * <p>
     * This class represents a singleton servlet context instance which is not a
     * real servlet context.
     */
    public abstract static class OSGiServletContext implements ServletContext {

        private final Map<String, Object> attributes = new ConcurrentHashMap<>();

        @Override
        public void setAttribute(String name, Object object) {
            attributes.put(name, object);
        }

        @Override
        public Object getAttribute(String name) {
            return attributes.get(name);
        }

        @Override
        public void removeAttribute(String name) {
            attributes.remove(name);
        }

        @Override
        public Enumeration<String> getAttributeNames() {
            // Attributes are transfered from a fake context to the real context
            // using this method, only Lookup may be transfered since fake
            // context contains global data like registry which would have been
            // shared between all the contexts being transfered
            return Collections.enumeration(
                    Collections.singletonList(Lookup.class.getName()));
        }

        @Override
        public void log(String msg) {
            // This method is used by Atmosphere initiailizer
            LoggerFactory.getLogger(OSGiAccess.class).warn(msg);
        }

        @Override
        public String getInitParameter(String name) {
            // OSGi is supported in compatibiity mode only. So set it by default
            // for every ServletContainerInitializer
            if (InitParameters.SERVLET_PARAMETER_COMPATIBILITY_MODE
                    .equals(name)) {
                return Boolean.TRUE.toString();
            }
            return null;
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return Collections.enumeration(Collections.singletonList(
                    InitParameters.SERVLET_PARAMETER_COMPATIBILITY_MODE));
        }

        @Override
        public Map<String, ? extends ServletRegistration> getServletRegistrations() {
            return Collections.emptyMap();
        }

    }

    /**
     * Gets the singleton instance.
     *
     * @return the singleton instance
     */
    public static OSGiAccess getInstance() {
        return INSTANCE;
    }

    /**
     * Gets a servlet context instance which is used to track registries which
     * are storage of scanned classes.
     * <p>
     * This is not a real servlet context. It's just a proxied unique instance
     * which is used to be able to access registries in a generic way via some
     * {@code getInstance(ServletContext)} method.
     *
     * @return
     */
    public ServletContext getOsgiServletContext() {
        return context;
    }

    /**
     * Sets the discovered servlet context initializer classes.
     * <p>
     * The OSGi bundle tracker is used to scan all classes in bundles and it
     * also scans <b>flow-server</b> module for servlet initializer classes.
     * They are set using this method once they are collected.
     *
     * @param contextInitializers
     *            servlet context initializer classes
     */
    public void setServletContainerInitializers(
            Collection<Class<? extends ServletContainerInitializer>> contextInitializers) {
        assert contextInitializers != null;
        initializerClasses.set(new ArrayList<>(contextInitializers));
    }

    /**
     * Checks whether the servlet initializers are discovered.
     *
     * @return {@code true} if servlet initializers are set, {@code false}
     *         otherwise
     */
    public boolean hasInitializers() {
        return initializerClasses.get() != null;
    }

    /**
     * Adds scanned classes in active bundles.
     * <p>
     * The map contains a bundle id as a key and classes discovered in the
     * bundle as a value.
     *
     * @param extenderClasses
     *            a map with discovered classes in active bundles
     */
    public void addScannedClasses(
            Map<Long, Collection<Class<?>>> extenderClasses) {
        cachedClasses.putAll(extenderClasses);
        resetContextInitializers();
    }

    /**
     * Removes classes from the bundle identified by the {@code bundleId}.
     * <p>
     * When a bundle becomes inactive its classes should not be used anymore.
     * This method removes the classes from the bundle from the collection of
     * discovered classes.
     *
     * @param bundleId
     *            the bundle identifier
     */
    public void removeScannedClasses(Long bundleId) {
        cachedClasses.remove(bundleId);
        resetContextInitializers();
    }

    private void resetContextInitializers() {
        /*
         * exclude dev mode initializer (at least for now) because it doesn't
         * work in its current state anyway (so it's no-op) but its initial
         * calls breaks assumptions about Servlet registration in OSGi.
         * 
         * Lookup is set immediately in the context, so no need to initialize it
         */
        initializerClasses.get().stream().filter(
                clazz -> !clazz.equals(DevModeInitializer.class) && !clazz
                        .equals(LookupServletContainerInitializer.class))
                .map(ReflectTools::createInstance).forEach(this::handleTypes);
    }

    private void handleTypes(ServletContainerInitializer initializer) {
        Optional<HandlesTypes> handleTypes = AnnotationReader
                .getAnnotationFor(initializer.getClass(), HandlesTypes.class);
        /*
         * Every initializer should be an instance of
         * ClassLoaderAwareServletContainerInitializer : there is a test which
         * forces this. So assert should be enough here.
         */
        assert initializer instanceof ClassLoaderAwareServletContainerInitializer;
        try {
            // don't use onStartup method because a fake servlet context is
            // passed here: no need to detect classloaders in OSGi case
            ((ClassLoaderAwareServletContainerInitializer) initializer).process(
                    filterClasses(handleTypes.orElse(null)),
                    getOsgiServletContext());
        } catch (ServletException e) {
            throw new RuntimeException(
                    "Couldn't run servlet context initializer "
                            + initializer.getClass(),
                    e);
        }
    }

    @SuppressWarnings("unchecked")
    private Set<Class<?>> filterClasses(HandlesTypes typesAnnotation) {
        Set<Class<?>> result = new HashSet<>();
        if (typesAnnotation == null) {
            cachedClasses.forEach((bundle, classes) -> result.addAll(classes));
        } else {
            Class<?>[] requestedTypes = typesAnnotation.value();

            Predicate<Class<?>> isAnnotation = Class::isAnnotation;

            List<Class<? extends Annotation>> annotations = Stream
                    .of(requestedTypes).filter(isAnnotation)
                    .map(clazz -> (Class<? extends Annotation>) clazz)
                    .collect(Collectors.toList());

            List<Class<?>> superTypes = Stream.of(requestedTypes)
                    .filter(isAnnotation.negate()).collect(Collectors.toList());

            Predicate<Class<?>> hasType = clazz -> annotations.stream()
                    .anyMatch(annotation -> AnnotationReader
                            .getAnnotationFor(clazz, annotation).isPresent())
                    || superTypes.stream()
                            .anyMatch(superType -> GenericTypeReflector
                                    .isSuperType(HasErrorParameter.class,
                                            clazz));

            cachedClasses.forEach((bundle, classes) -> result.addAll(classes
                    .stream().filter(hasType).collect(Collectors.toList())));

        }
        return result;
    }

    private ServletContext createOSGiServletContext() {
        Builder<OSGiServletContext> builder = new ByteBuddy()
                .subclass(OSGiServletContext.class);

        Class<? extends OSGiServletContext> osgiServletContextClass = builder
                .make().load(OSGiServletContext.class.getClassLoader(),
                        ClassLoadingStrategy.Default.WRAPPER)
                .getLoaded();

        OSGiServletContext osgiContext = ReflectTools.createProxyInstance(
                osgiServletContextClass, ServletContext.class);

        new VaadinServletContext(osgiContext).setAttribute(Lookup.class,
                new OsgiLookupImpl());

        return osgiContext;
    }

    private static final class LazyOSGiDetector {
        private static final boolean IS_IN_OSGI = isInOSGi();

        private static boolean isInOSGi() {
            try {
                Class<?> clazz = Class
                        .forName("org.osgi.framework.FrameworkUtil");

                Method method = clazz.getDeclaredMethod("getBundle",
                        Class.class);

                // even though the FrameworkUtil class is in the classpath it
                // may be there not because of OSGi container but plain WAR with
                // jar which contains the class
                if (method.invoke(null, OSGiAccess.class) == null) {
                    return false;
                }
                UsageStatistics.markAsUsed("flow/osgi", getOSGiVersion());

                return true;
            } catch (ClassNotFoundException | NoSuchMethodException
                    | SecurityException | IllegalAccessException
                    | IllegalArgumentException
                    | InvocationTargetException exception) {
                if (LoggerFactory.getLogger(OSGiAccess.class)
                        .isTraceEnabled()) {
                    LoggerFactory.getLogger(OSGiAccess.class)
                            .trace("Exception in OSGi container check "
                                    + "(which most likely means that this is not OSGi container)",
                                    exception);
                }
                return false;
            }
        }

        /**
         * Tries to detect the version of the OSGi framework used.
         *
         * @return the used OSGi version or {@code null} if not able to detect
         *         it
         */
        private static String getOSGiVersion() {
            try {
                Bundle osgiBundle = org.osgi.framework.FrameworkUtil
                        .getBundle(Bundle.class);
                return osgiBundle.getVersion().toString();
            } catch (Throwable throwable) {
                // just eat it so that any failure in the version detection
                // doesn't break OSGi usage
                LoggerFactory.getLogger(OSGiAccess.class)
                        .info("Unable to detect used OSGi framework version due to "
                                + throwable.getMessage());
            }
            return null;
        }

    }
}
