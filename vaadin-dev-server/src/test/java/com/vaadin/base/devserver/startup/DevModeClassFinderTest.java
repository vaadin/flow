/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.base.devserver.startup;

import jakarta.servlet.annotation.HandlesTypes;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.vaadin.base.devserver.startup.DevModeInitializer.DevModeClassFinder;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.internal.Template;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.LoadDependenciesOnStartup;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.SessionInitListener;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.frontend.TypeScriptBootstrapModifier;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DevModeClassFinderTest {

    private DevModeClassFinder classFinder = new DevModeClassFinder(
            Collections.emptySet());

    @Test
    void applicableClasses_knownClasses() {
        Collection<Class<?>> classes = getApplicableClasses();

        List<Class<?>> knownClasses = Arrays.asList(Route.class,
                UIInitListener.class, VaadinServiceInitListener.class,
                WebComponentExporter.class, WebComponentExporterFactory.class,
                NpmPackage.class, NpmPackage.Container.class, JsModule.class,
                JsModule.Container.class, JavaScript.class,
                JavaScript.Container.class, CssImport.class,
                CssImport.Container.class, Theme.class, NoTheme.class,
                HasErrorParameter.class, PWA.class, AppShellConfigurator.class,
                Template.class, LoadDependenciesOnStartup.class,
                Component.class, TypeScriptBootstrapModifier.class,
                Layout.class, StyleSheet.class, StyleSheet.Container.class);

        for (Class<?> clz : classes) {
            assertTrue(knownClasses.contains(clz),
                    "should be a known class " + clz.getName());
        }
        assertEquals(knownClasses.size(), classes.size());
    }

    @Test
    void callGetSubTypesOfByClass_expectedType_doesNotThrow() {
        for (Class<?> clazz : getApplicableClasses()) {
            classFinder.getSubTypesOf(clazz);
        }
    }

    @Test
    void callGetSubTypesOfByName_expectedType_doesNotThrow()
            throws ClassNotFoundException {
        for (Class<?> clazz : getApplicableClasses()) {
            classFinder.getSubTypesOf(clazz.getName());
        }
    }

    @Test
    void callGetgetAnnotatedClassesByName_expectedType_doesNotThrow()
            throws ClassNotFoundException {
        for (Class<?> clazz : getApplicableClasses()) {
            classFinder.getAnnotatedClasses(clazz.getName());
        }
    }

    @Test
    void callGetgetAnnotatedClassesByClass_expectedType_doesNotThrow()
            throws ClassNotFoundException {
        for (Class<?> clazz : getApplicableClasses()) {
            if (clazz.isAnnotation()) {
                classFinder.getAnnotatedClasses((Class) clazz);
            }
        }
    }

    @Test
    void callGetgetAnnotatedClassesByClass_unexpectedType_throw() {
        assertThrows(IllegalArgumentException.class,
                () -> classFinder.getAnnotatedClasses(Test.class));
    }

    @Test
    void callGetgetAnnotatedClassesByName_unexpectedType_throw()
            throws ClassNotFoundException {
        assertThrows(IllegalArgumentException.class, () -> classFinder
                .getAnnotatedClasses(ThemeDefinition.class.getName()));
    }

    @Test
    void callGetSubTypesOfByClass_unexpectedType_throw() {
        DevModeClassFinder classFinder = new DevModeClassFinder(
                Collections.emptySet());
        assertThrows(IllegalArgumentException.class,
                () -> classFinder.getSubTypesOf(Object.class));
    }

    @Test
    void callGetSubTypesOfByName_unexpectedType_throw()
            throws ClassNotFoundException {
        assertThrows(IllegalArgumentException.class, () -> classFinder
                .getSubTypesOf(SessionInitListener.class.getName()));
    }

    private Collection<Class<?>> getApplicableClasses() {
        HandlesTypes handlesTypes = DevModeStartupListener.class
                .getAnnotation(HandlesTypes.class);
        return Stream.of(handlesTypes.value()).collect(Collectors.toList());
    }
}
