/*
 * Copyright 2015 The original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vaadin.spring.servlet;

import org.springframework.web.context.WebApplicationContext;

import com.vaadin.spring.annotation.SpringUI;
import com.vaadin.spring.internal.Conventions;
import com.vaadin.spring.servlet.internal.AbstractSpringAwareUIProvider;
import com.vaadin.ui.UI;

/**
 * Vaadin {@link com.vaadin.server.UIProvider} that looks up UI classes from the
 * Spring application context. The UI classes must be annotated with
 * {@link com.vaadin.spring.annotation.SpringUI}.
 *
 * @author Petter Holmström (petter@vaadin.com)
 */
public class SpringAwareUIProvider extends AbstractSpringAwareUIProvider {

    private static final long serialVersionUID = 6954428459733726004L;

    public SpringAwareUIProvider(WebApplicationContext webApplicationContext) {
        super(webApplicationContext);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void detectUIs() {
        logger.info("Checking the application context for Vaadin UIs");
        final String[] uiBeanNames = getWebApplicationContext()
                .getBeanNamesForAnnotation(SpringUI.class);
        for (String uiBeanName : uiBeanNames) {
            Class<?> beanType = getWebApplicationContext().getType(uiBeanName);
            if (UI.class.isAssignableFrom(beanType)) {
                logger.info("Found Vaadin UI [{}]", beanType.getCanonicalName());
                SpringUI annotation = getWebApplicationContext()
                        .findAnnotationOnBean(uiBeanName, SpringUI.class);
                final String path;
                String tempPath = deriveMappingForUI(beanType, annotation);
                if (tempPath.length() > 0 && !tempPath.startsWith("/")) {
                    path = "/".concat(tempPath);
                } else {
                    path = tempPath;
                }
                Class<? extends UI> existingBeanType = getUIByPath(path);
                if (existingBeanType != null) {
                    throw new IllegalStateException(String.format(
                            "[%s] is already mapped to the path [%s]",
                            existingBeanType.getCanonicalName(), path));
                }
                logger.debug("Mapping Vaadin UI [{}] to path [{}]",
                        beanType.getCanonicalName(), path);
                mapPathToUI(path, (Class<? extends UI>) beanType);
            }
        }
    }

    /**
     * Derive the name (path) for a UI based on its class name and annotation
     * parameters.
     *
     * If a path is given as a parameter for the annotation, it is used. An
     * empty string maps to the root context. If the special (default) value
     * {@link SpringUI#USE_CONVENTIONS} is used, the path is generated from the
     * UI class name.
     *
     * This method can be overridden to disable the conventions based mapping
     * and map the default value to the root context.
     *
     * @param beanType
     *            class of the UI bean
     * @param annotation
     *            the {@link SpringUI} annotation of the UI bean
     * @return path to map the UI to
     */
    protected String deriveMappingForUI(Class<?> beanType, SpringUI annotation) {
        return Conventions.deriveMappingForUI(beanType, annotation);
    }

}
