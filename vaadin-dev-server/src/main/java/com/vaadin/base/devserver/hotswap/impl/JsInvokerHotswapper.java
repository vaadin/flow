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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.base.devserver.hotswap.HotswapClassEvent;
import com.vaadin.base.devserver.hotswap.VaadinHotswapper;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.js.JsExpression;
import com.vaadin.flow.js.JsInvoker;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.frontend.Options;
import com.vaadin.flow.server.frontend.TaskGenerateJsInvokers;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

/**
 * Reports a {@link JsInvoker} interface whose JavaScript the frontend bundle
 * does not carry.
 * <p>
 * The JavaScript an invoker method declares with {@link JsExpression} is
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
 * so the interface name, the method names, their argument counts and the
 * declared JavaScript all have to match for an interface to pass silently.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class JsInvokerHotswapper implements VaadinHotswapper {

    @Override
    public void onClassesChange(HotswapClassEvent event) {
        List<Class<?>> invokers = event.getChangedClasses().stream()
                .filter(type -> type.isAnnotationPresent(JsInvoker.class))
                .toList();
        if (invokers.isEmpty()) {
            return;
        }

        VaadinService service = event.getVaadinService();
        File generatedFile = generatedInvokersFile(service);
        String generated = readGeneratedInvokers(generatedFile);

        List<Class<?>> stale = invokers.stream()
                .filter(invoker -> !isInBundle(invoker, generated)).toList();
        if (stale.isEmpty()) {
            return;
        }

        String applied = hotApply(service, generatedFile, generated, invokers);
        // What the file holds now is what a browser can run, so anything the
        // rendering did not cover is still a change nobody can apply
        List<String> unresolved = stale.stream()
                .filter(invoker -> !isInBundle(invoker, applied))
                .map(Class::getName).toList();
        if (unresolved.isEmpty()) {
            getLogger().debug(
                    "Wrote the JavaScript declared by {} to {}, which the frontend dev server replaces in the browser",
                    stale.stream().map(Class::getName).toList(), generatedFile);
        } else {
            report(unresolved);
        }
    }

    /**
     * Writes the generated file again from what the invoker interfaces declare,
     * so the frontend dev server can replace the module in the browser.
     * <p>
     * Only with the dev server running: what a browser has without it is a
     * bundle, which this can not replace. The file is left alone when its
     * content would not change, so the dev server is not told about an update
     * that is not one.
     *
     * @return the content the file holds afterwards, which is the content it
     *         held already when nothing could be written
     */
    private static String hotApply(VaadinService service, File generatedFile,
            String generated, List<Class<?>> changedInvokers) {
        ApplicationConfiguration configuration = ApplicationConfiguration
                .get(service.getContext());
        if (generatedFile == null || configuration == null || configuration
                .getMode() != Mode.DEVELOPMENT_FRONTEND_LIVERELOAD) {
            return generated;
        }
        try {
            // Written by the task that generates it during a build, with the
            // interfaces it has to hold passed in: the changed classes are at
            // hand here, so nothing has to scan the class path for them
            return TaskGenerateJsInvokers.writeJsInvokers(
                    buildOptions(service, configuration),
                    invokersToRender(generated, changedInvokers));
        } catch (RuntimeException e) {
            getLogger().debug("Could not write {}", generatedFile, e);
            return generated;
        }
    }

    /**
     * The least an invoker file needs to be written: where the project is and
     * where its frontend folder is. No class finder, since what the file has to
     * hold is passed in rather than scanned for.
     */
    private static Options buildOptions(VaadinService service,
            ApplicationConfiguration configuration) {
        return new Options(service.getContext().getAttribute(Lookup.class),
                null, configuration.getProjectFolder())
                .withFrontendDirectory(configuration.getFrontendFolder());
    }

    /**
     * The interfaces the file has to hold: the ones it holds already, since
     * those are what the browser can run and none of them changed, plus the
     * ones that just changed - which is also how an interface that was only now
     * annotated gets in, without anything having scanned for it.
     * <p>
     * An interface the file holds and the application no longer has is left
     * out, and one whose annotation was removed keeps its functions in the file
     * with nothing calling them, until a build renders it again.
     */
    private static Collection<Class<?>> invokersToRender(String generated,
            List<Class<?>> changedInvokers) {
        Map<String, Class<?>> byName = new LinkedHashMap<>();
        changedInvokers
                .forEach(invoker -> byName.put(invoker.getName(), invoker));
        ClassLoader classLoader = changedInvokers.get(0).getClassLoader();
        for (String name : TaskGenerateJsInvokers.readInvokerNames(generated)) {
            if (byName.containsKey(name)) {
                continue;
            }
            try {
                byName.put(name, Class.forName(name, false, classLoader));
            } catch (ClassNotFoundException | LinkageError e) {
                getLogger().debug("Could not load the invoker {}", name, e);
            }
        }
        return byName.values();
    }

    /**
     * Says that the bundle does not carry what the given interfaces declare.
     * <p>
     * Package-private so that what a change is reported for can be asserted.
     *
     * @param invokerNames
     *            the names of the invoker interfaces to report, never empty
     */
    void report(List<String> invokerNames) {
        getLogger().warn(
                "The JavaScript declared by {} is not the JavaScript the frontend bundle carries. "
                        + "It is collected into the bundle when the frontend is built, so a call made through the invoker keeps running the previous version, or finds no function at all, until the application is restarted.",
                String.join(", ", invokerNames));
    }

    /**
     * Whether the generated file carries what the invoker declares, compared as
     * the build renders it. A method that was removed does not show up as a
     * difference: its function stays in the bundle with nothing calling it.
     */
    private static boolean isInBundle(Class<?> invoker, String generated) {
        if (generated == null) {
            // Nothing carries the declarations, so nothing matches them
            return false;
        }
        List<String> declared = TaskGenerateJsInvokers
                .renderInvokerLines(invoker);
        if (declared.isEmpty()) {
            // Declares no JavaScript, so there is nothing to carry
            return true;
        }
        return generated
                .contains(String.join(System.lineSeparator(), declared));
    }

    private static File generatedInvokersFile(VaadinService service) {
        File frontendFolder = FrontendUtils
                .getProjectFrontendDir(service.getDeploymentConfiguration());
        if (frontendFolder == null) {
            return null;
        }
        return new File(
                FrontendUtils.getFrontendGeneratedFolder(frontendFolder),
                FrontendUtils.JS_INVOKERS_FILE_NAME);
    }

    /**
     * Reads the generated file from the frontend folder, which is the file the
     * dev server reads and this class writes, so what is compared and what is
     * written are the same bytes. Fetching it from the dev server instead would
     * answer with the module as it transforms it, which is not what a
     * declaration renders to.
     */
    private static String readGeneratedInvokers(File generatedFile) {
        if (generatedFile == null || !generatedFile.exists()) {
            return null;
        }
        try {
            return Files.readString(generatedFile.toPath(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            getLogger().debug("Could not read {}", generatedFile, e);
            return null;
        }
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(JsInvokerHotswapper.class);
    }
}
