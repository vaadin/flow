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
import java.util.Optional;
import java.util.function.Consumer;

import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.UrlUtil;
import com.vaadin.flow.server.*;
import com.vaadin.flow.shared.ui.Dependency;
import elemental.json.JsonObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.DataNode;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.VaadinAppShell;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.server.InvalidApplicationConfigurationException;
import com.vaadin.flow.server.VaadinContext;
import org.jsoup.parser.Parser;
import org.jsoup.parser.Tag;

import static com.vaadin.flow.server.startup.VaadinAppShellInitializer.getValidAnnotations;

/**
 * The registry class for {@link VaadinAppShell} annotations.
 *
 * @since 3.0
 *
 */
public class VaadinAppShellRegistry implements Serializable {

    static final String ERROR_HEADER_NO_SHELL =
            "%n%nFound app shell configuration annotations in non `VaadinAppShell` classes."
            + "%nPlease create a custom class implementing `VaadinAppShell` and move the following annotations to it:%n  %s%n";

    static final String ERROR_HEADER_OFFENDING =
            "%n%nFound app shell configuration annotations in non `VaadinAppShell` classes."
            + "%nThe following annotations must be either removed or moved to the '%s' class:%n  %s%n";

    static final String ERROR_HEADER_NO_APP_CONFIGURATOR =
            "%n%nThe `PageConfigurator` interface is deprecated since Vaadin 15 and has no effect."
            + "%nPlease, create a class implementing `VaadinAppShell`, and remove `PageConfigurator` from: %n  - %s%n";

    static final String ERROR_HEADER_OFFENDING_CONFIGURATOR =
            "%n%nThe `PageConfigurator` interface is deprecated since Vaadin 15 and has no effect."
            + "%nPlease, configure the page in %s, and remove the `PageConfigurator` from: %n - %s%n";

    private static final String ERROR_LINE = "  - %s from %s";
    private static final String ERROR_MULTIPLE_SHELL =
            "%nUnable to find a single class implementing `VaadinAppShell` from the following candidates:%n  %s%n  %s%n";

    private static final String ERROR_MULTIPLE_VIEWPORT =
            "%nViewport is not a repeatable annotation type.%n";
    private static final String CSS_TYPE_ATTRIBUTE_VALUE = "text/css";
    private static final String SCRIPT_TAG = "script";
    private static final String DEFER_ATTRIBUTE = "defer";
    private static final String TYPE = "type";

    private static final String ERROR_MULTIPLE_BODYSIZE =
            "%nBodySize is not a repeatable annotation type.%n";

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
        this.appShell = null;
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

    /**
     * Modifies the `index.html` document based on the {@link VaadinAppShell}
     * annotations.
     *
     * @param document a JSoup document for the index.html page
     */
    public void modifyIndexHtmlResponse(Document document) {
        getAnnotations(Meta.class).forEach(meta -> {
            Element elem = new Element("meta");
            elem.attr("name", meta.name());
            elem.attr("content", meta.content());
            document.head().appendChild(elem);
        });

        if(getAnnotations(Viewport.class).size() > 1) {
            throw new InvalidApplicationConfigurationException(
                    VaadinAppShellRegistry.ERROR_MULTIPLE_VIEWPORT);
        } else if(!getAnnotations(Viewport.class).isEmpty()) {
            Element metaViewportElement = document.head().selectFirst("meta[name=viewport]");
            if (metaViewportElement == null) {
                metaViewportElement = new Element("meta");
                metaViewportElement.attr("name", "viewport");
                document.head().appendChild(metaViewportElement);
            }
            metaViewportElement.attr("content", getAnnotations(Viewport.class).get(0).value());
        }

        if(getAnnotations(BodySize.class).size() > 1) {
            throw new InvalidApplicationConfigurationException(
                    VaadinAppShellRegistry.ERROR_MULTIPLE_BODYSIZE);
        } else if(!getAnnotations(BodySize.class).isEmpty()) {
            String strBodySizeHeight = "height:" + getAnnotations(BodySize.class).get(0).height();
            String strBodySizeWidth = "width:" + getAnnotations(BodySize.class).get(0).width();
            Element elemStyle = new Element("style");
            elemStyle.attr(TYPE, CSS_TYPE_ATTRIBUTE_VALUE);
            String strContent = "body,#outlet{" + strBodySizeHeight + ";" + strBodySizeWidth + ";" + "}";
            elemStyle.append(strContent);
            document.head().appendChild(elemStyle);
        }
    }

    /**
     * Modifies the `index.html` document based on the {@link VaadinAppShell}
     * annotations.
     *
     * @param document a JSoup document for the index.html page
     * @param session
     *            The session for the request
     * @param request
     *            The request to handle
     */
    public void modifyIndexHtmlResponeWithInline(Document document, VaadinSession session, VaadinRequest request) {
        getInlineTargets(request).ifPresent(targets -> handleInlineTargets(session, request, document.head(), document.body(), targets));
    }

    private Element createInlineDependencyElement(VaadinSession session, VaadinRequest request,
                                            JsonObject dependencyJson) {
        String type = dependencyJson.getString(Dependency.KEY_TYPE);
        if (Dependency.Type.contains(type)) {
            Dependency.Type dependencyType = Dependency.Type.valueOf(type);
            return createInlineDependencyElement(session, request, dependencyJson, dependencyType);
        }
        return Jsoup.parse(
                dependencyJson.getString(Dependency.KEY_CONTENTS), "",
                Parser.xmlParser());
    }

    /**
     * Return the {@link VaadinAppShell} used in the application.
     *
     * @return the instance
     */
    public Optional<VaadinAppShell> getAppShell() {
        return Optional.ofNullable(appShell);
    }

    private Element createInlineDependencyElement(VaadinSession session, VaadinRequest request, JsonObject dependency,
                                            Dependency.Type type) {
        String url = dependency.hasKey(Dependency.KEY_URL)
                ? request.getService().resolveResource(
                        dependency.getString(Dependency.KEY_URL),
                        session.getBrowser())
                : null;

        final Element dependencyElement;
        switch (type) {
            case STYLESHEET:
                dependencyElement = createStylesheetElement(url);
                break;
            case JAVASCRIPT:
                dependencyElement = createJavaScriptElement(url, false, null);
                break;
            case JS_MODULE:
                if (url != null && UrlUtil.isExternal(url)) {
                    dependencyElement = createJavaScriptElement(url, false, "module");
                } else dependencyElement = null;
                break;
            default:
                throw new IllegalStateException(
                        "Unsupported dependency type: " + type);
        }

        if (dependencyElement != null) {
            dependencyElement.appendChild(new DataNode(
                    dependency.getString(Dependency.KEY_CONTENTS)));
        }

        return dependencyElement;
    }

    private Element createStylesheetElement(String url) {
        final Element cssElement;
        if (url != null) {
            cssElement = new Element(Tag.valueOf("link"), "")
                    .attr("rel", "stylesheet")
                    .attr(TYPE, CSS_TYPE_ATTRIBUTE_VALUE)
                    .attr("href", url);
        } else {
            cssElement = new Element(Tag.valueOf("style"), "").attr(TYPE,
                    CSS_TYPE_ATTRIBUTE_VALUE);
        }
        return cssElement;
    }

    private static Element createJavaScriptElement(String sourceUrl, boolean defer,
                                                   String type) {
        if (type == null) {
            type = "text/javascript";
        }

        Element jsElement = new Element(Tag.valueOf(SCRIPT_TAG), "")
                .attr(TYPE, type).attr(DEFER_ATTRIBUTE, defer);
        if (sourceUrl != null) {
            jsElement = jsElement.attr("src", sourceUrl);
        }
        return jsElement;
    }

    private void insertElements(Element element, Consumer<Element> action) {
        if (element instanceof Document) {
            element.getAllElements().stream()
                    .filter(item -> !(item instanceof Document)
                            && element.equals(item.parent()))
                    .forEach(action::accept);
        } else if (element != null) {
            action.accept(element);
        }
    }

    private void handleInlineTargets(VaadinSession session, VaadinRequest request, Element head, Element body, InlineTargets targets) {
        targets.getInlineHead(Inline.Position.PREPEND).stream().map(
                dependency -> createInlineDependencyElement(session, request, dependency))
                .forEach(element -> insertElements(element,
                        head::prependChild));
        targets.getInlineHead(Inline.Position.APPEND).stream().map(
                dependency -> createInlineDependencyElement(session, request, dependency))
                .forEach(element -> insertElements(element,
                        head::appendChild));

        targets.getInlineBody(Inline.Position.PREPEND).stream().map(
                dependency -> createInlineDependencyElement(session, request, dependency))
                .forEach(element -> insertElements(element,
                        body::prependChild));
        targets.getInlineBody(Inline.Position.APPEND).stream().map(
                dependency -> createInlineDependencyElement(session, request, dependency))
                .forEach(element -> insertElements(element,
                        body::appendChild));
    }

    private Optional<InlineTargets> getInlineTargets(
            VaadinRequest request) {
        List<Inline> inlineAnnotations = getAnnotations(Inline.class);

        if (inlineAnnotations.isEmpty()) {
            return Optional.empty();
        } else {
            InlineTargets inlines = new InlineTargets();
            inlineAnnotations.forEach(inline -> inlines
                    .addInlineDependency(inline, request));
            return Optional.of(inlines);
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
