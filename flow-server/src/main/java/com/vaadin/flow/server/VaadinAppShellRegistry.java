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
package com.vaadin.flow.server;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;

import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Inline.Position;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.TargetElement;
import com.vaadin.flow.component.page.VaadinAppShell;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.internal.UrlUtil;
import com.vaadin.flow.server.InlineTargets;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.VaadinAppShellSettings;
import com.vaadin.flow.server.InvalidApplicationConfigurationException;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.ui.Dependency;

import elemental.json.JsonObject;
import com.vaadin.flow.server.startup.AbstractAnnotationValidator;

import static com.vaadin.flow.server.startup.VaadinAppShellInitializer.getValidAnnotations;

/**
 * The registry class for {@link VaadinAppShell} annotations.
 *
 * @since 3.0
 *
 */
public class VaadinAppShellRegistry implements Serializable {

    public static final String ERROR_HEADER_NO_SHELL =
            "%n%nFound app shell configuration annotations in non `VaadinAppShell` classes."
            + "%nPlease create a custom class implementing `VaadinAppShell` and move the following annotations to it:%n  %s%n";

    public static final String ERROR_HEADER_OFFENDING =
            "%n%nFound app shell configuration annotations in non `VaadinAppShell` classes."
            + "%nThe following annotations must be either removed or moved to the '%s' class:%n  %s%n";

    public static final String ERROR_HEADER_NO_APP_CONFIGURATOR =
            "%n%nThe `PageConfigurator` interface is deprecated since Vaadin 15 and has no effect."
            + "%nPlease, create a class implementing `VaadinAppShell`, and remove `PageConfigurator` from: %n  - %s%n";

    public static final String ERROR_HEADER_OFFENDING_CONFIGURATOR =
            "%n%nThe `PageConfigurator` interface is deprecated since Vaadin 15 and has no effect."
            + "%nPlease, configure the page in %s, and remove the `PageConfigurator` from: %n - %s%n";

    private static final String ERROR_LINE = "  - %s from %s";
    private static final String ERROR_MULTIPLE_SHELL =
            "%nUnable to find a single class implementing `VaadinAppShell` from the following candidates:%n  %s%n  %s%n";

    private static final String ERROR_MULTIPLE_ANNOTATION =
            "%n%s is not a repeatable annotation type.%n";

    // There must be no more than one of the following elements per document
    private static final String[] UNIQUE_ELEMENTS = { "meta[name=viewport]",
            "meta[name=charset]", "meta[name=description]", "title", "base" };

    private Class<? extends VaadinAppShell> shell;
    private VaadinAppShell appShell;

    /**
     * A wrapper class for storing the {@link VaadinAppShellRegistry} instance
     * in the servlet context.
     */
    public static class VaadinAppShellRegistryWrapper implements Serializable {
        private final VaadinAppShellRegistry registry;

        /**
         * Construct a wraper for an app-shell registry.
         *
         * @param registry
         *            the app shell registry
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
        this.appShell = null;
    }

    /**
     * Sets the {@link VaadinAppShell} class in the application. Pass a null to
     * reset the previous one when reusing the instance.
     *
     * @param shell
     *            the class extending VaadinAppShell class.
     */
    public void setShell(Class<? extends VaadinAppShell> shell) {
        if (this.shell != null && shell != null) {
            throw new InvalidApplicationConfigurationException(
                    String.format(VaadinAppShellRegistry.ERROR_MULTIPLE_SHELL,
                            this.shell.getName(), shell.getName()));
        }
        this.shell = shell;
        this.appShell = ReflectTools.createInstance(shell);

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

    private VaadinAppShellSettings createSettings() {
        VaadinAppShellSettings settings = new VaadinAppShellSettings();

        getAnnotations(Meta.class).forEach(meta -> {
            settings.addMetaTag(meta.name(), meta.content());
        });
        List<Viewport> viewPorts = getAnnotations(Viewport.class);
        if(viewPorts.size() > 1) {
            throw new InvalidApplicationConfigurationException(
                    String.format(VaadinAppShellRegistry.ERROR_MULTIPLE_ANNOTATION, Viewport.class.getSimpleName()));
        } else if(!viewPorts.isEmpty()) {
            settings.setViewport(viewPorts.get(0).value());
        }
        List<BodySize> bodySizes = getAnnotations(BodySize.class);
        if(bodySizes.size() > 1) {
            throw new InvalidApplicationConfigurationException(
                    String.format(VaadinAppShellRegistry.ERROR_MULTIPLE_ANNOTATION, BodySize.class.getSimpleName()));
        } else if(!bodySizes.isEmpty()) {
            settings.setBodySize(bodySizes.get(0).width(),
                    bodySizes.get(0).height());
        }
        List<PageTitle> pageTitles = getAnnotations(PageTitle.class);
        if(pageTitles.size() > 1) {
            throw new InvalidApplicationConfigurationException(
                    String.format(VaadinAppShellRegistry.ERROR_MULTIPLE_ANNOTATION, PageTitle.class.getSimpleName()));
        } else if(!pageTitles.isEmpty()) {
            settings.setPageTitle(pageTitles.get(0).value());
        }
        getAnnotations(Inline.class).forEach(inline -> {
            settings.addInline(inline);
        });
        return settings;
    }

    /**
     * Modifies the `index.html` document based on the {@link VaadinAppShell}
     * annotations or {@link VaadinAppShell#configurePage(VaadinAppShellSettings)} method.
     *
     * @param document
     *            a JSoup document for the index.html page
     * @param session
     *            The session for the request
     * @param request
     *            The request to handle
     */
    public void modifyIndexHtml(Document document,
            VaadinSession session, VaadinRequest request) {

        VaadinAppShellSettings settings = createSettings();
        if (appShell != null) {
            appShell.configurePage(settings);
        }

        settings.getHeadElements(Position.PREPEND).forEach(
                elm -> insertElement(elm, document.head()::prependChild));
        settings.getHeadElements(Position.APPEND).forEach(
                elm -> insertElement(elm, document.head()::appendChild));

        settings.getInlineElements(request, TargetElement.HEAD,
                Position.PREPEND).stream()
                .forEach(elm -> insertInlineElement(elm,
                        document.head()::prependChild));
        settings.getInlineElements(request, TargetElement.HEAD, Position.APPEND)
                .stream().forEach(elm -> insertInlineElement(elm,
                        document.head()::appendChild));
        settings.getInlineElements(request, TargetElement.BODY,
                Position.PREPEND).stream()
                .forEach(elm -> insertInlineElement(elm,
                        document.body()::prependChild));
        settings.getInlineElements(request, TargetElement.BODY, Position.APPEND)
                .stream().forEach(elm -> insertInlineElement(elm,
                        document.body()::appendChild));
    }

    private void insertElement(Element elm, Consumer<Element> action) {
        action.accept(elm);
        for (String cssQuery : UNIQUE_ELEMENTS) {
            if (elm.is(cssQuery)) {
                Element first = elm.parent().selectFirst(cssQuery);
                if (first != elm && first != null) {
                    first.replaceWith(elm);
                }
                break;
            }
        }
    }

    private void insertInlineElement(Element elm, Consumer<Element> action) {
        if (elm instanceof Document) {
            elm.getAllElements().stream()
                    .filter(item -> !(item instanceof Document)
                            && elm.equals(item.parent()))
                    .forEach(action::accept);
        } else if (elm != null) {
            action.accept(elm);
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
