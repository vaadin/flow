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
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.base.devserver.hotswap.HotswapClassEvent;
import com.vaadin.base.devserver.hotswap.VaadinHotswapper;
import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.dom.JsExpression;
import com.vaadin.flow.dom.JsInvoker;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.server.Mode;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.frontend.TaskGenerateJsInvokers;
import com.vaadin.flow.server.frontend.scanner.ClassFinder;
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
        ApplicationConfiguration configuration = ApplicationConfiguration
                .get(service.getContext());
        File generatedFile = generatedInvokersFile(configuration);
        String generated = readGeneratedInvokers(generatedFile);

        List<String> stale = new ArrayList<>();
        for (Class<?> invoker : invokers) {
            if (!isInBundle(invoker, generated)) {
                stale.add(invoker.getName());
            }
        }
        if (stale.isEmpty()) {
            return;
        }

        if (regenerate(service, configuration, generatedFile)) {
            getLogger().debug(
                    "Wrote the JavaScript declared by {} to {}, which the frontend dev server replaces in the browser",
                    String.join(", ", stale), generatedFile);
        } else {
            report(stale);
        }
    }

    /**
     * Writes what every invoker interface declares to the generated file, so
     * the frontend dev server can replace the module in the browser.
     * <p>
     * Only with the dev server running: what a browser has without it is a
     * bundle, which this can not replace. The file is left alone when its
     * content would not change, so the dev server is not told about an update
     * that is not one.
     *
     * @return whether the file now holds what the interfaces declare
     */
    private static boolean regenerate(VaadinService service,
            ApplicationConfiguration configuration, File generatedFile) {
        if (generatedFile == null || configuration == null || configuration
                .getMode() != Mode.DEVELOPMENT_FRONTEND_LIVERELOAD) {
            return false;
        }
        Lookup lookup = service.getContext().getAttribute(Lookup.class);
        ClassFinder classFinder = lookup == null ? null
                : lookup.lookup(ClassFinder.class);
        if (classFinder == null) {
            return false;
        }
        try {
            String content = TaskGenerateJsInvokers.fileContent(
                    classFinder.getAnnotatedClasses(JsInvoker.class));
            if (generatedFile.exists()
                    && content.equals(Files.readString(generatedFile.toPath(),
                            StandardCharsets.UTF_8))) {
                return true;
            }
            Files.createDirectories(generatedFile.toPath().getParent());
            Files.writeString(generatedFile.toPath(), content,
                    StandardCharsets.UTF_8);
            return true;
        } catch (IOException | RuntimeException e) {
            getLogger().debug("Could not write {}", generatedFile, e);
            return false;
        }
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
        List<String> declared = TaskGenerateJsInvokers.invokerLines(invoker);
        if (declared.isEmpty()) {
            // Declares no JavaScript, so there is nothing to carry
            return true;
        }
        return generated
                .contains(String.join(System.lineSeparator(), declared));
    }

    private static File generatedInvokersFile(
            ApplicationConfiguration configuration) {
        if (configuration == null) {
            return null;
        }
        File frontendFolder = FrontendUtils
                .getProjectFrontendDir(configuration);
        if (frontendFolder == null) {
            return null;
        }
        return new File(
                FrontendUtils.getFrontendGeneratedFolder(frontendFolder),
                FrontendUtils.JS_INVOKERS_FILE_NAME);
    }

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
