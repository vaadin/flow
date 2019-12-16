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
package com.vaadin.flow.server.startup;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.vaadin.flow.component.page.BodySize;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.VaadinAppShell;
import com.vaadin.flow.server.InvalidApplicationConfigurationException;
import com.vaadin.flow.server.VaadinContext;

import static com.vaadin.flow.server.startup.VaadinAppShellInitializer.getValidAnnotations;

/**
 * The registry class for {@link VaadinAppShell} annotations.
 *
 * @since 3.0
 *
 */
public class VaadinAppShellRegistry implements Serializable {


    static final String ERROR_HEADER_NO_SHELL = "%n%nFound app shell configuration annotations in non `VaadinAppShell` classes."
            + "%nPlease create a custom class extending `VaadinAppShell` and move the following annotations to it:%n  %s%n";

    static final String ERROR_HEADER_OFFENDING = "%n%nFound app shell configuration annotations in non `VaadinAppShell` classes."
            + "%nThe following annotations must be either removed or moved to the '%s' class:%n  %s%n";

    private static final String ERROR_LINE = "  - %s from %s";
    private static final String ERROR_MULTIPLE_SHELL =
            "%nUnable to find a single class extending `VaadinAppShell` from the following candidates:%n  %s%n  %s%n";

    private static final String ERROR_MULTIPLE_BODYSIZE =
            "%nBodySize is not a repeatable annotation type.%n";


    private Class<? extends VaadinAppShell> shell;

    /**
     * A wrapper class for storing the {@link VaadinAppShellRegistry} instance
     * in the servlet context.
     */
    public static class VaadinAppShellRegistryWrapper implements Serializable {
        private final VaadinAppShellRegistry registry;

        /**
         * Construct a wraper for an app-shell registry.
         *
         * @param registry the app shell registry
         */
        public VaadinAppShellRegistryWrapper(VaadinAppShellRegistry registry) {
            this.registry = registry;
        }
    }

    /**
     * Returns the instance of the registry, or create a new one if it does not
     * exist yet.
     *
     * @param context
     *            servlet context
     * @return the registry instance
     */
    @SuppressWarnings("unchecked")
    public static VaadinAppShellRegistry getInstance(VaadinContext context) {
        synchronized (context) { // NOSONAR
            VaadinAppShellRegistryWrapper attribute = context
                    .getAttribute(VaadinAppShellRegistryWrapper.class);
            if (attribute == null) {
                attribute = new VaadinAppShellRegistryWrapper(
                        new VaadinAppShellRegistry());
                context.setAttribute(attribute);
            }
            return attribute.registry;
        }
    }

    /**
     * Reset the registry configuration so as it's possible to perform a new
     * configuration and validation.
     */
    public void reset() {
        this.shell = null;
    }

    /**
     * Sets the {@link VaadinAppShell} class in the application. Pass a null to
     * reset the previous one when reusing the instance.
     *
     * @param shell
     *            the class extending VaadinAppShell class.
     */
    public void setShell(
            Class<? extends VaadinAppShell> shell) {
        if (this.shell != null && shell != null) {
            throw new InvalidApplicationConfigurationException(
                    String.format(VaadinAppShellRegistry.ERROR_MULTIPLE_SHELL,
                            this.shell.getName(), shell.getName()));
        }
        this.shell = shell;
    }

    /**
     * Returns the {@link VaadinAppShell} class in the application.
     *
     * @return
     */
    public Class<? extends VaadinAppShell> getShell() {
        return shell;
    }

    /**
     * Checks whether the class is extending {@link VaadinAppShell}.
     *
     * @param clz
     *            the class to check.
     * @return true if the class extends {@link VaadinAppShell}.
     */
    public boolean isShell(Class<?> clz) {
        assert clz != null;
        try {
            // Use the same class-loader for the checking
            return clz.getClassLoader()
                    .loadClass(VaadinAppShell.class.getName())
                    .isAssignableFrom(clz);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException(e);
        }
    }

    /**
     * Checks whether a class have annotations that should only be in
     * {@link VaadinAppShell} classes.
     *
     * @param clz
     *            the class to check.
     * @return a string with the error lines if the class has offending
     *         annotations
     */
    public String validateClass(Class<?> clz) {
        String error = null;
        @SuppressWarnings({ "unchecked", "rawtypes" })
        String annotations = AbstractAnnotationValidator
                .getClassAnnotations(clz, (List) getValidAnnotations());
        if (!annotations.isEmpty()) {
            error = String.format(VaadinAppShellRegistry.ERROR_LINE,
                    annotations, clz.getName());
        }
        return error;
    }

    /**
     * Modifies the `index.html` document based on the {@link VaadinAppShell}
     * annotations.
     *g
     * @param document a JSoup document for the index.html page
     */
    public void modifyIndexHtmlResponse(Document document) {
        getAnnotations(Meta.class).forEach(meta -> {
            Element elem = new Element("meta");
            elem.attr("name", meta.name());
            elem.attr("content", meta.content());
            document.head().appendChild(elem);
        });

        if(getAnnotations(BodySize.class).size() > 1) {
            throw new InvalidApplicationConfigurationException(
                    VaadinAppShellRegistry.ERROR_MULTIPLE_BODYSIZE);
        } else if(!getAnnotations(BodySize.class).isEmpty()) {
            String strBodySizeHeight = "height:" + getAnnotations(BodySize.class).get(0).height();
            String strBodySizeWidth = "width:" + getAnnotations(BodySize.class).get(0).width();
            Element elemStyle = new Element("style");
            elemStyle.attr("type", "text/css");
            String strContent = "body,#outlet{" + strBodySizeHeight + ";" + strBodySizeWidth + ";" + "}";
            elemStyle.append(strContent);
            document.head().appendChild(elemStyle);
        }
    }

    @Override
    public String toString() {
        return "Shell: " + shell + " metas: " + getAnnotations(Meta.class);
    }

    private <T extends Annotation> List<T> getAnnotations(Class<T> annotation) {
        assert getValidAnnotations().contains(annotation);
        return shell == null ? Collections.emptyList()
                : Arrays.asList(shell.getAnnotationsByType(annotation));
    }
}
