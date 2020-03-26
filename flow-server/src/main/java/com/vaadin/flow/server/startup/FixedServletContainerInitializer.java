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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.stream.Stream;

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
public interface FixedServletContainerInitializer
        extends ServletContainerInitializer {

    @Override
    public default void onStartup(Set<Class<?>> set, ServletContext ctx)
            throws ServletException {
        ClassLoader webClassLoader = ctx.getClassLoader();
        ClassLoader classLoader = getClass().getClassLoader();

        /*
         * Hack is needed to make a workaround for weird behavior of WildFly
         * with skinnywar See https://github.com/vaadin/flow/issues/7805
         */
        boolean noHack = false;
        while (webClassLoader != null) {
            if (webClassLoader.equals(classLoader)) {
                noHack = true;
                break;
            } else {
                webClassLoader = webClassLoader.getParent();
            }
        }
        if (noHack) {
            process(set, ctx);
            return;
        }

        try {
            Class<?> initializer = ctx.getClassLoader()
                    .loadClass(getClass().getName());

            String processName = Stream
                    .of(FixedServletContainerInitializer.class
                            .getDeclaredMethods())
                    .filter(method -> !method.isDefault()
                            && !method.isSynthetic())
                    .findFirst().get().getName();
            Method operation = Stream.of(initializer.getDeclaredMethods())
                    .filter(method -> method.getName().equals(processName))
                    .findFirst().get();
            operation.invoke(initializer.newInstance(),
                    new Object[] { set, ctx });
        } catch (ClassNotFoundException | IllegalAccessException
                | IllegalArgumentException | InvocationTargetException
                | InstantiationException e) {
            throw new ServletException(e);
        }
    }

    /**
     * Implement this method instead of {@link #onStartup(Set, ServletContext)}.
     *
     * @param set
     *            the Set of application classes that extend, implement, or have
     *            been annotated with the class types specified by the
     *            {@link javax.servlet.annotation.HandlesTypes HandlesTypes}
     *            annotation, or <tt>null</tt> if there are no matches, or this
     *            <tt>ServletContainerInitializer</tt> has not been annotated
     *            with <tt>HandlesTypes</tt>
     *
     * @param ctx
     *            the <tt>ServletContext</tt> of the web application that is
     *            being started and in which the classes contained in <tt>c</tt>
     *            were found
     *
     * @throws ServletException
     *             if an error has occurred
     *
     * @see #onStartup(Set, ServletContext)
     */
    public void process(Set<Class<?>> set, ServletContext ctx)
            throws ServletException;
}
