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
package com.vaadin.base.devserver.hotswap.impl;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.base.devserver.hotswap.HotswapClassEvent;
import com.vaadin.base.devserver.hotswap.VaadinHotswapper;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.internal.ReflectTools;
import com.vaadin.flow.internal.ReflectionCache;
import com.vaadin.flow.js.JsDefinition;
import com.vaadin.flow.js.JsDefinitionProxy;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsFunctionParameterNames;
import com.vaadin.flow.js.JsSnippet;
import com.vaadin.flow.server.AbstractConfiguration;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.frontend.Options;
import com.vaadin.flow.server.frontend.TaskGenerateJsDefinitions;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

/**
 * Reports a {@link JsDefinition} interface whose JavaScript the frontend bundle
 * does not carry.
 * <p>
 * The JavaScript a definition method declares with {@link JsExpression} is
 * collected into the bundle when the frontend is built. Redefining the
 * interface therefore does not change what the browser can run: a call made
 * after the change either runs the JavaScript the bundle was built with, or
 * finds no function at all when the redefinition renamed or added a method.
 * <p>
 * With the frontend dev server running, the file the functions are generated
 * into is written again from what the interfaces now declare. The dev server
 * replaces the module in every browser that has it, the file registers the new
 * functions, and a call made afterwards runs them - no restart, and nothing is
 * compiled from a string in the browser, since the dev server serves the file
 * it just read. Without the dev server, a bundle is what the browser runs and
 * only a build produces a new one, so the change is reported instead.
 * <p>
 * The comparison is against the generated file the bundle was built from, which
 * is what the browser can run, and it uses the same rendering the build wrote,
 * so the declared JavaScript of every method and the number of arguments it
 * takes have to match for an interface to pass silently.
 * <p>
 * A snippet type of the application, an interface that extends
 * {@link JsSnippet}, can declare with {@link JsFunctionParameterNames} what the
 * functions of the definition methods returning it are called with. Redefining
 * such a type changes the JavaScript of those definitions, so they are written
 * again or reported as if they had been redefined themselves. They are found
 * among the definitions the generated file names, which is every one a browser
 * can run.
 * <p>
 * The methods of an interface that a definition extends, annotated or not, are
 * generated as part of every definition that extends it. Redefining such an
 * interface is handled the same way: the definitions the generated file names
 * that extend it are written again or reported.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 * 
 * @since 25.4
 */
public class JsDefinitionHotswapper implements VaadinHotswapper {

    @Override
    public void onClassesChange(HotswapClassEvent event) {
        Set<Class<?>> changed = event.getChangedClasses();
        List<Class<?>> definitions = new ArrayList<>(changed.stream()
                .filter(JsDefinitionProxy::isDefinition).toList());
        List<Class<?>> snippetTypes = changed.stream()
                .filter(JsDefinitionProxy::isApplicationSnippetType).toList();
        List<Class<?>> inheritedTypes = changed.stream()
                .filter(JsDefinitionHotswapper::declaresJavaScript).toList();
        if (definitions.isEmpty() && snippetTypes.isEmpty()
                && inheritedTypes.isEmpty()) {
            return;
        }

        VaadinService service = event.getVaadinService();
        Options options = buildOptions(service);

        for (Class<?> definition : findAffectedDefinitions(options,
                snippetTypes, inheritedTypes)) {
            // Not redefined itself, so what was cached of it, such as the
            // identifiers of its functions, is not cleared after the
            // hotswappers run and would still be what the type declared before
            ReflectionCache.clearAll(definition);
            if (!definitions.contains(definition)) {
                definitions.add(definition);
            }
        }
        if (definitions.isEmpty()) {
            return;
        }

        List<Class<?>> missing;
        if (canReplaceInTheBrowser(service)) {
            // Writing the file again is what the browser runs afterwards, and
            // the write leaves the file alone when nothing it holds changed,
            // so what comes back is what could not be applied
            missing = TaskGenerateJsDefinitions.updateJsDefinitions(options,
                    definitions);
        } else {
            // What a browser has without the dev server is a bundle, which
            // only a build produces, so a change can only be reported
            missing = TaskGenerateJsDefinitions
                    .findMissingFromGeneratedFile(options, definitions);
        }

        if (!missing.isEmpty()) {
            warnAboutMissingDefinitions(missing);
        }
    }

    /**
     * Checks whether the given type is an interface with JavaScript of its own
     * or inherited, which is generated as part of the definitions that extend
     * it, whether or not it is a definition itself.
     */
    private static boolean declaresJavaScript(Class<?> type) {
        return type.isInterface() && ReflectTools.getCurrentPublicMethods(type)
                .stream().anyMatch(method -> method
                        .isAnnotationPresent(JsExpression.class));
    }

    /**
     * The definitions named by the generated file that have a method returning
     * one of the given snippet types or that extend one of the given
     * interfaces. A name that can not be loaded is skipped: it is a definition
     * that is no longer on the class path.
     */
    private static List<Class<?>> findAffectedDefinitions(Options options,
            List<Class<?>> snippetTypes, List<Class<?>> inheritedTypes) {
        if (snippetTypes.isEmpty() && inheritedTypes.isEmpty()) {
            return List.of();
        }
        List<Class<?>> relatedTypes = new ArrayList<>(snippetTypes);
        relatedTypes.addAll(inheritedTypes);
        List<Class<?>> definitions = new ArrayList<>();
        for (String name : TaskGenerateJsDefinitions
                .findDefinitionNamesInGeneratedFile(options)) {
            Class<?> definition = load(name, relatedTypes);
            if (definition != null && (Arrays.stream(definition.getMethods())
                    .map(Method::getReturnType).anyMatch(snippetTypes::contains)
                    || inheritedTypes.stream()
                            .anyMatch(type -> type != definition
                                    && type.isAssignableFrom(definition)))) {
                definitions.add(definition);
            }
        }
        return definitions;
    }

    /**
     * Loads a definition with the loader of one of the types it may be related
     * to, or with the context class loader, which is what loads the application
     * when the definition comes from it and the related type from a library.
     */
    private static Class<?> load(String name, List<Class<?>> relatedTypes) {
        List<ClassLoader> loaders = new ArrayList<>();
        loaders.add(Thread.currentThread().getContextClassLoader());
        relatedTypes.forEach(type -> loaders.add(type.getClassLoader()));
        for (ClassLoader loader : loaders) {
            if (loader == null) {
                continue;
            }
            try {
                return Class.forName(name, false, loader);
            } catch (ClassNotFoundException | LinkageError e) {
                getLogger().debug("Could not load {} with {}", name, loader, e);
            }
        }
        return null;
    }

    private static boolean canReplaceInTheBrowser(VaadinService service) {
        ApplicationConfiguration configuration = ApplicationConfiguration
                .get(service.getContext());
        return configuration != null && configuration
                .getMode() == Mode.DEVELOPMENT_FRONTEND_LIVERELOAD;
    }

    /**
     * The least the generated file needs to be read and written: where the
     * project is and where its frontend folder is. No class finder, since what
     * the file has to hold is passed in rather than scanned for.
     */
    private static Options buildOptions(VaadinService service) {
        AbstractConfiguration configuration = service
                .getDeploymentConfiguration();
        return new Options(service.getContext().getAttribute(Lookup.class),
                null, configuration.getProjectFolder()).withFrontendDirectory(
                        FrontendUtils.getProjectFrontendDir(configuration));
    }

    /**
     * Warns that the bundle does not carry what the given interfaces declare.
     * <p>
     * Package-private so that what a change is warned about can be asserted.
     *
     * @param definitions
     *            the JavaScript definitions the bundle does not carry, never
     *            empty
     */
    void warnAboutMissingDefinitions(List<Class<?>> definitions) {
        getLogger().warn(
                "The JavaScript declared by {} is not the JavaScript the frontend bundle carries. "
                        + "It is collected into the bundle when the frontend is built, so a call made through the definition keeps running the previous version, or finds no function at all, until the application is restarted.",
                definitions.stream().map(Class::getName)
                        .collect(Collectors.joining(", ")));
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(JsDefinitionHotswapper.class);
    }
}
