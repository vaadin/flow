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

import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.annotation.HandlesTypes;
import jakarta.servlet.annotation.WebListener;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.component.page.Inline;
import com.vaadin.flow.component.page.Meta;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.AppShellRegistry;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.InvalidApplicationConfigurationException;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.VaadinContext;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.Theme;

import static com.vaadin.flow.server.AppShellRegistry.ERROR_HEADER_NO_SHELL;
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
        BodySize.class, PageTitle.class, Push.class, ColorScheme.class,
        Theme.class, NoTheme.class, StyleSheet.class,
        StyleSheet.Container.class })
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
     * @param classes
     *            a set of classes that matches the {@link HandlesTypes} set in
     *            this class.
     * @param context
     *            the {@link VaadinContext}.
     */
    @SuppressWarnings("unchecked")
    public static void init(Set<Class<?>> classes, VaadinContext context) {
        ApplicationConfiguration config = ApplicationConfiguration.get(context);

        boolean disregardOffendingAnnotations = config.getBooleanProperty(
                Constants.ALLOW_APPSHELL_ANNOTATIONS, false);

        AppShellRegistry registry = AppShellRegistry.getInstance(context);
        registry.reset();

        if (classes == null || classes.isEmpty()) {
            return;
        }

        List<String> offendingAnnotations = new ArrayList<>();
        AppShellPredicate predicate = context.getAttribute(Lookup.class)
                .lookup(AppShellPredicate.class);

        classes.stream()
                .filter(clazz -> !Modifier.isAbstract(clazz.getModifiers()))
                // sort classes by putting the app shell in first position
                .sorted((a, b) -> predicate.isShell(a) ? -1
                        : predicate.isShell(b) ? 1 : 0)
                .forEach(clz -> {
                    if (predicate.isShell(clz)) {
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
