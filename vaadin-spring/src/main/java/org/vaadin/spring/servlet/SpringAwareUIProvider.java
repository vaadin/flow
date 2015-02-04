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
package org.vaadin.spring.servlet;

import com.vaadin.ui.UI;

import org.springframework.web.context.WebApplicationContext;
import org.vaadin.spring.annotation.VaadinUI;
import org.vaadin.spring.servlet.internal.AbstractSpringAwareUIProvider;

/**
 * Vaadin {@link com.vaadin.server.UIProvider} that looks up UI classes from the Spring application context. The UI
 * classes must be annotated with {@link org.vaadin.spring.annotation.VaadinUI}.
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
        final String[] uiBeanNames = getWebApplicationContext().getBeanNamesForAnnotation(VaadinUI.class);
        for (String uiBeanName : uiBeanNames) {
            Class<?> beanType = getWebApplicationContext().getType(uiBeanName);
            if (UI.class.isAssignableFrom(beanType)) {
                logger.info("Found Vaadin UI [{}]", beanType.getCanonicalName());
                final String path = getWebApplicationContext().findAnnotationOnBean(uiBeanName, VaadinUI.class).path();
                Class<? extends UI> existingBeanType = getUIByPath(path);
                if (existingBeanType != null) {
                    throw new IllegalStateException(String.format("[%s] is already mapped to the path [%s]", existingBeanType.getCanonicalName(), path));
                }
                logger.debug("Mapping Vaadin UI [{}] to path [{}]", beanType.getCanonicalName(), path);
                mapPathToUI(path, (Class<? extends UI>) beanType);
            }
        }
    }

}
