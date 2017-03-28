/*
 * Copyright 2015-2017 The original authors
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
package com.vaadin.spring.internal;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import com.vaadin.ui.Component;
import com.vaadin.ui.declarative.Design.DefaultComponentFactory;
import com.vaadin.ui.declarative.DesignContext;

/**
 * A component factory for Vaadin design files, capable of creating Spring
 * beans.
 */
public class VaadinSpringComponentFactory extends DefaultComponentFactory
        implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    public VaadinSpringComponentFactory() {
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public Component createComponent(String fullyQualifiedClassName,
            DesignContext context) {
        Class<? extends Component> componentClass = resolveComponentClass(
                fullyQualifiedClassName, context);

        Component managedComponent = SpringBeanUtil
                .createManagedBeanIfAvailable(applicationContext,
                        componentClass);
        if (managedComponent != null) {
            return managedComponent;
        }

        return super.createComponent(fullyQualifiedClassName, context);
    }

}
