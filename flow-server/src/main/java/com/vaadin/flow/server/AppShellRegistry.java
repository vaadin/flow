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
package com.vaadin.flow.server;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.PushConfiguration;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Inline.Position;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.TargetElement;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.function.DeploymentConfiguration;
import com.vaadin.flow.internal.ActiveStyleSheetTracker;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.shared.ApplicationConstants;
import com.vaadin.flow.theme.Theme;

import static com.vaadin.flow.server.startup.AbstractAnnotationValidator.getClassAnnotations;
import static com.vaadin.flow.server.startup.VaadinAppShellInitializer.getValidAnnotations;

/**
 * The registry class for {@link AppShellConfigurator} annotations.
 *
 * @since 3.0
 *
 */
public class AppShellRegistry implements Serializable {

    public static final String ERROR_HEADER_NO_SHELL = "%n%nFound app shell configuration annotations in non `AppShellConfigurator` classes."
            + "%nPlease create a custom class implementing `AppShellConfigurator` and move the following annotations to it:%n  %s%n";

    public static final String ERROR_HEADER_OFFENDING_PWA = "%n%nWe changed the way you configure PWAs, please see this link for more info:%n"
            + "https://vaadin.com/docs/latest/flow/advanced/modifying-the-bootstrap-page%n";

    private static final String ERROR_LINE = "  - %s from %s";
    private static final String ERROR_MULTIPLE_SHELL = "%n%nMultiple classes implementing `AppShellConfigurator` were found. However, only a single class implementing `AppShellConfigurator` is allowed."
            + "%nRemove \"implements AppShellConfigurator\" from all but one of the following classes:%n  %s%n  %s%n";

    private static final String ERROR_MULTIPLE_ANNOTATION = "%n%s is not a repeatable annotation type.%n";

    private static final String ERROR_EXTENDS_COMPONENT = "%nApp shell class is not allowed to extend Vaadin Component: %s. "
            + "App shells are only intended for page configuration and are instantiated before the UI is created.%n";

    // There must be no more than one of the following elements per document
    private static final String[] UNIQUE_ELEMENTS = { "meta[name=viewport]",
            "meta[name=description]", "title", "base" };
    private static final Logger log = LoggerFactory
            .getLogger(AppShellRegistry.class);

    private Class<? extends AppShellConfigurator> appShellClass;

    /**
     * A wrapper class for storing the {@link AppShellRegistry} instance in the
     * servlet context.
     */
    public static class AppShellRegistryWrapper implements Serializable {
        private final AppShellRegistry registry;

        /**
         * Construct a wraper for an app-shell registry.
         *
         * @param registry
         *            the app shell registry
         */
        public AppShellRegistryWrapper(AppShellRegistry registry) {
            this.registry = registry;
        }
    }

    private AppShellRegistry() {
    }

    /**
     * Returns the instance of the registry, or create a new one if it does not
     * exist yet.
     *
     * @param context
     *            servlet context
     * @return the registry instance
     */
    public static AppShellRegistry getInstance(VaadinContext context) {
        synchronized (context) { // NOSONAR
            AppShellRegistryWrapper attribute = context
                    .getAttribute(AppShellRegistryWrapper.class);
            if (attribute == null) {
                attribute = new AppShellRegistryWrapper(new AppShellRegistry());
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
        this.appShellClass = null;
    }

    /**
     * Sets the {@link AppShellConfigurator} class in the application. Pass a
     * null to reset the previous one when reusing the instance.
     *
     * @param shell
     *            the class implementing AppShellConfigurator.
     */
    public void setShell(Class<? extends AppShellConfigurator> shell) {
        if (this.appShellClass != null && shell != null) {
            throw new InvalidApplicationConfigurationException(
                    String.format(AppShellRegistry.ERROR_MULTIPLE_SHELL,
                            this.appShellClass.getName(), shell.getName()));
        }
        if (shell != null && Component.class.isAssignableFrom(shell)) {
            throw new InvalidApplicationConfigurationException(
                    String.format(ERROR_EXTENDS_COMPONENT, shell.getName()));
        }
        this.appShellClass = shell;
    }

    /**
     * Returns the {@link AppShellConfigurator} class in the application.
     *
     * @return the app shell class
     */
    public Class<? extends AppShellConfigurator> getShell() {
        return appShellClass;
    }

    /**
     * Checks whether a class have annotations that should only be in
     * {@link AppShellConfigurator} classes.
     *
     * @param clz
     *            the class to check.
     * @return a string with the error lines if the class has offending
     *         annotations
     */
    public String validateClass(Class<?> clz) {
        String error = null;

        List<Class<?>> validOnlyForAppShell = (List) getValidAnnotations();
        // PageTitle can be in AppShell and Views
        validOnlyForAppShell.remove(PageTitle.class);
        // StyleSheet is allowed on Components; do not treat it as an
        // AppShell-only annotation
        validOnlyForAppShell.remove(StyleSheet.class);
        validOnlyForAppShell.remove(StyleSheet.Container.class);
        if (WebComponentExporter.class.isAssignableFrom(clz)) {
            // Webcomponent exporter should have the theme annotation
            // and Push annotation as it is not appShell configured.
            validOnlyForAppShell.remove(Theme.class);
            validOnlyForAppShell.remove(Push.class);
        }

        String offending = getClassAnnotations(clz, validOnlyForAppShell);
        if (!offending.isEmpty()) {
            error = String.format(AppShellRegistry.ERROR_LINE, offending,
                    clz.getName());
        }
        return error;
    }

    private AppShellSettings createSettings(VaadinRequest request) {
        AppShellSettings settings = new AppShellSettings();

        getAnnotations(Meta.class).forEach(
                meta -> settings.addMetaTag(meta.name(), meta.content()));

        List<Viewport> viewPorts = getAnnotations(Viewport.class);
        if (viewPorts.size() > 1) {
            throw new InvalidApplicationConfigurationException(
                    String.format(AppShellRegistry.ERROR_MULTIPLE_ANNOTATION,
                            Viewport.class.getSimpleName()));
        } else if (!viewPorts.isEmpty()) {
            settings.setViewport(viewPorts.get(0).value());
        }
        List<BodySize> bodySizes = getAnnotations(BodySize.class);
        if (bodySizes.size() > 1) {
            throw new InvalidApplicationConfigurationException(
                    String.format(AppShellRegistry.ERROR_MULTIPLE_ANNOTATION,
                            BodySize.class.getSimpleName()));
        } else if (!bodySizes.isEmpty()) {
            settings.setBodySize(bodySizes.get(0).width(),
                    bodySizes.get(0).height());
        }
        List<PageTitle> pageTitles = getAnnotations(PageTitle.class);
        if (pageTitles.size() > 1) {
            throw new InvalidApplicationConfigurationException(
                    String.format(AppShellRegistry.ERROR_MULTIPLE_ANNOTATION,
                            PageTitle.class.getSimpleName()));
        } else if (!pageTitles.isEmpty()) {
            settings.setPageTitle(pageTitles.get(0).value());
        }
        getAnnotations(Inline.class).forEach(settings::addInline);

        Map<String, String> stylesheets = new LinkedHashMap<>();
        for (StyleSheet sheet : getAnnotations(StyleSheet.class)) {
            String href = resolveStyleSheetHref(sheet.value(), request);
            if (href != null && !href.isBlank()) {
                stylesheets.put(href, sheet.value());
            }
        }
        addStyleSheets(request, stylesheets, settings);
        return settings;
    }

    private static String resolveStyleSheetHref(String href,
            VaadinRequest request) {
        if (href == null || href.isBlank()) {
            return null;
        }
        if (HandlerHelper
                .isPathUnsafe(href.startsWith("/") ? href : "/" + href)) {
            log.warn(
                    "@StyleSheet href containing traversals ('../') are not allowed, ignored: {}",
                    href);
            return null;
        }
        href = href.trim();
        // Accept absolute http(s) URLs unchanged
        String lower = href.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")) {
            return href;
        }
        // Treat ./ as relative path to static resources location
        if (href.startsWith("./")) {
            href = href.substring(2);
        }
        // Accept bare paths beginning with '/' as-is
        if (href.startsWith("/")) {
            return href;
        }

        String contextPath = request.getContextPath();
        if (!contextPath.isEmpty()) {
            String contextProtocol = ApplicationConstants.CONTEXT_PROTOCOL_PREFIX;
            if (!lower.startsWith(contextProtocol)) {
                // Prepend context protocol so URL is resolved with context path
                href = contextProtocol + href;
            }
        }
        BootstrapHandler.BootstrapUriResolver resolver = new BootstrapHandler.BootstrapUriResolver(
                contextPath + "/", null);
        return resolver.resolveVaadinUri(href);
    }

    /**
     * Modifies the `index.html` document based on the
     * {@link AppShellConfigurator} annotations or
     * {@link AppShellConfigurator#configurePage(AppShellSettings)} method.
     *
     * @param document
     *            a JSoup document for the index.html page
     * @param request
     *            The request to handle
     */
    public void modifyIndexHtml(Document document, VaadinRequest request) {
        AppShellSettings settings = createSettings(request);
        if (appShellClass != null) {
            VaadinService.getCurrent().getInstantiator()
                    .getOrCreate(appShellClass).configurePage(settings);
        }

        settings.getHeadElements(Position.PREPEND).forEach(
                elm -> insertElement(elm, document.head()::prependChild));
        settings.getHeadElements(Position.APPEND).forEach(
                elm -> insertElement(elm, document.head()::appendChild));

        settings.getInlineElements(request.getService(), TargetElement.HEAD,
                Position.PREPEND)
                .forEach(elm -> insertInlineElement(elm,
                        document.head()::prependChild));
        settings.getInlineElements(request.getService(), TargetElement.HEAD,
                Position.APPEND)
                .forEach(elm -> insertInlineElement(elm,
                        document.head()::appendChild));
        settings.getInlineElements(request.getService(), TargetElement.BODY,
                Position.PREPEND)
                .forEach(elm -> insertInlineElement(elm,
                        document.body()::prependChild));
        settings.getInlineElements(request.getService(), TargetElement.BODY,
                Position.APPEND)
                .forEach(elm -> insertInlineElement(elm,
                        document.body()::appendChild));
    }

    /**
     * Modifies PushConfiguration instance based on the {@link Push} annotation
     * on {@link AppShellConfigurator}.
     *
     * @param pushConfiguration
     *            the PushConfigration instance to modify
     */
    public void modifyPushConfiguration(PushConfiguration pushConfiguration) {
        List<Push> pushAnnotations = getAnnotations(Push.class);
        if (pushAnnotations.size() > 1) {
            throw new InvalidApplicationConfigurationException(
                    String.format(AppShellRegistry.ERROR_MULTIPLE_ANNOTATION,
                            Push.class.getSimpleName()));
        } else if (!pushAnnotations.isEmpty()) {
            Push push = pushAnnotations.get(0);
            pushConfiguration.setPushMode(push.value());
            pushConfiguration.setTransport(push.transport());
        }
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
        return "Shell: " + appShellClass + " metas: "
                + getAnnotations(Meta.class);
    }

    private <T extends Annotation> List<T> getAnnotations(Class<T> annotation) {
        assert getValidAnnotations().contains(annotation);
        return appShellClass == null ? Collections.emptyList()
                : Arrays.asList(appShellClass.getAnnotationsByType(annotation));
    }

    private static void addStyleSheets(VaadinRequest request,
            Map<String, String> stylesheets, AppShellSettings settings) {
        DeploymentConfiguration config = request.getService()
                .getDeploymentConfiguration();
        if (!config.isProductionMode()) {
            // Include hot-reloaded stylesheets
            try {
                ActiveStyleSheetTracker tracker = ActiveStyleSheetTracker
                        .get(request.getService());
                for (String url : tracker.getActiveUrls()) {
                    String href = resolveStyleSheetHref(url, request);
                    if (href != null && !href.isBlank()) {
                        stylesheets.putIfAbsent(href, url);
                    }
                }
            } catch (Exception e) {
                log.error("Failed to restore hot-reloaded stylesheets", e);
            }

            stylesheets.replaceAll((resolved, source) -> {
                if (source.startsWith("/")) {
                    source = source.substring(1);
                }
                if (source.startsWith("./")) {
                    source = source.substring(2);
                }
                if (source.startsWith(
                        ApplicationConstants.CONTEXT_PROTOCOL_PREFIX)) {
                    source = source.substring(
                            ApplicationConstants.CONTEXT_PROTOCOL_PREFIX
                                    .length());
                }
                return source;
            });
        }

        stylesheets.forEach((href, sourcePath) -> {
            Map<String, String> attributes = Map.of("rel", "stylesheet",
                    "data-file-path", sourcePath, "data-id",
                    "appShell-" + sourcePath);
            settings.addLink(Position.APPEND, href, attributes);
        });
    }
}
