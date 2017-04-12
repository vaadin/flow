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

import java.io.Serializable;

import org.springframework.context.ApplicationContext;

import com.vaadin.spring.annotation.SpringViewDisplay;

/**
 * Dynamically registered bean which can provide a reference to the current view
 * display instance.
 *
 * @see SpringViewDisplay
 * @see SpringViewDisplayPostProcessor
 *
 * @author Vaadin Ltd
 */
public class SpringViewDisplayRegistrationBean implements Serializable {

    private Class<?> beanClass;
    private String beanName;

    public Object getSpringViewDisplay(ApplicationContext applicationContext) {
        if (beanName != null) {
            return applicationContext.getBean(beanName);
        } else {
            // get the bean of the correct class from the context
            return applicationContext.getBean(beanClass);
        }
    }

    /**
     * Set the class of the bean that has the view display annotation. Either
     * this method or {@link #setBeanName(String)} should be called.
     *
     * @param beanClass
     *            class of the bean that contains the SpringViewDisplay
     *            annotation or has it directly on the class
     */
    public void setBeanClass(Class<?> beanClass) {
        this.beanClass = beanClass;
    }

    /**
     * Set the name of the bean that has the view display annotation. Either
     * this method or {@link #setBeanClass(Class)} should be called.
     *
     * @param beanName
     *            name of the bean that has the SpringViewDisplay annotation
     */
    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    /**
     * Returns the view display bean class (if set).
     *
     * @return view display bean class or null if using bean name
     */
    public Class<?> getBeanClass() {
        return beanClass;
    }

    /**
     * Returns the view display bean name (if set).
     *
     * @return view display bean name or null if using bean class
     */
    public String getBeanName() {
        return beanName;
    }

}
