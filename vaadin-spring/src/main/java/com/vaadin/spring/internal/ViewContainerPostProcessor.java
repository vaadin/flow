/*
 * Copyright 2015-2016 The original authors
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

import java.lang.reflect.Field;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionValidationException;
import org.springframework.util.ReflectionUtils;
import org.springframework.util.ReflectionUtils.FieldCallback;

import com.vaadin.spring.annotation.ViewContainer;

public class ViewContainerPostProcessor implements BeanPostProcessor {
    private Object viewContainer = null;

    @Override
    public Object postProcessAfterInitialization(final Object bean,
            String beanName) throws BeansException {
        ReflectionUtils.doWithFields(bean.getClass(), new FieldCallback() {
            @Override
            public void doWith(Field field)
                    throws IllegalArgumentException, IllegalAccessException {
                if (field.isAnnotationPresent(ViewContainer.class)) {
                    if (viewContainer == null) {
                        field.setAccessible(true);
                        viewContainer = field.get(bean);
                        field.setAccessible(false);
                    } else {
                        throw new BeanDefinitionValidationException(
                                "Multiple definitions of @"
                                        + ViewContainer.class.getSimpleName()
                                        + " on fields, including "
                                        + bean.getClass() + "."
                                        + field.getName());
                    }
                }
            }
        });
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName)
            throws BeansException {
        return bean;
    }

    public Object getViewContainer() {
        return viewContainer;
    }
}