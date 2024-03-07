/*
 * Copyright (C) 2000-2024 Vaadin Ltd
 *
 * This program is available under Vaadin Commercial License and Service Terms.
 *
 * See <https://vaadin.com/commercial-license-and-service-terms> for the full
 * license.
 */
package com.vaadin.flow.server.startup;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.HandlesTypes;
import javax.servlet.annotation.WebListener;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.InvalidApplicationConfigurationException;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.PageConfigurator;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.Theme;

import static com.vaadin.flow.server.AppShellRegistry.ERROR_HEADER_NO_APP_CONFIGURATOR;
import static com.vaadin.flow.server.AppShellRegistry.ERROR_HEADER_NO_SHELL;
import static com.vaadin.flow.server.AppShellRegistry.ERROR_HEADER_OFFENDING_CONFIGURATOR;
import static com.vaadin.flow.server.AppShellRegistry.ERROR_HEADER_OFFENDING_PWA;

/**
 * Servlet initializer visiting {@link AppShellConfigurator} configuration.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @since 3.0
 */
@HandlesTypes({ AppShellConfigurator.class, Meta.class, Meta.Container.class,
        PWA.class, Inline.class, Inline.Container.class, Viewport.class,
        BodySize.class, PageTitle.class, PageConfigurator.class, Push.class,
        Theme.class, NoTheme.class })
// @WebListener is needed so that servlet containers know that they have to run
// it
@WebListener
public class VaadinAppShellInitializer
        implements VaadinServletContextStartupInitializer,
        // implementing ServletContextListener is needed for the @WebListener
        // annotation.
        ServletContextListener, Serializable {

    @Override
    public void initialize(Set<Class<?>> classes, VaadinContext context) {
        init(AbstractAnnotationValidator
                .removeHandleTypesSelfReferences(classes, this), context);
    }

    /**
     * Initializes the {@link AppShellRegistry} for the application.
     *
     * @deprecated Use {@link #init(Set, VaadinContext)} instead by wrapping
     *             {@link ServletContext} with {@link VaadinServletContext}.
     *
     * @param classes
     *            a set of classes that matches the {@link HandlesTypes} set in
     *            this class.
     * @param context
     *            the servlet context.
     */
    @Deprecated
    public static void init(Set<Class<?>> classes, ServletContext context) {
        init(classes, new VaadinServletContext(context));
    }

    /**
     * Initializes the {@link AppShellRegistry} for the application.
     *
     * @param classes
     *            a set of classes that matches the {@link HandlesTypes} set in
     *            this class.
     * @param context
     *            the {@link VaadinContext}.
     */
    @SuppressWarnings("unchecked")
    public static void init(Set<Class<?>> classes, VaadinContext context) {
        ApplicationConfiguration config = ApplicationConfiguration.get(context);

        if (config.useV14Bootstrap()) {
            return;
        }

        boolean disregardOffendingAnnotations = config.getBooleanProperty(
                Constants.ALLOW_APPSHELL_ANNOTATIONS, false);

        AppShellRegistry registry = AppShellRegistry.getInstance(context);
        registry.reset();

        if (classes == null || classes.isEmpty()) {
            return;
        }

        List<String> offendingAnnotations = new ArrayList<>();

        classes.stream()
                // sort classes by putting the app shell in first position
                .sorted((a, b) -> registry.isShell(a) ? -1
                        : registry.isShell(b) ? 1 : 0)
                .forEach(clz -> {
                    if (registry.isShell(clz)) {
                        registry.setShell(
                                (Class<? extends AppShellConfigurator>) clz);
                        getLogger().debug(
                                "Using {} class for configuring `index.html` response",
                                clz.getName());
                    } else {
                        String error = registry.validateClass(clz);
                        if (error != null) {
                            offendingAnnotations.add(error);
                        }
                    }
                });

        if (!offendingAnnotations.isEmpty()) {
            if (disregardOffendingAnnotations) {
                boolean hasPwa = offendingAnnotations.stream()
                        .anyMatch(err -> err.matches(".*@PWA.*"));
                String message = String.format(
                        hasPwa ? ERROR_HEADER_OFFENDING_PWA
                                : ERROR_HEADER_NO_SHELL,
                        String.join("\n  ", offendingAnnotations));
                getLogger().error(message);
            } else {
                String message = String.format(ERROR_HEADER_NO_SHELL,
                        String.join("\n  ", offendingAnnotations));
                throw new InvalidApplicationConfigurationException(message);
            }
        }

        List<String> classesImplementingPageConfigurator = classes.stream()
                .filter(clz -> PageConfigurator.class.isAssignableFrom(clz))
                .map(Class::getName).collect(Collectors.toList());

        if (!classesImplementingPageConfigurator.isEmpty()) {
            String message = String.join("\n - ",
                    classesImplementingPageConfigurator);
            if (registry.getShell() != null) {
                message = String.format(ERROR_HEADER_OFFENDING_CONFIGURATOR,
                        registry.getShell().getName(), message);
                throw new InvalidApplicationConfigurationException(message);
            } else {
                message = String.format(ERROR_HEADER_NO_APP_CONFIGURATOR,
                        message);
                getLogger().error(message);
            }
        }
    }

    /**
     * Return the list of annotations handled by this class. This method is
     * thought to be called from external plugins (e.g. Vaadin Spring) that
     * would need to override the <code>@HandlesTypes</code>-based classpath
     * scanning.
     *
     * @return list of annotations handled by
     *         {@link VaadinAppShellInitializer#init(Set, VaadinContext)}
     */
    @SuppressWarnings("unchecked")
    public static List<Class<? extends Annotation>> getValidAnnotations() {
        return Arrays.stream(getHandledTypes()).filter(Class::isAnnotation)
                .map(clz -> (Class<? extends Annotation>) clz)
                .collect(Collectors.toList());
    }

    /**
     * Return the list of super classes handled by this class. This method is
     * thought to be called from external plugins (e.g. Vaadin Spring) that
     * would need to override the <code>@HandlesTypes</code>-based classpath
     * scanning.
     *
     * @return list of super classes handled by
     *         {@link VaadinAppShellInitializer#init(Set, VaadinContext)}
     */
    public static List<Class<?>> getValidSupers() {
        return Arrays.stream(getHandledTypes())
                .filter(clz -> !clz.isAnnotation())
                .collect(Collectors.toList());
    }

    private static Class<?>[] getHandledTypes() {
        HandlesTypes annotation = VaadinAppShellInitializer.class
                .getAnnotation(HandlesTypes.class);
        assert annotation != null;
        return annotation.value();
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(VaadinAppShellInitializer.class);
    }

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        // No need to do anything on init
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        // No need to do anything on destroy
    }
}
