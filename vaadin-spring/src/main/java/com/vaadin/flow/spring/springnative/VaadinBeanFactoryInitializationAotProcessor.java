package com.vaadin.flow.spring.springnative;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouterLayout;

public class VaadinBeanFactoryInitializationAotProcessor
        implements BeanFactoryInitializationAotProcessor {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    public static class Marker {

    }

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(
            ConfigurableListableBeanFactory beanFactory) {
        // Find and register @Route classes so they can be created as beans at
        // runtime
        if (beanFactory instanceof BeanDefinitionRegistry) {
            findAndRegisterRoutes(
                    (BeanDefinitionRegistry & BeanFactory) beanFactory);
        } else {
            logger.error(
                    "Unable to register @Route classes as beans because the used bean factory is of type {} which does not implement {}",
                    beanFactory.getClass().getName(),
                    BeanDefinitionRegistry.class.getName());
        }

        return (generationContext, beanFactoryInitializationCode) -> {
            var hints = generationContext.getRuntimeHints();
            for (var pkg : getPackages(beanFactory)) {
                var reflections = new Reflections(pkg);

                /*
                 * This aims to register most types in the project that are
                 * needed for Flow to function properly. Examples are @Route
                 * annotated classes, Component and event classes which are
                 * instantiated through reflection etc
                 */

                for (var c : getRouteTypesFor(reflections, pkg)) {
                    registerType(hints, c);
                    registerResources(hints, c);
                }
                for (var c : reflections
                        .getSubTypesOf(AppShellConfigurator.class)) {
                    registerType(hints, c);
                    registerResources(hints, c);
                }
                registerSubTypes(hints, reflections, Component.class);
                registerSubTypes(hints, reflections, RouterLayout.class);
                registerSubTypes(hints, reflections, HasErrorParameter.class);
                registerSubTypes(hints, reflections, ComponentEvent.class);
                registerSubTypes(hints, reflections, HasUrlParameter.class);
                registerSubTypes(hints, reflections,
                        "com.vaadin.flow.data.converter.Converter");
            }
        };
    }

    private void registerSubTypes(RuntimeHints hints, Reflections reflections,
            Class<?> cls) {
        for (var c : reflections.getSubTypesOf(cls)) {
            registerType(hints, c);
        }
    }

    private void registerSubTypes(RuntimeHints hints, Reflections reflections,
            String className) {
        try {
            Class<?> cls = Class.forName(className);
            for (var c : reflections.getSubTypesOf(cls)) {
                registerType(hints, c);
            }
        } catch (ClassNotFoundException | NoClassDefFoundError e) {
            // Ignore. this happens for e.g. Converter in a Hilla project where
            // you do not
            // have flow-data
        }
    }

    private static List<String> getPackagesWithRoutes(BeanFactory beanFactory) {
        List<String> packages = new ArrayList<String>();
        packages.add("com.vaadin");
        packages.addAll(AutoConfigurationPackages.get(beanFactory));
        return packages;
    }

    private <T extends BeanFactory & BeanDefinitionRegistry> void findAndRegisterRoutes(
            T beanFactory) {
        String markerBeanName = Marker.class.getName();
        logger.debug("Finding and registering routes");

        if (beanFactory.containsBeanDefinition(markerBeanName)) {
            logger.debug("Routes already registered");
            return;
        }

        Set<String> registeredClasses = new HashSet<>();
        for (String beanName : beanFactory.getBeanDefinitionNames()) {
            // Routes can be manually registered using @Component.
            // We should not register those again
            BeanDefinition def = beanFactory.getBeanDefinition(beanName);
            if (def.getBeanClassName() != null) {
                registeredClasses.add(def.getBeanClassName());
            }
        }

        for (String pkg : getPackagesWithRoutes(beanFactory)) {
            logger.debug("Scanning for @{} or @{} annotated beans in {}",
                    Route.class.getSimpleName(),
                    RouteAlias.class.getSimpleName(), pkg);
            var reflections = new Reflections(pkg);
            for (var c : getRouteTypesFor(reflections, pkg)) {
                if (registeredClasses.contains(c.getName())) {
                    logger.debug(
                            "Skipping route class {} as it has already been registered as a bean",
                            c.getName());
                    continue;
                }
                registeredClasses.add(c.getName());
                logger.debug("Registering a bean for route class {}",
                        c.getName());
                AbstractBeanDefinition beanDefinition = BeanDefinitionBuilder
                        .rootBeanDefinition(c).setScope("prototype")
                        .getBeanDefinition();
                beanFactory.registerBeanDefinition(c.getName(), beanDefinition);
            }
        }

        beanFactory.registerBeanDefinition(markerBeanName, BeanDefinitionBuilder
                .rootBeanDefinition(Marker.class).getBeanDefinition());

    }

    private static Collection<Class<?>> getRouteTypesFor(
            Reflections reflections, String packageName) {
        var routeTypes = new HashSet<Class<?>>();
        routeTypes.addAll(reflections.getTypesAnnotatedWith(Route.class));
        routeTypes.addAll(reflections.getTypesAnnotatedWith(RouteAlias.class));
        return routeTypes;
    }

    private void registerResources(RuntimeHints hints, Class<?> c) {
        if (c.getCanonicalName() == null) {
            // See
            // https://github.com/spring-projects/spring-framework/issues/29774
            return;
        }
        hints.resources().registerType(c);
    }

    private void registerType(RuntimeHints hints, Class<?> c) {
        if (c.getCanonicalName() == null) {
            // See
            // https://github.com/spring-projects/spring-framework/issues/29774
            return;
        }
        MemberCategory[] memberCategories = MemberCategory.values();
        hints.reflection().registerType(c, memberCategories);
    }

    private static List<String> getPackages(BeanFactory beanFactory) {
        var listOf = new ArrayList<String>();
        listOf.add("com.vaadin");
        listOf.addAll(AutoConfigurationPackages.get(beanFactory));
        return listOf;
    }

}
