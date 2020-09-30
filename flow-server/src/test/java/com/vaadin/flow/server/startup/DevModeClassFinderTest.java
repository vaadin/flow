/*
 * Copyright 2000-2020 Vaadin Ltd.
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

import javax.servlet.annotation.HandlesTypes;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;

import com.vaadin.flow.component.WebComponentExporter;
import com.vaadin.flow.component.WebComponentExporterFactory;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.SessionInitListener;
import com.vaadin.flow.server.UIInitListener;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.server.startup.DevModeInitializer.DevModeClassFinder;
import com.vaadin.flow.theme.NoTheme;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.ThemeDefinition;

import static org.junit.Assert.assertTrue;

public class DevModeClassFinderTest {

    private DevModeClassFinder classFinder = new DevModeClassFinder(
            Collections.emptySet());

    @Test
    public void applicableClasses_knownClasses() {
        Collection<Class<?>> classes = getApplicableClasses();

        List<Class<?>> knownClasses = Arrays.asList(
            Route.class,
            UIInitListener.class,
            VaadinServiceInitListener.class,
            WebComponentExporter.class,
            WebComponentExporterFactory.class,
            NpmPackage.class,
            NpmPackage.Container.class,
            JsModule.class,
            JsModule.Container.class,
            JavaScript.class,
            JavaScript.Container.class,
            CssImport.class,
            CssImport.Container.class,
            Theme.class,
            NoTheme.class,
            HasErrorParameter.class,
            PWA.class);

        for (Class<?> clz : classes) {
            assertTrue("should be a known class " + clz.getName(), knownClasses.contains(clz));
        }
        Assert.assertEquals(knownClasses.size(), classes.size());
    }

    @Test
    public void callGetSubTypesOfByClass_expectedType_doesNotThrow() {
        for (Class<?> clazz : getApplicableClasses()) {
            classFinder.getSubTypesOf(clazz);
        }
    }

    @Test
    public void callGetSubTypesOfByName_expectedType_doesNotThrow()
            throws ClassNotFoundException {
        for (Class<?> clazz : getApplicableClasses()) {
            classFinder.getSubTypesOf(clazz.getName());
        }
    }

    @Test
    public void callGetgetAnnotatedClassesByName_expectedType_doesNotThrow()
            throws ClassNotFoundException {
        for (Class<?> clazz : getApplicableClasses()) {
            classFinder.getAnnotatedClasses(clazz.getName());
        }
    }

    @Test
    public void callGetgetAnnotatedClassesByClass_expectedType_doesNotThrow()
            throws ClassNotFoundException {
        for (Class<?> clazz : getApplicableClasses()) {
            if (clazz.isAnnotation()) {
                classFinder.getAnnotatedClasses((Class) clazz);
            }
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void callGetgetAnnotatedClassesByClass_unexpectedType_throw() {
        classFinder.getAnnotatedClasses(Test.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void callGetgetAnnotatedClassesByName_unexpectedType_throw()
            throws ClassNotFoundException {
        classFinder.getAnnotatedClasses(ThemeDefinition.class.getName());
    }

    @Test(expected = IllegalArgumentException.class)
    public void callGetSubTypesOfByClass_unexpectedType_throw() {
        DevModeClassFinder classFinder = new DevModeClassFinder(
                Collections.emptySet());
        classFinder.getSubTypesOf(Object.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void callGetSubTypesOfByName_unexpectedType_throw()
            throws ClassNotFoundException {
        classFinder.getSubTypesOf(SessionInitListener.class.getName());
    }

    private Collection<Class<?>> getApplicableClasses() {
        HandlesTypes handlesTypes = DevModeInitializer.class
                .getAnnotation(HandlesTypes.class);
        return Stream.of(handlesTypes.value()).collect(Collectors.toList());
    }
}
