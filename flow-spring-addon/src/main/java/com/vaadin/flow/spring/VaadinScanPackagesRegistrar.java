/*
 * Copyright 2000-2017 Vaadin Ltd.
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;

import com.vaadin.flow.spring.annotation.VaadinScan;

/**
 * Internal registrar for Vaadin scan packages settings.
 * 
 * @author Vaadin Ltd
 *
 */
public class VaadinScanPackagesRegistrar
        implements ImportBeanDefinitionRegistrar {

    static class VaadinScanPackages {

        private final List<String> scanPackages;

        private VaadinScanPackages(String[] scanPackages) {
            this.scanPackages = Collections
                    .unmodifiableList(Arrays.asList(scanPackages));
        }

        List<String> getScanPackages() {
            return scanPackages;
        }

    }

    @Override
    public void registerBeanDefinitions(AnnotationMetadata annotationMetadata,
            BeanDefinitionRegistry registry) {
        String annotationName = VaadinScan.class.getName();
        if (annotationMetadata.hasAnnotation(annotationName)) {
            String[] packages = (String[]) annotationMetadata
                    .getAnnotationAttributes(annotationName).get("value");
            GenericBeanDefinition beanDefinition = new GenericBeanDefinition();
            beanDefinition.setBeanClass(VaadinScanPackages.class);
            beanDefinition.getConstructorArgumentValues()
                    .addIndexedArgumentValue(0, packages);
            beanDefinition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
            registry.registerBeanDefinition(VaadinScanPackages.class.getName(),
                    beanDefinition);
        }
    }

}
