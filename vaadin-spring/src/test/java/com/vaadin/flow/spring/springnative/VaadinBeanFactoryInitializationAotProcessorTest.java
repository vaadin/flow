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
package com.vaadin.flow.spring.springnative;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEvent;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.webcomponent.WebComponent;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.auth.MenuAccessControl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

class VaadinBeanFactoryInitializationAotProcessorTest {

    // ================== Route Detection & Bean Registration Tests

    @Test
    void processAheadOfTime_routeClass_beanDefinitionRegistered() {
        BeanDefinitionRegistry registry = processAotForBeanRegistration(
                TestRouteView.class);

        verify(registry).registerBeanDefinition(
                eq(TestRouteView.class.getName()), any(BeanDefinition.class));
    }

    @Test
    void processAheadOfTime_routeAliasClass_beanDefinitionRegistered() {
        BeanDefinitionRegistry registry = processAotForBeanRegistration(
                TestRouteAliasView.class);

        verify(registry).registerBeanDefinition(
                eq(TestRouteAliasView.class.getName()),
                any(BeanDefinition.class));
    }

    @Test
    void processAheadOfTime_layoutClass_beanDefinitionRegistered() {
        BeanDefinitionRegistry registry = processAotForBeanRegistration(
                TestLayoutView.class);

        verify(registry).registerBeanDefinition(
                eq(TestLayoutView.class.getName()), any(BeanDefinition.class));
    }

    @Test
    void processAheadOfTime_routeWithLayout_layoutBeanAlsoRegistered() {
        BeanDefinitionRegistry registry = processAotForBeanRegistration(
                RouteWithLayout.class);

        verify(registry).registerBeanDefinition(
                eq(RouteWithLayout.class.getName()), any(BeanDefinition.class));
        verify(registry).registerBeanDefinition(
                eq(TestLayoutView.class.getName()), any(BeanDefinition.class));
    }

    @Test
    void processAheadOfTime_routeWithRouteAliasLayout_layoutBeanAlsoRegistered() {
        BeanDefinitionRegistry registry = processAotForBeanRegistration(
                RouteAliasWithLayout.class);

        verify(registry).registerBeanDefinition(
                eq(RouteAliasWithLayout.class.getName()),
                any(BeanDefinition.class));
        verify(registry).registerBeanDefinition(
                eq(AnotherLayoutView.class.getName()),
                any(BeanDefinition.class));
    }

    @Test
    void processAheadOfTime_multipleRouteAliasWithLayouts_allLayoutsRegistered() {
        BeanDefinitionRegistry registry = processAotForBeanRegistration(
                MultipleRouteAliasView.class);

        verify(registry).registerBeanDefinition(
                eq(MultipleRouteAliasView.class.getName()),
                any(BeanDefinition.class));
        verify(registry).registerBeanDefinition(
                eq(TestLayoutView.class.getName()), any(BeanDefinition.class));
        verify(registry).registerBeanDefinition(
                eq(AnotherLayoutView.class.getName()),
                any(BeanDefinition.class));
    }

    @Test
    void processAheadOfTime_routeWithUILayout_uiLayoutNotRegistered() {
        BeanDefinitionRegistry registry = processAotForBeanRegistration(
                RouteWithUILayout.class);

        verify(registry).registerBeanDefinition(
                eq(RouteWithUILayout.class.getName()),
                any(BeanDefinition.class));
        // UI layout should NOT be registered as a bean
        verify(registry, never()).registerBeanDefinition(eq(UI.class.getName()),
                any(BeanDefinition.class));
    }

    @Test
    void processAheadOfTime_routeWithRouterLayoutDefault_routerLayoutNotRegistered() {
        BeanDefinitionRegistry registry = processAotForBeanRegistration(
                RouteWithDefaultLayout.class);

        verify(registry).registerBeanDefinition(
                eq(RouteWithDefaultLayout.class.getName()),
                any(BeanDefinition.class));
        // RouterLayout.class (default) should NOT be registered as a bean
        verify(registry, never()).registerBeanDefinition(
                eq(RouterLayout.class.getName()), any(BeanDefinition.class));
    }

    @Test
    void processAheadOfTime_alreadyRegisteredBean_notReRegistered() {
        ConfigurableListableBeanFactory beanFactory = mock(
                ConfigurableListableBeanFactory.class,
                withSettings().extraInterfaces(BeanDefinitionRegistry.class));
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

        try (var mockedStatic = Mockito
                .mockStatic(AutoConfigurationPackages.class)) {
            mockedStatic.when(() -> AutoConfigurationPackages.get(beanFactory))
                    .thenReturn(List.of(getClass().getPackageName()));

            when(beanFactory.getBeanClassLoader())
                    .thenReturn(getClass().getClassLoader());
            // Simulate TestRouteView already registered
            when(beanFactory.getBeanDefinitionNames())
                    .thenReturn(new String[] { "existingBean" });
            GenericBeanDefinition existingDef = new GenericBeanDefinition();
            existingDef.setBeanClassName(TestRouteView.class.getName());
            when(beanFactory.getBeanDefinition("existingBean"))
                    .thenReturn(existingDef);
            when(registry.containsBeanDefinition(
                    VaadinBeanFactoryInitializationAotProcessor.Marker.class
                            .getName()))
                    .thenReturn(false);

            VaadinBeanFactoryInitializationAotProcessor processor = createProcessor(
                    TestRouteView.class);
            processor.processAheadOfTime(beanFactory);

            verify(registry, never()).registerBeanDefinition(
                    eq(TestRouteView.class.getName()),
                    any(BeanDefinition.class));
        }
    }

    @Test
    void processAheadOfTime_markerBean_registeredForIdempotency() {
        BeanDefinitionRegistry registry = processAotForBeanRegistration(
                TestRouteView.class);

        verify(registry).registerBeanDefinition(
                eq(VaadinBeanFactoryInitializationAotProcessor.Marker.class
                        .getName()),
                any(BeanDefinition.class));
    }

    @Test
    void processAheadOfTime_routesAlreadyProcessed_skipsProcessing() {
        ConfigurableListableBeanFactory beanFactory = mock(
                ConfigurableListableBeanFactory.class,
                withSettings().extraInterfaces(BeanDefinitionRegistry.class));
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

        try (var mockedStatic = Mockito
                .mockStatic(AutoConfigurationPackages.class)) {
            mockedStatic.when(() -> AutoConfigurationPackages.get(beanFactory))
                    .thenReturn(List.of(getClass().getPackageName()));

            when(beanFactory.getBeanClassLoader())
                    .thenReturn(getClass().getClassLoader());
            when(beanFactory.getBeanDefinitionNames())
                    .thenReturn(new String[0]);
            // Marker already exists - routes were already processed
            when(registry.containsBeanDefinition(
                    VaadinBeanFactoryInitializationAotProcessor.Marker.class
                            .getName()))
                    .thenReturn(true);

            VaadinBeanFactoryInitializationAotProcessor processor = createProcessor(
                    TestRouteView.class);
            processor.processAheadOfTime(beanFactory);

            // Should not register anything since marker exists
            verify(registry, never()).registerBeanDefinition(
                    eq(TestRouteView.class.getName()),
                    any(BeanDefinition.class));
        }
    }

    @Test
    void processAheadOfTime_beanDefinitionIsPrototypeScoped() {
        ConfigurableListableBeanFactory beanFactory = mock(
                ConfigurableListableBeanFactory.class,
                withSettings().extraInterfaces(BeanDefinitionRegistry.class));
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

        try (var mockedStatic = Mockito
                .mockStatic(AutoConfigurationPackages.class)) {
            mockedStatic.when(() -> AutoConfigurationPackages.get(beanFactory))
                    .thenReturn(List.of(getClass().getPackageName()));

            when(beanFactory.getBeanClassLoader())
                    .thenReturn(getClass().getClassLoader());
            when(beanFactory.getBeanDefinitionNames())
                    .thenReturn(new String[0]);
            when(registry.containsBeanDefinition(any())).thenReturn(false);

            VaadinBeanFactoryInitializationAotProcessor processor = createProcessor(
                    TestRouteView.class);
            processor.processAheadOfTime(beanFactory);

            ArgumentCaptor<BeanDefinition> captor = ArgumentCaptor
                    .forClass(BeanDefinition.class);
            verify(registry).registerBeanDefinition(
                    eq(TestRouteView.class.getName()), captor.capture());
            assertThat(captor.getValue().getScope())
                    .as("Bean definition should be prototype scoped")
                    .isEqualTo("prototype");
        }
    }

    // ================== Runtime Hints Tests ==================

    @Test
    void processAheadOfTime_routeClass_reflectionHintRegistered() {
        RuntimeHints hints = processAotForHints(TestRouteView.class);

        assertThat(
                RuntimeHintsPredicates.reflection().onType(TestRouteView.class))
                .as("Route class should be registered for reflection")
                .accepts(hints);
    }

    @Test
    void processAheadOfTime_componentSubtype_reflectionHintRegistered() {
        RuntimeHints hints = processAotForHintsWithSubtypes(
                TestComponentSubtype.class, Component.class);

        assertThat(RuntimeHintsPredicates.reflection()
                .onType(TestComponentSubtype.class))
                .as("Component subtype should be registered for reflection")
                .accepts(hints);
    }

    @Test
    void processAheadOfTime_routerLayoutSubtype_reflectionHintRegistered() {
        RuntimeHints hints = processAotForHintsWithSubtypes(
                TestLayoutView.class, RouterLayout.class);

        assertThat(RuntimeHintsPredicates.reflection()
                .onType(TestLayoutView.class))
                .as("RouterLayout subtype should be registered for reflection")
                .accepts(hints);
    }

    @Test
    void processAheadOfTime_hasErrorParameterSubtype_reflectionHintRegistered() {
        RuntimeHints hints = processAotForHintsWithSubtypes(
                TestErrorParameterView.class, HasErrorParameter.class);

        assertThat(RuntimeHintsPredicates.reflection()
                .onType(TestErrorParameterView.class))
                .as("HasErrorParameter subtype should be registered for reflection")
                .accepts(hints);
    }

    @Test
    void processAheadOfTime_componentEventSubtype_reflectionHintRegistered() {
        RuntimeHints hints = processAotForHintsWithSubtypes(
                TestComponentEvent.class, ComponentEvent.class);

        assertThat(RuntimeHintsPredicates.reflection()
                .onType(TestComponentEvent.class))
                .as("ComponentEvent subtype should be registered for reflection")
                .accepts(hints);
    }

    @Test
    void processAheadOfTime_hasUrlParameterSubtype_reflectionHintRegistered() {
        RuntimeHints hints = processAotForHintsWithSubtypes(
                TestUrlParameterView.class, HasUrlParameter.class);

        assertThat(RuntimeHintsPredicates.reflection()
                .onType(TestUrlParameterView.class))
                .as("HasUrlParameter subtype should be registered for reflection")
                .accepts(hints);
    }

    // ================== AppShellConfigurator & PWA Tests ==================

    @Test
    void processAheadOfTime_appShellConfigurator_reflectionHintRegistered() {
        RuntimeHints hints = processAotForHintsWithSubtypes(TestAppShell.class,
                AppShellConfigurator.class);

        assertThat(
                RuntimeHintsPredicates.reflection().onType(TestAppShell.class))
                .as("AppShellConfigurator should be registered for reflection")
                .accepts(hints);
    }

    @Test
    void processAheadOfTime_appShellWithPWA_jniHintsRegistered() {
        RuntimeHints hints = processAotForHintsWithSubtypes(
                TestAppShellWithPWA.class, AppShellConfigurator.class);

        assertThat(hints.jni().typeHints())
                .as("Should register JNI hints when PWA annotation is present")
                .isNotEmpty();
    }

    @Test
    void processAheadOfTime_appShellWithoutPWA_noJniHints() {
        RuntimeHints hints = processAotForHintsWithSubtypes(TestAppShell.class,
                AppShellConfigurator.class);

        assertThat(hints.jni().typeHints()).as(
                "Should not register JNI hints when PWA annotation is not present")
                .isEmpty();
    }

    // ================== Edge Cases ==================

    @Test
    void processAheadOfTime_nonBeanDefinitionRegistry_stillReturnsContribution() {
        ConfigurableListableBeanFactory beanFactory = mock(
                ConfigurableListableBeanFactory.class);
        // Note: NOT adding BeanDefinitionRegistry interface

        try (var mockedStatic = Mockito
                .mockStatic(AutoConfigurationPackages.class)) {
            mockedStatic.when(() -> AutoConfigurationPackages.get(beanFactory))
                    .thenReturn(List.of(getClass().getPackageName()));

            when(beanFactory.getBeanClassLoader())
                    .thenReturn(getClass().getClassLoader());

            VaadinBeanFactoryInitializationAotProcessor processor = createProcessor(
                    TestRouteView.class);
            BeanFactoryInitializationAotContribution contribution = processor
                    .processAheadOfTime(beanFactory);

            // Should still return contribution for hints even if bean
            // registration fails
            assertThat(contribution).as(
                    "Should return contribution even when bean factory is not a registry")
                    .isNotNull();
        }
    }

    @Test
    void processAheadOfTime_multipleRoutes_allRegistered() {
        BeanDefinitionRegistry registry = processAotForBeanRegistration(
                TestRouteView.class, TestRouteAliasView.class,
                TestLayoutView.class);

        verify(registry).registerBeanDefinition(
                eq(TestRouteView.class.getName()), any(BeanDefinition.class));
        verify(registry).registerBeanDefinition(
                eq(TestRouteAliasView.class.getName()),
                any(BeanDefinition.class));
        verify(registry).registerBeanDefinition(
                eq(TestLayoutView.class.getName()), any(BeanDefinition.class));
    }

    @Test
    void processAheadOfTime_multipleRoutes_allHintsRegistered() {
        RuntimeHints hints = processAotForHints(TestRouteView.class,
                TestRouteAliasView.class, TestLayoutView.class);

        assertThat(
                RuntimeHintsPredicates.reflection().onType(TestRouteView.class))
                .as("First route class should be registered for reflection")
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection()
                .onType(TestRouteAliasView.class))
                .as("Second route class should be registered for reflection")
                .accepts(hints);
        assertThat(RuntimeHintsPredicates.reflection()
                .onType(TestLayoutView.class))
                .as("Layout class should be registered for reflection")
                .accepts(hints);
    }

    // ================== Direct Scanning Tests ==================

    @Test
    void getRouteTypesFor_findsRouteAnnotatedClasses() {
        VaadinBeanFactoryInitializationAotProcessor processor = new VaadinBeanFactoryInitializationAotProcessor();

        Collection<Class<?>> routes = processor
                .getRouteTypesFor(getClass().getPackageName());

        assertThat(routes).as("Should find @Route annotated classes").contains(
                TestRouteView.class, RouteWithLayout.class,
                RouteWithUILayout.class, RouteWithDefaultLayout.class);
    }

    @Test
    void getRouteTypesFor_findsRouteAliasAnnotatedClasses() {
        VaadinBeanFactoryInitializationAotProcessor processor = new VaadinBeanFactoryInitializationAotProcessor();

        Collection<Class<?>> routes = processor
                .getRouteTypesFor(getClass().getPackageName());

        assertThat(routes).as("Should find @RouteAlias annotated classes")
                .contains(TestRouteAliasView.class, RouteAliasWithLayout.class,
                        MultipleRouteAliasView.class);
    }

    @Test
    void getRouteTypesFor_findsLayoutAnnotatedClasses() {
        VaadinBeanFactoryInitializationAotProcessor processor = new VaadinBeanFactoryInitializationAotProcessor();

        Collection<Class<?>> routes = processor
                .getRouteTypesFor(getClass().getPackageName());

        assertThat(routes).as("Should find @Layout annotated classes")
                .contains(TestLayoutView.class);
    }

    @Test
    void getSubtypesOf_findsComponentSubclasses() {
        VaadinBeanFactoryInitializationAotProcessor processor = new VaadinBeanFactoryInitializationAotProcessor();

        Collection<Class<?>> components = processor
                .getSubtypesOf(getClass().getPackageName(), Component.class);

        assertThat(components).as("Should find Component subclasses").contains(
                TestComponentSubtype.class, TestRouteView.class,
                TestLayoutView.class);
    }

    @Test
    void getSubtypesOf_findsRouterLayoutImplementations() {
        VaadinBeanFactoryInitializationAotProcessor processor = new VaadinBeanFactoryInitializationAotProcessor();

        Collection<Class<?>> layouts = processor
                .getSubtypesOf(getClass().getPackageName(), RouterLayout.class);

        assertThat(layouts).as("Should find RouterLayout implementations")
                .contains(TestLayoutView.class, AnotherLayoutView.class);
    }

    @Test
    void getSubtypesOf_findsHasErrorParameterImplementations() {
        VaadinBeanFactoryInitializationAotProcessor processor = new VaadinBeanFactoryInitializationAotProcessor();

        Collection<Class<?>> errorHandlers = processor.getSubtypesOf(
                getClass().getPackageName(), HasErrorParameter.class);

        assertThat(errorHandlers)
                .as("Should find HasErrorParameter implementations")
                .contains(TestErrorParameterView.class);
    }

    @Test
    void getSubtypesOf_findsHasUrlParameterImplementations() {
        VaadinBeanFactoryInitializationAotProcessor processor = new VaadinBeanFactoryInitializationAotProcessor();

        Collection<Class<?>> urlParamViews = processor.getSubtypesOf(
                getClass().getPackageName(), HasUrlParameter.class);

        assertThat(urlParamViews)
                .as("Should find HasUrlParameter implementations")
                .contains(TestUrlParameterView.class);
    }

    @Test
    void getSubtypesOf_findsComponentEventSubclasses() {
        VaadinBeanFactoryInitializationAotProcessor processor = new VaadinBeanFactoryInitializationAotProcessor();

        Collection<Class<?>> events = processor.getSubtypesOf(
                getClass().getPackageName(), ComponentEvent.class);

        assertThat(events).as("Should find ComponentEvent subclasses")
                .contains(TestComponentEvent.class);
    }

    @Test
    void getSubtypesOf_findsAppShellConfiguratorImplementations() {
        VaadinBeanFactoryInitializationAotProcessor processor = new VaadinBeanFactoryInitializationAotProcessor();

        Collection<Class<?>> appShells = processor.getSubtypesOf(
                getClass().getPackageName(), AppShellConfigurator.class);

        assertThat(appShells)
                .as("Should find AppShellConfigurator implementations")
                .contains(TestAppShell.class, TestAppShellWithPWA.class);
    }

    @Test
    void getSubtypesOf_excludesParentType() {
        VaadinBeanFactoryInitializationAotProcessor processor = new VaadinBeanFactoryInitializationAotProcessor();

        Collection<Class<?>> layouts = processor
                .getSubtypesOf(getClass().getPackageName(), RouterLayout.class);

        assertThat(layouts).as("Should not include the parent type itself")
                .isNotEmpty().doesNotContain(RouterLayout.class);
    }

    @Test
    void getSubtypesOf_findsWebComponentExporterSubclasses() {
        VaadinBeanFactoryInitializationAotProcessor processor = new VaadinBeanFactoryInitializationAotProcessor();

        Collection<Class<?>> exporters = processor.getSubtypesOf(
                getClass().getPackageName(), WebComponentExporter.class);

        assertThat(exporters).as("Should find WebComponentExporter subclasses")
                .contains(TestWebComponentExporter.class);
    }

    @Test
    void getSubtypesOf_findsI18NProviderImplementations() {
        VaadinBeanFactoryInitializationAotProcessor processor = new VaadinBeanFactoryInitializationAotProcessor();

        Collection<Class<?>> providers = processor
                .getSubtypesOf(getClass().getPackageName(), I18NProvider.class);

        assertThat(providers).as("Should find I18NProvider implementations")
                .contains(TestI18NProvider.class);
    }

    @Test
    void getSubtypesOf_findsMenuAccessControlImplementations() {
        VaadinBeanFactoryInitializationAotProcessor processor = new VaadinBeanFactoryInitializationAotProcessor();

        Collection<Class<?>> accessControls = processor.getSubtypesOf(
                getClass().getPackageName(), MenuAccessControl.class);

        assertThat(accessControls)
                .as("Should find MenuAccessControl implementations")
                .contains(TestMenuAccessControl.class);
    }

    @Test
    void processAheadOfTime_webComponentExporterSubtype_reflectionHintRegistered() {
        RuntimeHints hints = processAotForHintsWithSubtypes(
                TestWebComponentExporter.class, WebComponentExporter.class);

        assertThat(RuntimeHintsPredicates.reflection()
                .onType(TestWebComponentExporter.class))
                .as("WebComponentExporter subtype should be registered for reflection")
                .accepts(hints);
    }

    @Test
    void processAheadOfTime_i18nProviderSubtype_reflectionHintRegistered() {
        RuntimeHints hints = processAotForHintsWithSubtypes(
                TestI18NProvider.class, I18NProvider.class);

        assertThat(RuntimeHintsPredicates.reflection()
                .onType(TestI18NProvider.class))
                .as("I18NProvider subtype should be registered for reflection")
                .accepts(hints);
    }

    @Test
    void processAheadOfTime_menuAccessControlSubtype_reflectionHintRegistered() {
        RuntimeHints hints = processAotForHintsWithSubtypes(
                TestMenuAccessControl.class, MenuAccessControl.class);

        assertThat(RuntimeHintsPredicates.reflection()
                .onType(TestMenuAccessControl.class))
                .as("MenuAccessControl subtype should be registered for reflection")
                .accepts(hints);
    }

    @Test
    void getAnnotatedClasses_findsClassesWithSpecificAnnotation() {
        VaadinBeanFactoryInitializationAotProcessor processor = new VaadinBeanFactoryInitializationAotProcessor();

        Collection<Class<?>> routes = processor
                .getAnnotatedClasses(getClass().getPackageName(), Route.class);

        assertThat(routes).as("Should find classes annotated with @Route")
                .contains(TestRouteView.class, RouteWithLayout.class)
                .doesNotContain(TestAppShellWithPWA.class);
    }

    @Test
    void getAnnotatedClasses_findsClassesWithMultipleAnnotations() {
        VaadinBeanFactoryInitializationAotProcessor processor = new VaadinBeanFactoryInitializationAotProcessor();

        Collection<Class<?>> classes = processor.getAnnotatedClasses(
                getClass().getPackageName(), Route.class, RouteAlias.class);

        assertThat(classes)
                .as("Should find classes with either @Route or @RouteAlias")
                .contains(TestRouteView.class, TestRouteAliasView.class,
                        RouteWithLayout.class, RouteAliasWithLayout.class);
    }

    // ================== Helper Methods ==================

    private VaadinBeanFactoryInitializationAotProcessor createProcessor(
            Class<?>... routeClasses) {
        return new VaadinBeanFactoryInitializationAotProcessor() {
            @Override
            Collection<Class<?>> getRouteTypesFor(String packageName) {
                return Arrays.asList(routeClasses);
            }

            @Override
            Collection<Class<?>> getSubtypesOf(String basePackage,
                    Class<?> parentType) {
                return Arrays.stream(routeClasses)
                        .filter(parentType::isAssignableFrom)
                        .collect(Collectors.toList());
            }

            @Override
            Collection<Class<?>> getAnnotatedClasses(String basePackage,
                    Class<?>... annotations) {
                return Collections.emptyList();
            }
        };
    }

    private VaadinBeanFactoryInitializationAotProcessor createProcessorWithSubtypes(
            Class<?> subtypeClass, Class<?> parentType) {
        return new VaadinBeanFactoryInitializationAotProcessor() {
            @Override
            Collection<Class<?>> getRouteTypesFor(String packageName) {
                return Collections.emptyList();
            }

            @Override
            Collection<Class<?>> getSubtypesOf(String basePackage,
                    Class<?> parentTypeParam) {
                if (parentType.equals(parentTypeParam)
                        || parentTypeParam.isAssignableFrom(subtypeClass)) {
                    return List.of(subtypeClass);
                }
                return Collections.emptyList();
            }

            @Override
            Collection<Class<?>> getAnnotatedClasses(String basePackage,
                    Class<?>... annotations) {
                return Collections.emptyList();
            }
        };
    }

    private BeanDefinitionRegistry processAotForBeanRegistration(
            Class<?>... routeClasses) {
        ConfigurableListableBeanFactory beanFactory = mock(
                ConfigurableListableBeanFactory.class,
                withSettings().extraInterfaces(BeanDefinitionRegistry.class));
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

        try (var mockedStatic = Mockito
                .mockStatic(AutoConfigurationPackages.class)) {
            mockedStatic.when(() -> AutoConfigurationPackages.get(beanFactory))
                    .thenReturn(List.of(getClass().getPackageName()));

            when(beanFactory.getBeanClassLoader())
                    .thenReturn(getClass().getClassLoader());
            when(beanFactory.getBeanDefinitionNames())
                    .thenReturn(new String[0]);
            when(registry.containsBeanDefinition(any())).thenReturn(false);

            VaadinBeanFactoryInitializationAotProcessor processor = createProcessor(
                    routeClasses);
            processor.processAheadOfTime(beanFactory);
        }
        return registry;
    }

    private RuntimeHints processAotForHints(Class<?>... routeClasses) {
        ConfigurableListableBeanFactory beanFactory = mock(
                ConfigurableListableBeanFactory.class,
                withSettings().extraInterfaces(BeanDefinitionRegistry.class));
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

        try (var mockedStatic = Mockito
                .mockStatic(AutoConfigurationPackages.class)) {
            mockedStatic.when(() -> AutoConfigurationPackages.get(beanFactory))
                    .thenReturn(List.of(getClass().getPackageName()));

            when(beanFactory.getBeanClassLoader())
                    .thenReturn(getClass().getClassLoader());
            when(beanFactory.getBeanDefinitionNames())
                    .thenReturn(new String[0]);
            when(registry.containsBeanDefinition(any())).thenReturn(false);

            VaadinBeanFactoryInitializationAotProcessor processor = createProcessor(
                    routeClasses);
            BeanFactoryInitializationAotContribution contribution = processor
                    .processAheadOfTime(beanFactory);

            RuntimeHints hints = new RuntimeHints();
            if (contribution != null) {
                GenerationContext generationContext = mock(
                        GenerationContext.class);
                when(generationContext.getRuntimeHints()).thenReturn(hints);
                BeanFactoryInitializationCode code = mock(
                        BeanFactoryInitializationCode.class);
                contribution.applyTo(generationContext, code);
            }
            return hints;
        }
    }

    private RuntimeHints processAotForHintsWithSubtypes(Class<?> subtypeClass,
            Class<?> parentType) {
        ConfigurableListableBeanFactory beanFactory = mock(
                ConfigurableListableBeanFactory.class,
                withSettings().extraInterfaces(BeanDefinitionRegistry.class));
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

        try (var mockedStatic = Mockito
                .mockStatic(AutoConfigurationPackages.class)) {
            mockedStatic.when(() -> AutoConfigurationPackages.get(beanFactory))
                    .thenReturn(List.of(getClass().getPackageName()));

            when(beanFactory.getBeanClassLoader())
                    .thenReturn(getClass().getClassLoader());
            when(beanFactory.getBeanDefinitionNames())
                    .thenReturn(new String[0]);
            when(registry.containsBeanDefinition(any())).thenReturn(false);

            VaadinBeanFactoryInitializationAotProcessor processor = createProcessorWithSubtypes(
                    subtypeClass, parentType);
            BeanFactoryInitializationAotContribution contribution = processor
                    .processAheadOfTime(beanFactory);

            RuntimeHints hints = new RuntimeHints();
            if (contribution != null) {
                GenerationContext generationContext = mock(
                        GenerationContext.class);
                when(generationContext.getRuntimeHints()).thenReturn(hints);
                BeanFactoryInitializationCode code = mock(
                        BeanFactoryInitializationCode.class);
                contribution.applyTo(generationContext, code);
            }
            return hints;
        }
    }

    // ================== Test Data Classes ==================

    @Route("test")
    @Tag("div")
    public static class TestRouteView extends Component {
    }

    @Route("test")
    @RouteAlias("alias")
    @Tag("div")
    public static class TestRouteAliasView extends Component {
    }

    @Layout
    @Tag("div")
    public static class TestLayoutView extends Component
            implements RouterLayout {
    }

    @Tag("div")
    public static class AnotherLayoutView extends Component
            implements RouterLayout {
    }

    @Route(value = "with-layout", layout = TestLayoutView.class)
    @Tag("div")
    public static class RouteWithLayout extends Component {
    }

    @Route("test")
    @RouteAlias(value = "alias-with-layout", layout = AnotherLayoutView.class)
    @Tag("div")
    public static class RouteAliasWithLayout extends Component {
    }

    @Tag("div")
    public static class TestComponentSubtype extends Component {
    }

    @Tag("div")
    public static class TestErrorParameterView extends Component
            implements HasErrorParameter<Exception> {
        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<Exception> parameter) {
            return 500;
        }
    }

    @Tag("div")
    public static class TestUrlParameterView extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event, String parameter) {
        }
    }

    public static class TestComponentEvent extends ComponentEvent<Component> {
        public TestComponentEvent(Component source, boolean fromClient) {
            super(source, fromClient);
        }
    }

    public static class TestAppShell implements AppShellConfigurator {
    }

    @PWA(name = "Test App", shortName = "Test")
    public static class TestAppShellWithPWA implements AppShellConfigurator {
    }

    @Route("test")
    @RouteAlias(value = "alias1", layout = TestLayoutView.class)
    @RouteAlias(value = "alias2", layout = AnotherLayoutView.class)
    @Tag("div")
    public static class MultipleRouteAliasView extends Component {
    }

    @Route(value = "ui-layout", layout = UI.class)
    @Tag("div")
    public static class RouteWithUILayout extends Component {
    }

    @Route(value = "default-layout", layout = RouterLayout.class)
    @Tag("div")
    public static class RouteWithDefaultLayout extends Component {
    }

    public static class TestWebComponentExporter
            extends WebComponentExporter<Component> {
        public TestWebComponentExporter() {
            super("test-exporter");
        }

        @Override
        protected void configureInstance(WebComponent<Component> webComponent,
                Component component) {
        }
    }

    public static class TestI18NProvider implements I18NProvider {
        @Override
        public java.util.List<java.util.Locale> getProvidedLocales() {
            return java.util.List.of(java.util.Locale.ENGLISH);
        }

        @Override
        public String getTranslation(String key, java.util.Locale locale,
                Object... params) {
            return key;
        }
    }

    public static class TestMenuAccessControl implements MenuAccessControl {
        @Override
        public void setPopulateClientSideMenu(
                PopulateClientMenu populateClientMenu) {
        }

        @Override
        public PopulateClientMenu getPopulateClientSideMenu() {
            return PopulateClientMenu.AUTOMATIC;
        }
    }
}
