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

import javax.servlet.ServletContext;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.jsoup.nodes.Element;

import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.VaadinAppShell;
import com.vaadin.flow.server.InvalidApplicationConfigurationException;

import static com.vaadin.flow.server.startup.VaadinAppShellInitializer.getValidAnnotations;

/**
 * The registry class for {@link VaadinAppShell} annotations.
 *
 * @since 3.0
 *
 */
public class VaadinAppShellRegistry implements Serializable {


    static final String ERROR_HEADER = "%n%nFound configuration annotations in non `VaadinApplicationShell` classes."
            + "%nThe following annotations must be moved to the '%s' class:%n%s%n";

    private static final String ERROR_LINE = "  - %s contains: %s";
    private static final String ERROR_MULTIPLE_SHELL = "%nFound multiple classes extending `VaadinApplicationShell` in the application%n  %s%n  %s%n";
    private static final String SHELL_KEY = VaadinAppShell.class.getName();

    private static VaadinAppShellRegistry instance;
    private Class<? extends VaadinAppShell> shell;

    /**
     * Returns the instance of the registry, or create a new one if it does not
     * exist yet.
     *
     * @param context
     *            servlet context
     * @return the registry instance
     */
    @SuppressWarnings("unchecked")
    public static VaadinAppShellRegistry getInstance(ServletContext context) {

        if (instance != null) {
            return instance;
        }
        assert context != null;

        synchronized (context) {
            instance = new VaadinAppShellRegistry();

            String shellName = (String) context.getAttribute(SHELL_KEY);
            if (shellName != null) {
                try {
                    instance.shell = (Class<? extends VaadinAppShell>) Class
                            .forName(shellName);
                } catch (ClassNotFoundException e) {
                    throw new IllegalArgumentException(e);
                }
            }
        }
        return instance;
    }

    /**
     * Sets the {@link VaadinAppShell} class in the application. Pass a null to
     * reset the previous one when reusing the instance.
     *
     * @param shell
     *            the class extending VaadinAppShell class.
     * @param context
     *            the servlet context.
     */
    public void setShell(
            Class<? extends VaadinAppShell> shell, ServletContext context) {
        if (this.shell != null && shell != null) {
            throw new InvalidApplicationConfigurationException(
                    String.format(VaadinAppShellRegistry.ERROR_MULTIPLE_SHELL,
                            this.shell, shell.getName()));
        }
        this.shell = shell;
        if (shell != null) {
            context.setAttribute(SHELL_KEY, shell.getName());
        } else {
            context.removeAttribute(SHELL_KEY);
        }
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
        // Compare string names in order to works when classloaders are different
        return clz.getSuperclass().getName()
                .equals(VaadinAppShell.class.getName()); // NOSONAR
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
        if (shell != null) {
            @SuppressWarnings({ "unchecked", "rawtypes" })
            String annotations = AbstractAnnotationValidator
                    .getClassAnnotations(clz, (List) getValidAnnotations());
            if (!annotations.isEmpty()) {
                error = String.format(VaadinAppShellRegistry.ERROR_LINE,
                        clz.getName(), annotations);
            }
        }
        return error;
    }

    /**
     * Gets the list of {@link Element} that should be appended to the document
     * head based on the {@link VaadinAppShell} annotations.
     *
     * @return
     */
    public List<Element> getElements() {
        List<Element> ret = new ArrayList<>();
        getAnnotations(Meta.class).forEach(meta -> {
            Element elem = new Element("meta");
            elem.attr("name", meta.name());
            elem.attr("content", meta.content());
            ret.add(elem);
        });
        return ret;
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
