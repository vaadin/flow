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
package com.vaadin.flow.server.startup;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Stream;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinServletContext;

/**
 * Allows to load the implementation class by one classloader but accepts
 * classes in {@link #onStartup(Set, ServletContext)} method loaded by another
 * classloader.
 * <p>
 * Workaround for https://github.com/vaadin/flow/issues/7805.
 *
 * @author Vaadin Ltd
 *
 */
public interface ClassLoaderAwareServletContainerInitializer
        extends ServletContainerInitializer {

    /**
     * Overridden to use different classloaders if needed.
     * <p>
     * {@inheritDoc}
     */
    @Override
    default void onStartup(Set<Class<?>> set, ServletContext context)
            throws ServletException {
        // see DeferredServletContextIntializers
        DeferredServletContextInitializers.Initializer deferredInitializer = ctx -> {
            ClassLoader webClassLoader = ctx.getClassLoader();
            ClassLoader classLoader = getClass().getClassLoader();

            /*
             * Hack is needed to make a workaround for weird behavior of WildFly
             * with skinnywar See https://github.com/vaadin/flow/issues/7805
             */
            boolean noHack = false;
            while (classLoader != null) {
                if (classLoader.equals(webClassLoader)) {
                    noHack = true;
                    break;
                } else {
                    /*
                     * The classloader which has loaded this class ({@code
                     * classLoader}) should be either the {@code webClassLoader}
                     * or its child: in this case it knows how to handle the
                     * classes loaded by the {@code webClassLoader} : it either
                     * is able to load them itself or delegate to its parent
                     * (which is the {@code webClassLoader}): in this case hack
                     * is not needed and the {@link #process(Set,
                     * ServletContext)} method can be called directly.
                     */
                    classLoader = classLoader.getParent();
                }
            }

            if (noHack) {
                process(set, ctx);
                return;
            }

            try {
                Class<?> initializer = ctx.getClassLoader()
                        .loadClass(getClass().getName());

                String processMethodName = Stream
                        .of(ClassLoaderAwareServletContainerInitializer.class
                                .getDeclaredMethods())
                        .filter(method -> !method.isDefault()
                                && !method.isSynthetic())
                        .findFirst().get().getName();
                Method operation = Stream.of(initializer.getMethods()).filter(
                        method -> method.getName().equals(processMethodName))
                        .findFirst().get();
                operation.invoke(initializer.newInstance(),
                        new Object[] { set, ctx });
            } catch (ClassNotFoundException | IllegalAccessException
                    | IllegalArgumentException | InvocationTargetException
                    | InstantiationException e) {
                throw new ServletException(e);
            }
        };

        if (requiresLookup()) {
            VaadinServletContext vaadinContext = new VaadinServletContext(
                    context);
            synchronized (context) {
                if (vaadinContext.getAttribute(Lookup.class) == null) {
                    DeferredServletContextInitializers initializers = vaadinContext
                            .getAttribute(
                                    DeferredServletContextInitializers.class,
                                    () -> new DeferredServletContextInitializers());
                    initializers.addInitializer(
                            ctx -> deferredInitializer.init(ctx));
                    return;
                }
            }
        }
        deferredInitializer.init(context);
    }

    /**
     * Whether this initializer requires lookup or not.
     *
     * @return whether this initializer requires lookup
     */
    default boolean requiresLookup() {
        return true;
    }

    /**
     * Implement this method instead of {@link #onStartup(Set, ServletContext)}
     * to handle classes accessible by different classloaders.
     *
     * @param classSet
     *            the Set of application classes that extend, implement, or have
     *            been annotated with the class types specified by the
     *            {@link jakarta.servlet.annotation.HandlesTypes HandlesTypes}
     *            annotation, or <code>null</code> if there are no matches, or
     *            this <code>ServletContainerInitializer</code> has not been
     *            annotated with <code>HandlesTypes</code>
     *
     * @param context
     *            the <code>ServletContext</code> of the web application that is
     *            being started and in which the classes contained in
     *            <code>classSet</code> were found
     *
     * @throws ServletException
     *             if an error has occurred
     *
     * @see #onStartup(Set, ServletContext)
     */
    void process(Set<Class<?>> classSet, ServletContext context)
            throws ServletException;
}
