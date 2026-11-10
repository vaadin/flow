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
package com.vaadin.flow.spring;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import com.vaadin.flow.spring.annotation.EnableVaadin;

/**
 * Internal registrar for Vaadin scan packages settings.
 *
 * @author Vaadin Ltd
 *
 * @since 10.0
 */
public class VaadinScanPackagesRegistrar
        implements ImportBeanDefinitionRegistrar {

    /**
     * Creates the registrar. Spring instantiates it.
     */
    public VaadinScanPackagesRegistrar() {
        // Default constructor
    }

    record VaadinScanPackages(List<String> scanPackages) {
    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata,
            BeanDefinitionRegistry registry) {
        String[] packages = getPackages(annotationMetadata, EnableVaadin.class,
                "value");
        if (packages.length == 0) {
            return;
        }
        String beanName = VaadinScanPackages.class.getName();
        Set<String> scanPackages = new LinkedHashSet<>();
        BeanDefinition beanDefinition;
        if (registry.containsBeanDefinition(beanName)) {
            // Another @EnableVaadin annotation has registered the bean
            // already, so its packages are kept and the new ones added
            beanDefinition = registry.getBeanDefinition(beanName);
            @SuppressWarnings("unchecked")
            List<String> registeredPackages = (List<String>) beanDefinition
                    .getConstructorArgumentValues()
                    .getIndexedArgumentValue(0, List.class).getValue();
            scanPackages.addAll(registeredPackages);
        } else {
            GenericBeanDefinition newBeanDefinition = new GenericBeanDefinition();
            newBeanDefinition.setBeanClass(VaadinScanPackages.class);
            newBeanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            registry.registerBeanDefinition(beanName, newBeanDefinition);
            beanDefinition = newBeanDefinition;
        }
        scanPackages.addAll(Arrays.asList(packages));
        beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0,
                List.copyOf(scanPackages));
    }

    private <T> T getPackages(Class<T> clazz,
            AnnotationMetadata annotationMetadata,
            Class<? extends Annotation> annotation, String getterName) {
        String annotationName = annotation.getName();
        if (annotationMetadata.hasAnnotation(annotationName)) {
            Map<String, Object> annotationAttributes = annotationMetadata
                    .getAnnotationAttributes(annotationName);
            return annotationAttributes != null
                    ? clazz.cast(annotationAttributes.get(getterName))
                    : null;
        }
        return null;
    }

    private String[] getPackages(AnnotationMetadata annotationMetadata,
            Class<? extends Annotation> annotation, String getterName) {
        String[] packages = getPackages(String[].class, annotationMetadata,
                annotation, getterName);
        if (packages == null) {
            return new String[0];
        }
        return packages;
    }

}
