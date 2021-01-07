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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.vaadin.flow.component.template.internal.DeprecatedPolymerPublishedEventHandler;
import com.vaadin.flow.di.InstantiatorFactory;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.LookupInitializer;
import com.vaadin.flow.di.ResourceProvider;
import com.vaadin.flow.function.VaadinApplicationInitializationBootstrap;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.server.VaadinContext;
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
        EndpointGeneratorTaskFactory.class,
        ApplicationConfigurationFactory.class, LookupInitializer.class })
public class LookupServletContainerInitializer
        implements ClassLoaderAwareServletContainerInitializer {

    @Override
    public void process(Set<Class<?>> classSet, ServletContext servletContext)
            throws ServletException {
        VaadinServletContext vaadinContext = new VaadinServletContext(
                servletContext);
        Map<Class<?>, Collection<Class<?>>> services = new HashMap<>();

        collectSubclasses(LookupInitializer.class, classSet, services);

        LookupInitializer initializer = getLookupInitializer(services);

        services.remove(LookupInitializer.class);

        collectServiceImplementations(classSet, services);

        initializer.initialize(vaadinContext, services, lookup -> {
            vaadinContext.setAttribute(Lookup.class, lookup);

            DeferredServletContextInitializers deferredInitializers;
            synchronized (servletContext) {
                deferredInitializers = vaadinContext
                        .getAttribute(DeferredServletContextInitializers.class);
                vaadinContext.removeAttribute(
                        DeferredServletContextInitializers.class);
            }

            if (deferredInitializers != null) {
                deferredInitializers.runInitializers(servletContext);
            }
        });
    }

    @Override
    public boolean requiresLookup() {
        return false;
    }

    /**
     * Gets the service types that are used to set services into the
     * {@link Lookup} based on found subtypes by the
     * {@link ServletContainerInitializer}.
     * <p>
     * {@link LookupServletContainerInitializer} uses
     * {@link ServletContainerInitializer} classes discovering mechanism based
     * on {@link HandlesTypes} annotation. The method may be overridden to
     * return the service types which should be put into the {@link Lookup}
     * instance if another mechanism of class searching is used (e.g. Spring
     * boot case).
     * <p>
     * The set of classes (passed into the {@link #process(Set, ServletContext)}
     * method) will be filtered via checking whether they are assignable to the
     * service types and the resulting classes will be instantiated via
     * reflection.
     * 
     * @return a collection of service types which should be available via
     *         Lookup
     * @see LookupInitializer#initialize(VaadinContext, Map,
     *      VaadinApplicationInitializationBootstrap)
     */
    protected Collection<Class<?>> getServiceTypes() {
        HandlesTypes annotation = getClass().getAnnotation(HandlesTypes.class);
        if (annotation == null) {
            throw new IllegalStateException(
                    "Cannot collect service types based on "
                            + HandlesTypes.class.getSimpleName()
                            + " annotation. The default 'getServiceTypes' method implementation can't be used.");
        }
        return Stream.of(annotation.value())
                .filter(clazz -> !clazz.equals(LookupInitializer.class))
                .collect(Collectors.toSet());
    }

    private void collectServiceImplementations(Set<Class<?>> classSet,
            Map<Class<?>, Collection<Class<?>>> services) {
        for (Class<?> serviceType : getServiceTypes()) {
            collectSubclasses(serviceType, classSet, services);
        }

    }

    private void collectSubclasses(Class<?> clazz, Set<Class<?>> classSet,
            Map<Class<?>, Collection<Class<?>>> services) {
        services.put(clazz, filterSubClasses(clazz, classSet).stream()
                .collect(Collectors.toList()));
    }

    private Set<Class<?>> filterSubClasses(Class<?> clazz,
            Set<Class<?>> classes) {
        return classes == null ? Collections.emptySet()
                : classes.stream().filter(clazz::isAssignableFrom)
                        .filter(ReflectTools::isInstantiableService)
                        .filter(cls -> !clazz.equals(cls))
                        .collect(Collectors.toSet());
    }

    private LookupInitializer getLookupInitializer(
            Map<Class<?>, Collection<Class<?>>> services)
            throws ServletException {
        Collection<Class<?>> initializers = services
                .remove(LookupInitializer.class);
        if (initializers == null) {
            initializers = Collections.emptyList();
        } else {
            initializers.remove(LookupInitializer.class);
        }

        LookupInitializer initializer;
        if (initializers.isEmpty()) {
            initializer = new LookupInitializer();
        } else if (initializers.size() > 1) {
            throw new ServletException("Several implementation of "
                    + LookupInitializer.class.getSimpleName()
                    + " are found in the claspath: " + initializers);
        } else {
            initializer = LookupInitializer.class.cast(ReflectTools
                    .createInstance(initializers.iterator().next()));
        }
        return initializer;
    }
}
