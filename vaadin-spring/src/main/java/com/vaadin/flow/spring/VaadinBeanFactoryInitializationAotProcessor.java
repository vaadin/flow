package com.vaadin.flow.spring;

import java.lang.reflect.Member;
import java.util.ArrayList;
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
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.data.converter.Converter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouterLayout;

class VaadinBeanFactoryInitializationAotProcessor
        implements BeanFactoryInitializationAotProcessor {

    @Override
    public BeanFactoryInitializationAotContribution processAheadOfTime(
            ConfigurableListableBeanFactory beanFactory) {
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

                Set<Class<?>> routeTypes = new HashSet<Class<?>>();
                routeTypes
                        .addAll(reflections.getTypesAnnotatedWith(Route.class));
                routeTypes.addAll(
                        reflections.getTypesAnnotatedWith(RouteAlias.class));
                for (var c : routeTypes) {
                    registerType(hints, c);
                    registerResources(hints, c);
                }
                for (var c : reflections
                        .getSubTypesOf(AppShellConfigurator.class)) {
                    registerType(hints, c);
                    registerResources(hints, c);
                }
                for (var c : reflections.getSubTypesOf(Component.class)) {
                    registerType(hints, c);
                }
                for (var c : reflections.getSubTypesOf(RouterLayout.class)) {
                    registerType(hints, c);
                }
                for (var c : reflections
                        .getSubTypesOf(HasErrorParameter.class)) {
                    registerType(hints, c);
                }
                for (var c : reflections.getSubTypesOf(ComponentEvent.class)) {
                    registerType(hints, c);
                }
                for (var c : reflections.getSubTypesOf(Converter.class)) {
                    registerType(hints, c);
                }
                for (var c : reflections.getSubTypesOf(HasUrlParameter.class)) {
                    registerType(hints, c);
                }
            }
        };
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
