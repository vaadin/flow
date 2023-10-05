/*
 * Copyright 2000-2023 Vaadin Ltd.
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
package com.vaadin.flow.spring;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.HandlesTypes;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.googlecode.gentyref.GenericTypeReflector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.di.LookupInitializer;
import com.vaadin.flow.internal.DevModeHandlerManager;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.AmbiguousRouteConfigurationException;
import com.vaadin.flow.server.InvalidRouteConfigurationException;
import com.vaadin.flow.server.RouteRegistry;
import com.vaadin.flow.server.VaadinServletContext;
import com.vaadin.flow.server.communication.IndexHtmlRequestHandler;
import com.vaadin.flow.server.startup.AbstractRouteRegistryInitializer;
import com.vaadin.flow.server.startup.AnnotationValidator;
import com.vaadin.flow.server.startup.ApplicationConfiguration;
import com.vaadin.flow.server.startup.ApplicationRouteRegistry;
import com.vaadin.flow.server.startup.ClassLoaderAwareServletContainerInitializer;
import com.vaadin.flow.server.startup.LookupServletContainerInitializer;
import com.vaadin.flow.server.startup.VaadinAppShellInitializer;
import com.vaadin.flow.server.startup.VaadinInitializerException;
import com.vaadin.flow.server.startup.WebComponentConfigurationRegistryInitializer;
import com.vaadin.flow.server.startup.WebComponentExporterAwareValidator;
import com.vaadin.flow.server.webcomponent.WebComponentConfigurationRegistry;
import com.vaadin.flow.spring.VaadinScanPackagesRegistrar.VaadinScanPackages;
import com.vaadin.flow.theme.Theme;

/**
 * Servlet context initializer for Spring Boot Application.
 * <p>
 * If Java application is used to run Spring Boot then it doesn't run registered
 * {@link ServletContainerInitializer}s (e.g. to scan for {@link Route}
 * annotations in the classpath). This class enables this scanning via Spring so
 * that the functionality which relies on {@link ServletContainerInitializer}
 * works in the same way as in deployable WAR file.
 *
 * @see ServletContainerInitializer
 * @see RouteRegistry
 */
public class VaadinServletContextInitializer
        implements ServletContextInitializer {

    private static boolean devModeCachingEnabled;
    private ApplicationContext appContext;
    private ResourceLoader customLoader;

    /**
     * Packages that should be excluded when scanning all packages.
     */
    private static final List<String> DEFAULT_SCAN_NEVER = Stream.of("antlr",
            "cglib", "ch/quos/logback", "commons-codec", "commons-fileupload",
            "commons-io", "commons-logging", "com/fasterxml", "com/google",
            "com/h2database", "com/helger", "com/vaadin/external/atmosphere",
            "com/vaadin/webjar", "junit", "net/bytebuddy", "org/apache",
            "org/aspectj", "org/bouncycastle", "org/dom4j", "org/easymock",
            "org/eclipse/persistence", "org/hamcrest", "org/hibernate",
            "org/javassist", "org/jboss", "org/jsoup", "org/seleniumhq",
            "org/slf4j", "org/atmosphere", "org/springframework",
            "org/webjars/bowergithub", "org/yaml",

            "java/", "javax/", "javafx/", "com/sun/", "oracle/deploy",
            "oracle/javafx", "oracle/jrockit", "oracle/jvm", "oracle/net",
            "oracle/nio", "oracle/tools", "oracle/util", "oracle/webservices",
            "oracle/xmlns",

            "com/intellij/", "org/jetbrains",

            "com/vaadin/external/gwt", "javassist/", "io/methvin",
            "com/github/javaparser", "oshi/", "io/micrometer", "jakarta/",
            "com/nimbusds", "elemental/util", "elemental/json",
            "org/reflections", "org/aopalliance", "org/objectweb")
            .collect(Collectors.toList());

    /**
     * Packages that should be scanned by default and can't be overriden by a
     * custom list.
     */
    private static final List<String> DEFAULT_SCAN_ONLY = Stream
            .of(Component.class.getPackage().getName(),
                    Theme.class.getPackage().getName(),
                    // LitRenderer uses script annotation
                    "com.vaadin.flow.data.renderer", "com.vaadin.shrinkwrap",
                    "dev.hilla")
            .collect(Collectors.toList());

    /**
     * Packages marked by the user to be scanned exclusively.
     */
    private final List<String> customScanOnly;

    /**
     * Class path scanner that reuses infrastructure from Spring while also
     * considering abstract types.
     */
    private static class ClassPathScanner
            extends ClassPathScanningCandidateComponentProvider {
        private ClassPathScanner(Environment environment,
                ResourceLoader resourceLoader,
                Collection<Class<? extends Annotation>> annotations,
                Collection<Class<?>> types) {
            super(false, environment);
            setResourceLoader(resourceLoader);

            annotations.stream().map(AnnotationTypeFilter::new)
                    .forEach(this::addIncludeFilter);
            types.stream().map(AssignableTypeFilter::new)
                    .forEach(this::addIncludeFilter);
        }

        @Override
        protected boolean isCandidateComponent(
                AnnotatedBeanDefinition beanDefinition) {
            return super.isCandidateComponent(beanDefinition)
                    || beanDefinition.getMetadata().isAbstract();
        }
    }

    /*
     * A wrapper interface for {@link ServletContextListener} that allows not
     * running more listeners when one fails. This allows that user does not
     * wait until the last listener has been run.
     */
    private interface FailFastServletContextListener
            extends ServletContextListener, Serializable {

        static String ATTR = "failed-"
                + FailFastServletContextListener.class.getName();

        @Override
        default void contextInitialized(ServletContextEvent event) {
            if (event.getServletContext().getAttribute(ATTR) == null) {
                try {
                    failFastContextInitialized(event);
                } catch (Exception e) {
                    event.getServletContext().setAttribute(ATTR, true);
                    throw new RuntimeException(
                            "Unable to initialize " + this.getClass().getName(),
                            e);
                }
            }
        }

        @Override
        default void contextDestroyed(ServletContextEvent sce) {
            // NOP
        }

        void failFastContextInitialized(ServletContextEvent event)
                throws ServletException;

    }

    private static class CompositeServletContextListener
            implements ServletContextListener, Serializable {
        private final List<FailFastServletContextListener> listeners = new ArrayList<>();

        @Override
        public void contextInitialized(ServletContextEvent event) {
            long start = System.nanoTime();

            listeners.forEach(listener -> listener.contextInitialized(event));

            long ms = (System.nanoTime() - start) / 1000000;
            getLogger().debug(
                    "Total time for Vaadin Servlet Context Init took {} ms",
                    ms);
        }

        @Override
        public void contextDestroyed(ServletContextEvent event) {
            listeners.forEach(listener -> listener.contextDestroyed(event));
        }

        private void addListener(FailFastServletContextListener listener) {
            listeners.add(listener);
        }

    }

    private class LookupInitializerListener
            extends LookupServletContainerInitializer
            implements FailFastServletContextListener {

        @Override
        public void failFastContextInitialized(ServletContextEvent event)
                throws ServletException {
            long start = System.nanoTime();

            VaadinServletContext vaadinContext = new VaadinServletContext(
                    event.getServletContext());
            if (vaadinContext.getAttribute(Lookup.class) != null) {
                return;
            }

            Set<Class<?>> classes = null;
            if (devModeCachingEnabled) {
                classes = ReloadCache.lookupClasses;
            }
            if (classes == null) {
                classes = Stream
                        .concat(findByAnnotationOrSuperType(getLookupPackages(),
                                appContext, Collections.emptyList(),
                                getServiceTypes()),
                                // LookupInitializer is necessary here: it
                                // allows identify Spring boot as a regular Web
                                // container (and run
                                // LookupServletContainerInitializer logic) even
                                // though LookupInitializer will be ignored
                                // because there is its subclass
                                // SpringLookupInitializer provided
                                Stream.of(LookupInitializer.class,
                                        SpringLookupInitializer.class))
                        .collect(Collectors.toSet());
                if (devModeCachingEnabled) {
                    ReloadCache.lookupClasses = classes;
                }
            }
            process(classes, event.getServletContext());

            long ms = (System.nanoTime() - start) / 1000000;
            getLogger().debug("Lookup initializer took {} ms", ms);

        }

        @Override
        protected Collection<Class<?>> getServiceTypes() {
            // intentionally make annotation unmodifiable empty list because
            // LookupInitializer doesn't have annotations at the moment
            List<Class<? extends Annotation>> annotations = Collections
                    .emptyList();
            List<Class<?>> types = new LinkedList<>();
            collectHandleTypes(LookupServletContainerInitializer.class,
                    annotations, types);
            types.remove(LookupInitializer.class);
            return types;
        }

    }

    private class RouteServletContextListener
            extends AbstractRouteRegistryInitializer
            implements FailFastServletContextListener {

        @SuppressWarnings("unchecked")
        @Override
        public void failFastContextInitialized(ServletContextEvent event) {
            long start = System.nanoTime();

            final VaadinServletContext vaadinServletContext = new VaadinServletContext(
                    event.getServletContext());
            ApplicationRouteRegistry registry = ApplicationRouteRegistry
                    .getInstance(vaadinServletContext);

            getLogger().debug(
                    "Servlet Context initialized. Running route discovering....");

            if (registry.getRegisteredRoutes().isEmpty()) {
                getLogger().debug("There are no discovered routes yet. "
                        + "Start to collect all routes from the classpath...");
                try {
                    Collection<String> routePackages = null;
                    if (devModeCachingEnabled
                            && ReloadCache.routePackages != null) {
                        routePackages = ReloadCache.routePackages;
                    } else {
                        routePackages = getRoutePackages();
                    }

                    List<Class<?>> routeClasses = findByAnnotation(
                            routePackages, Route.class, RouteAlias.class)
                            .collect(Collectors.toList());

                    if (devModeCachingEnabled) {
                        ReloadCache.routePackages = routeClasses.stream()
                                .map(Class::getPackageName)
                                .collect(Collectors.toSet());
                    }

                    getLogger().debug(
                            "Found {} route classes. Here is the list: {}",
                            routeClasses.size(), routeClasses);

                    Set<Class<? extends Component>> navigationTargets = validateRouteClasses(
                            vaadinServletContext, routeClasses.stream());

                    getLogger().debug(
                            "There are {} navigation targets after filtering route classes: {}",
                            navigationTargets.size(), navigationTargets);

                    RouteConfiguration routeConfiguration = RouteConfiguration
                            .forRegistry(registry);
                    routeConfiguration
                            .update(() -> setAnnotatedRoutes(routeConfiguration,
                                    navigationTargets));
                    registry.setPwaConfigurationClass(validatePwaClass(
                            vaadinServletContext, routeClasses.stream()));
                } catch (InvalidRouteConfigurationException e) {
                    throw new IllegalStateException(e);
                }
            } else {
                getLogger().debug(
                        "Skipped discovery as there was {} routes already in registry",
                        registry.getRegisteredRoutes().size());
            }

            long ms = (System.nanoTime() - start) / 1000000;
            getLogger().debug("Route discovery took {} ms", ms);
        }

        private void setAnnotatedRoutes(RouteConfiguration routeConfiguration,
                Set<Class<? extends Component>> routes) {
            routeConfiguration.getHandledRegistry().clean();
            for (Class<? extends Component> navigationTarget : routes) {
                try {
                    routeConfiguration.setAnnotatedRoute(navigationTarget);
                } catch (AmbiguousRouteConfigurationException exception) {
                    if (!handleAmbiguousRoute(routeConfiguration,
                            exception.getConfiguredNavigationTarget(),
                            navigationTarget)) {
                        throw exception;
                    }
                }
            }
        }

        private boolean handleAmbiguousRoute(
                RouteConfiguration routeConfiguration,
                Class<? extends Component> configuredNavigationTarget,
                Class<? extends Component> navigationTarget) {
            if (GenericTypeReflector.isSuperType(navigationTarget,
                    configuredNavigationTarget)) {
                return true;
            } else if (GenericTypeReflector.isSuperType(
                    configuredNavigationTarget, navigationTarget)) {
                routeConfiguration.removeRoute(configuredNavigationTarget);
                routeConfiguration.setAnnotatedRoute(navigationTarget);
                return true;
            }
            return false;
        }

    }

    private class ErrorParameterServletContextListener
            implements FailFastServletContextListener {

        @Override
        @SuppressWarnings("unchecked")
        public void failFastContextInitialized(ServletContextEvent event) {
            long start = System.nanoTime();

            ApplicationRouteRegistry registry = ApplicationRouteRegistry
                    .getInstance(new VaadinServletContext(
                            event.getServletContext()));
            Stream<Class<? extends Component>> hasErrorComponents = findBySuperType(
                    getErrorParameterPackages(), HasErrorParameter.class)
                    .filter(Component.class::isAssignableFrom)
                    .map(clazz -> (Class<? extends Component>) clazz);
            registry.setErrorNavigationTargets(
                    hasErrorComponents.collect(Collectors.toSet()));

            long ms = (System.nanoTime() - start) / 1000000;
            getLogger().debug("Search for error navigation targets took {} ms",
                    ms);
        }
    }

    private class AnnotationValidatorServletContextListener
            implements FailFastServletContextListener {

        @Override
        public void failFastContextInitialized(ServletContextEvent event) {
            long start = System.nanoTime();

            AnnotationValidator annotationValidator = new AnnotationValidator();
            validateAnnotations(annotationValidator, event.getServletContext(),
                    annotationValidator.getAnnotations());

            WebComponentExporterAwareValidator extraValidator = new WebComponentExporterAwareValidator();
            validateAnnotations(extraValidator, event.getServletContext(),
                    extraValidator.getAnnotations());

            long ms = (System.nanoTime() - start) / 1000000;
            getLogger().debug("Annotation validation took {} ms", ms);
        }

        @SuppressWarnings("unchecked")
        private void validateAnnotations(
                ClassLoaderAwareServletContainerInitializer initializer,
                ServletContext context, List<Class<?>> annotations) {

            Stream<Class<?>> annotatedClasses = findByAnnotation(
                    getVerifiableAnnotationPackages(),
                    annotations.toArray(new Class[annotations.size()]));
            Set<Class<?>> set = annotatedClasses.collect(Collectors.toSet());
            try {
                initializer.process(set, context);
            } catch (ServletException exception) {
                throw new RuntimeException(
                        "Unexpected servlet exception from "
                                + initializer.getClass() + " validator",
                        exception);
            }
        }
    }

    private class DevModeServletContextListener
            implements FailFastServletContextListener {

        private transient DevModeHandlerManager devModeHandlerManager;

        @Override
        public void failFastContextInitialized(ServletContextEvent event)
                throws ServletException {
            VaadinServletContext vaadinContext = new VaadinServletContext(
                    event.getServletContext());

            ApplicationConfiguration config = ApplicationConfiguration
                    .get(new VaadinServletContext(event.getServletContext()));

            if (config == null || config.isProductionMode()) {
                return;
            }

            Lookup lookup = vaadinContext.getAttribute(Lookup.class);
            devModeHandlerManager = lookup.lookup(DevModeHandlerManager.class);
            if (devModeHandlerManager == null) {
                throw new RuntimeException(
                        "no DevModeHandlerManager implementation found but "
                                + "but dev server enabled. Either disable by "
                                + "setting vaadin.frontend.hotdeploy=false (and "
                                + "run the build-frontend maven goal) or "
                                + "include the vaadin-dev-server dependency");
            }
            if (devModeHandlerManager.getDevModeHandler() != null) {
                /*
                 * If a Spring Boot app is deployed as a war, the initializers
                 * have already been run and should not be run again here
                 */
                return;
            }

            Set<String> basePackages;
            if (isScanOnlySet()) {
                basePackages = new HashSet<>(getScanOnlyPackages());
            } else {
                if (devModeCachingEnabled
                        && ReloadCache.dynamicWhiteList != null) {
                    basePackages = ReloadCache.dynamicWhiteList;
                } else {
                    basePackages = Collections.singleton("");
                }
            }

            long start = System.nanoTime();

            List<Class<? extends Annotation>> annotations = new ArrayList<>();
            List<Class<?>> superTypes = new ArrayList<>();
            collectHandleTypes(devModeHandlerManager.getHandlesTypes(),
                    annotations, superTypes);

            Set<Class<?>> classes = findByAnnotationOrSuperType(basePackages,
                    customLoader, annotations, superTypes)
                    .collect(Collectors.toSet());

            if (devModeCachingEnabled) {
                classes.addAll(ReloadCache.jarClasses);
                ReloadCache.dynamicWhiteList = classes.stream().filter(
                        c -> !ReloadCache.jarClassNames.contains(c.getName()))
                        .map(Class::getPackageName).collect(Collectors.toSet());
                ReloadCache.jarClasses = classes.stream().filter(
                        c -> ReloadCache.jarClassNames.contains(c.getName()))
                        .collect(Collectors.toSet());
            }

            long ms = (System.nanoTime() - start) / 1000000;
            getLogger().info(
                    "Search for subclasses and classes with annotations took {} ms",
                    ms);

            Environment environment = appContext.getEnvironment();
            if (ms > 10000 && environment
                    .getProperty("vaadin.whitelisted-packages") == null) {
                getLogger().info(
                        "Due to slow search it is recommended to use the whitelisted-packages feature to make scanning faster.\n\n"
                                + "See the whitelisted-packages section in the docs at https://vaadin.com/docs/latest/flow/integrations/spring/configuration#special-configuration-parameters");
            }

            start = System.nanoTime();
            try {
                devModeHandlerManager.initDevModeHandler(classes,
                        new VaadinServletContext(event.getServletContext()));
            } catch (VaadinInitializerException e) {
                throw new RuntimeException(
                        "Unable to initialize Vaadin DevModeHandler", e);
            }
            ms = (System.nanoTime() - start) / 1000000;
            getLogger().debug("DevModeHandlerManager init took {} ms", ms);

            // Make live reload port available for index.html handler
            event.getServletContext().setAttribute(
                    IndexHtmlRequestHandler.LIVE_RELOAD_PORT_ATTR,
                    environment.getProperty("spring.devtools.livereload.port"));
        }

        @Override
        public void contextDestroyed(ServletContextEvent sce) {
            if (devModeHandlerManager != null) {
                devModeHandlerManager.stopDevModeHandler();
            }
        }

        private Collection<String> getScanOnlyPackages() {
            HashSet<String> npmPackages = new HashSet<>(getDefaultPackages());
            npmPackages.addAll(DEFAULT_SCAN_ONLY);
            if (customScanOnly != null) {
                npmPackages.addAll(customScanOnly);
            }
            return npmPackages;
        }

        private boolean isScanOnlySet() {
            return customScanOnly != null && !customScanOnly.isEmpty();
        }

    }

    private class WebComponentServletContextListener
            implements FailFastServletContextListener {

        @Override
        public void failFastContextInitialized(ServletContextEvent event)
                throws ServletException {
            long start = System.nanoTime();

            WebComponentConfigurationRegistry registry = WebComponentConfigurationRegistry
                    .getInstance(new VaadinServletContext(
                            event.getServletContext()));

            if (registry.getConfigurations() == null
                    || registry.getConfigurations().isEmpty()) {
                WebComponentConfigurationRegistryInitializer initializer = new WebComponentConfigurationRegistryInitializer();

                Set<Class<?>> webComponentExporters = findBySuperType(
                        getWebComponentPackages(), WebComponentExporter.class)
                        .collect(Collectors.toSet());

                initializer.process(webComponentExporters,
                        event.getServletContext());
            }

            long ms = (System.nanoTime() - start) / 1000000;
            getLogger().debug("WebComponent init took {} ms", ms);
        }
    }

    private class VaadinAppShellContextListener
            implements FailFastServletContextListener {

        @Override
        public void failFastContextInitialized(ServletContextEvent event) {
            long start = System.nanoTime();

            ApplicationConfiguration config = ApplicationConfiguration
                    .get(new VaadinServletContext(event.getServletContext()));

            if (config == null) {
                return;
            }

            if (!config.isProductionMode()) {
                initializeDevModeClassCache();
            }

            Set<Class<?>> classes = null;
            if (devModeCachingEnabled) {
                classes = ReloadCache.appShellClasses;
            }
            if (classes == null) {
                classes = findByAnnotationOrSuperType(
                        getVerifiableAnnotationPackages(), customLoader,
                        VaadinAppShellInitializer.getValidAnnotations(),
                        VaadinAppShellInitializer.getValidSupers())
                        .collect(Collectors.toSet());
                if (devModeCachingEnabled) {
                    ReloadCache.appShellClasses = classes;
                }
            }

            VaadinAppShellInitializer.init(classes,
                    new VaadinServletContext(event.getServletContext()));

            long ms = (System.nanoTime() - start) / 1000000;
            getLogger().debug("Search for VaadinAppShell took {} ms", ms);
        }
    }

    /**
     * Creates a new {@link ServletContextInitializer} instance with application
     * {@code context} provided.
     *
     * @param context
     *            the application context
     */
    public VaadinServletContextInitializer(ApplicationContext context) {
        appContext = context;

        String neverScanProperty = appContext.getEnvironment()
                .getProperty("vaadin.blacklisted-packages");
        List<String> neverScan;
        if (neverScanProperty == null) {
            neverScan = Collections.emptyList();
        } else {
            neverScan = Arrays.stream(neverScanProperty.split(","))
                    .map(pkg -> pkg.replace('.', '/').trim())
                    .collect(Collectors.toList());
        }

        String onlyScanProperty = appContext.getEnvironment()
                .getProperty("vaadin.whitelisted-packages");
        if (onlyScanProperty == null) {
            customScanOnly = Collections.emptyList();
            customLoader = new CustomResourceLoader(appContext, neverScan);

        } else {
            customScanOnly = Arrays.stream(onlyScanProperty.split(","))
                    .map(onlyPackage -> onlyPackage.replace('/', '.').trim())
                    .collect(Collectors.toList());
            customLoader = appContext;
        }

        if (!customScanOnly.isEmpty() && !neverScan.isEmpty()) {
            getLogger().warn(
                    "vaadin.blacklisted-packages is ignored because both vaadin.whitelisted-packages and vaadin.blacklisted-packages have been set.");
        }
    }

    @Override
    public void onStartup(ServletContext servletContext)
            throws ServletException {

        VaadinServletContext vaadinContext = new VaadinServletContext(
                servletContext);
        servletContext.addListener(createCompositeListener(vaadinContext));
    }

    private void initializeDevModeClassCache() {
        try {
            Class.forName(
                    "org.springframework.boot.devtools.livereload.LiveReloadServer");
            if (appContext instanceof ConfigurableApplicationContext) {
                String devModeCachingProperty = appContext.getEnvironment()
                        .getProperty("vaadin.devmode-caching");
                if (devModeCachingProperty != null
                        && !"true".equals(devModeCachingProperty)) {
                    getLogger().debug(
                            "Disabling dev mode scanned class caching since "
                                    + "vaadin.devmode-caching is set to a non-true value.");
                } else {
                    getLogger().debug(
                            "Spring Boot DevTools found. Enabling scanned class caching.");
                    devModeCachingEnabled = true;
                    ((ConfigurableApplicationContext) appContext)
                            .addApplicationListener(new ReloadListener(e -> {
                                // Updates cached white list and route packages
                                Set<String> addedPackages = new HashSet<>();
                                e.getAddedClasses().forEach(c -> {
                                    addedPackages.add(
                                            c.substring(0, c.lastIndexOf(".")));
                                });
                                ReloadCache.dynamicWhiteList
                                        .addAll(addedPackages);
                                ReloadCache.routePackages.addAll(addedPackages);
                            }));
                }
            }
        } catch (ClassNotFoundException e) {
            getLogger().debug(
                    "Spring Boot DevTools not found. Disabling scanned class caching.");
        }
    }

    private CompositeServletContextListener createCompositeListener(
            VaadinServletContext context) {
        CompositeServletContextListener compositeListener = new CompositeServletContextListener();

        compositeListener.addListener(new LookupInitializerListener());

        compositeListener.addListener(new VaadinAppShellContextListener());

        ApplicationRouteRegistry registry = ApplicationRouteRegistry
                .getInstance(context);

        // If the registry is already initialized then RouteRegistryInitializer
        // has done its job already, skip the custom routes search
        if (registry.getRegisteredRoutes().isEmpty()) {
            /*
             * Don't rely on RouteRegistry.isInitialized() negative return value
             * here because it's not known whether RouteRegistryInitializer has
             * been executed already or not (the order is undefined). Postpone
             * this to the end of context initialization cycle. At this point
             * RouteRegistry is either initialized or it's not initialized
             * because an RouteRegistryInitializer has not been executed (end
             * never will).
             */
            compositeListener.addListener(new RouteServletContextListener());
        }

        compositeListener
                .addListener(new ErrorParameterServletContextListener());

        compositeListener
                .addListener(new AnnotationValidatorServletContextListener());

        compositeListener.addListener(new DevModeServletContextListener());

        // Skip custom web component builders search if registry already
        // initialized
        if (!WebComponentConfigurationRegistry.getInstance(context)
                .hasConfigurations()) {
            compositeListener
                    .addListener(new WebComponentServletContextListener());
        }

        return compositeListener;
    }

    private Stream<Class<?>> findByAnnotation(Collection<String> packages,
            Class<? extends Annotation>... annotations) {
        return findByAnnotation(packages, appContext, annotations);
    }

    private Stream<Class<?>> findByAnnotation(Collection<String> packages,
            ResourceLoader loader, Class<? extends Annotation>... annotations) {
        return findByAnnotationOrSuperType(packages, loader,
                Arrays.asList(annotations), Collections.emptySet());
    }

    Stream<Class<?>> findBySuperType(Collection<String> packages,
            Class<?> type) {
        return findBySuperType(packages, appContext, type);
    }

    private Stream<Class<?>> findBySuperType(Collection<String> packages,
            ResourceLoader loader, Class<?> type) {
        return findByAnnotationOrSuperType(packages, loader,
                Collections.emptySet(), Collections.singleton(type));
    }

    Stream<Class<?>> findByAnnotationOrSuperType(Collection<String> packages,
            ResourceLoader loader,
            Collection<Class<? extends Annotation>> annotations,
            Collection<Class<?>> types) {
        ClassPathScanner scanner = new ClassPathScanner(
                appContext.getEnvironment(), loader, annotations, types);
        return packages.stream().map(scanner::findCandidateComponents)
                .flatMap(Collection::stream).map(this::getBeanClass);
    }

    private Class<?> getBeanClass(BeanDefinition beanDefinition) {
        AbstractBeanDefinition definition = (AbstractBeanDefinition) beanDefinition;
        Class<?> beanClass;
        if (definition.hasBeanClass()) {
            beanClass = definition.getBeanClass();
        } else {
            try {
                beanClass = definition
                        .resolveBeanClass(appContext.getClassLoader());
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(e);
            }
        }
        return beanClass;
    }

    private Collection<String> getRoutePackages() {
        return getDefaultPackages();
    }

    private Collection<String> getVerifiableAnnotationPackages() {
        return getDefaultPackages();
    }

    private Collection<String> getWebComponentPackages() {
        return getDefaultPackages();
    }

    private Collection<String> getErrorParameterPackages() {
        return Stream
                .concat(Stream
                        .of(HasErrorParameter.class.getPackage().getName()),
                        getDefaultPackages().stream())
                .collect(Collectors.toSet());
    }

    List<String> getDefaultPackages() {
        List<String> packagesList = Collections.emptyList();
        if (appContext
                .getBeanNamesForType(VaadinScanPackages.class).length > 0) {
            VaadinScanPackages packages = appContext
                    .getBean(VaadinScanPackages.class);
            packagesList = packages.getScanPackages();

        }
        if (!packagesList.isEmpty()) {
            getLogger().trace(
                    "Using explicitly configured packages for scan Vaadin types at startup {}",
                    packagesList);
        } else if (AutoConfigurationPackages.has(appContext)) {
            packagesList = AutoConfigurationPackages.get(appContext);
        }
        return packagesList;
    }

    private List<String> getLookupPackages() {
        return Stream
                .concat(getDefaultPackages().stream(),
                        Stream.of("dev.hilla.frontend",
                                "com.vaadin.flow.component.polymertemplate.rpc",
                                "com.vaadin.base.devserver"))
                .collect(Collectors.toList());
    }

    private static void collectHandleTypes(Class<?> clazz,
            List<Class<? extends Annotation>> annotations,
            List<Class<?>> superTypes) {
        HandlesTypes handlesTypes = clazz.getAnnotation(HandlesTypes.class);
        assert handlesTypes != null;
        collectHandleTypes(handlesTypes.value(), annotations, superTypes);
    }

    private static void collectHandleTypes(Class<?>[] handleTypes,
            List<Class<? extends Annotation>> annotations,
            List<Class<?>> superTypes) {
        assert handleTypes != null;
        for (Class<?> type : handleTypes) {
            if (type.isAnnotation()) {
                annotations.add((Class<? extends Annotation>) type);
            } else {
                superTypes.add(type);
            }
        }
    }

    /**
     * For npm we scan all packages. For performance reasons and due to problems
     * with atmosphere we skip known packaged from our resources collection.
     */
    private static class CustomResourceLoader
            extends PathMatchingResourcePatternResolver {

        private final PrefixTree scanNever = new PrefixTree(DEFAULT_SCAN_NEVER);

        private final PrefixTree scanAlways = new PrefixTree(DEFAULT_SCAN_ONLY
                .stream().map(packageName -> packageName.replace('.', '/'))
                .collect(Collectors.toList()));

        public CustomResourceLoader(ResourceLoader resourceLoader,
                List<String> addedScanNever) {
            super(resourceLoader);

            Objects.requireNonNull(addedScanNever,
                    "addedScanNever shouldn't be null!");

            addedScanNever.forEach(scanNever::addPrefix);
        }

        /**
         * Lock used to ensure there's only one update going on at once.
         * <p>
         * The lock is configured to always guarantee a fair ordering.
         */
        private final ReentrantLock lock = new ReentrantLock(true);

        private Map<String, Resource[]> cache = new HashMap<>();
        private Set<String> rootPaths = new HashSet<>();

        @Override
        public Resource[] getResources(String locationPattern)
                throws IOException {
            lock.lock();
            try {
                if (cache.containsKey(locationPattern)) {
                    return cache.get(locationPattern);
                }
                Resource[] resources = collectResources(locationPattern);
                cache.put(locationPattern, resources);
                return resources;
            } finally {
                lock.unlock();
            }
        }

        private Resource[] collectResources(String locationPattern)
                throws IOException {
            List<Resource> resourcesList = new ArrayList<>();

            Set<String> skipped = ReloadCache.skippedResources;
            Set<String> valid = ReloadCache.validResources;

            for (Resource resource : super.getResources(locationPattern)) {
                String originalPath = resource.getURL().getPath();
                String path;
                if (originalPath.startsWith("file:///resources!")) {
                    // It's a resource from a native build, remove the
                    // prefix from URL path
                    path = originalPath
                            .substring("file:///resources!".length());
                } else {
                    path = originalPath;
                }

                if (devModeCachingEnabled && skipped.contains(originalPath)) {
                    continue;
                }

                if (devModeCachingEnabled && valid.contains(originalPath)) {
                    resourcesList.add(resource);
                } else {
                    if (path.endsWith(".jar!/")) {
                        resourcesList.add(resource);
                    } else if (path.endsWith("/")) {
                        rootPaths.add(path);
                        resourcesList.add(resource);
                    } else {
                        int index = path.lastIndexOf(".jar!/");
                        if (index >= 0) {
                            String relativePath = path.substring(index + 6);
                            if (devModeCachingEnabled
                                    && relativePath.endsWith(".class")) {
                                // Stores names of all classes from JARs
                                String className = relativePath
                                        .replace(File.separatorChar, '.')
                                        .replace(".class", "");
                                ReloadCache.jarClassNames.add(className);
                            }
                            if (shouldPathBeScanned(relativePath)) {
                                resourcesList.add(resource);
                            }
                        } else {
                            List<String> parents = rootPaths.stream()
                                    .filter(path::startsWith)
                                    .collect(Collectors.toList());
                            if (parents.isEmpty()) {
                                throw new IllegalStateException(String.format(
                                        "Parent resource of [%s] not found in the resources!",
                                        path));
                            }

                            if (parents.stream()
                                    .anyMatch(parent -> shouldPathBeScanned(
                                            path.substring(parent.length())))) {
                                resourcesList.add(resource);
                            }
                        }
                    }
                }

                if (devModeCachingEnabled) {
                    if (resourcesList.contains(resource)) {
                        valid.add(originalPath);
                    } else {
                        skipped.add(originalPath);
                    }
                }
            }

            return resourcesList.toArray(new Resource[0]);
        }

        private boolean shouldPathBeScanned(String path) {
            return scanAlways.hasPrefix(path) || !scanNever.hasPrefix(path);
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(VaadinServletContextInitializer.class);
    }
}
