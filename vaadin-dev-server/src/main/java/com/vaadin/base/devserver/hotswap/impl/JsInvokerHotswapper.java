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
import java.io.Serializable;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.base.devserver.hotswap.HotswapClassEvent;
import com.vaadin.base.devserver.hotswap.VaadinHotswapper;
import com.vaadin.flow.dom.JsExpression;
import com.vaadin.flow.dom.JsInvoker;
import com.vaadin.flow.dom.JsInvokerCall;
import com.vaadin.flow.internal.FrontendUtils;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.startup.ApplicationConfiguration;

/**
 * Reports a {@link JsInvoker} interface whose JavaScript the frontend bundle no
 * longer carries.
 * <p>
 * The JavaScript an invoker method declares with {@link JsExpression} is
 * collected into the bundle when the frontend is built. Redefining the
 * interface therefore does not change what the browser can run: a call made
 * after the change either runs the JavaScript the bundle was built with, or
 * finds no function at all when the redefinition added or renamed a method. The
 * dev loop escalates to a restart for this, but a class redefined straight from
 * an IDE reaches the application without it, and there is nothing this
 * hotswapper could apply in the browser instead - only a frontend build
 * produces the new function. So it says what happened, and what to do about it.
 * <p>
 * What the browser can run is what the generated file holds, so that file is
 * what the declarations are compared against, and an interface whose JavaScript
 * is already in there passes silently.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 */
public class JsInvokerHotswapper implements VaadinHotswapper, Serializable {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(JsInvokerHotswapper.class);

    @Override
    public void onClassesChange(HotswapClassEvent event) {
        List<Class<?>> invokers = event.getChangedClasses().stream()
                .filter(type -> type.isAnnotationPresent(JsInvoker.class))
                .toList();
        if (invokers.isEmpty()) {
            return;
        }

        String generated = readGeneratedInvokers(event.getVaadinService());
        List<String> stale = new ArrayList<>();
        for (Class<?> invoker : invokers) {
            if (!isInBundle(invoker, generated)) {
                stale.add(invoker.getName());
            }
        }
        if (stale.isEmpty()) {
            return;
        }

        LOGGER.warn(
                "The JavaScript declared by {} is not the JavaScript the frontend bundle carries. "
                        + "It is collected into the bundle when the frontend is built, so calls made through the invoker keep running the previous version, or fail to find a function at all, until the application is restarted.",
                String.join(", ", stale));
    }

    /**
     * Whether everything the invoker declares can be found in the generated
     * file. A method that was removed is not reported: the function stays in
     * the bundle with nothing calling it.
     */
    // Package-private so the comparison can be asserted directly.
    static boolean isInBundle(Class<?> invoker, String generated) {
        if (generated == null) {
            // Nothing to compare against, so anything declared may be missing
            return false;
        }
        for (Method method : invoker.getMethods()) {
            JsExpression expression = method.getAnnotation(JsExpression.class);
            if (expression == null) {
                continue;
            }
            String methodId = JsInvokerCall.methodId(method.getName(),
                    method.getParameterCount());
            if (!generated.contains("\"" + methodId + "\"")
                    || !generated.contains(expression.value())) {
                return false;
            }
        }
        return true;
    }

    private static String readGeneratedInvokers(VaadinService service) {
        ApplicationConfiguration configuration = ApplicationConfiguration
                .get(service.getContext());
        if (configuration == null) {
            return null;
        }
        File frontendFolder = FrontendUtils
                .getProjectFrontendDir(configuration);
        if (frontendFolder == null) {
            return null;
        }
        File generated = new File(
                new File(frontendFolder, FrontendUtils.GENERATED),
                FrontendUtils.JS_INVOKERS_FILE_NAME);
        if (!generated.exists()) {
            return null;
        }
        try {
            return Files.readString(generated.toPath(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.debug("Could not read {}", generated, e);
            return null;
        }
    }
}
