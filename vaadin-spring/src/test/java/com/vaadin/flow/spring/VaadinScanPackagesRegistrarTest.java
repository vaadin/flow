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

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;

import com.vaadin.flow.spring.VaadinScanPackagesRegistrar.VaadinScanPackages;
import com.vaadin.flow.spring.annotation.EnableVaadin;

import static org.assertj.core.api.Assertions.assertThat;

class VaadinScanPackagesRegistrarTest {

    @Test
    void multipleEnableVaadinAnnotations_packagesMergedWithoutDuplicates() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            // Spring Boot does not allow bean definition overriding
            context.setAllowBeanDefinitionOverriding(false);
            context.register(ServiceAConfiguration.class,
                    ServiceBConfiguration.class);
            context.refresh();

            assertThat(context.getBean(VaadinScanPackages.class).scanPackages())
                    .containsExactly("pkg.service.a.views",
                            "pkg.service.b.views", "pkg.shared.views");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableVaadin("pkg.service.a.views")
    static class ServiceAConfiguration {
    }

    @Configuration(proxyBeanMethods = false)
    @EnableVaadin({ "pkg.service.b.views", "pkg.service.a.views",
            "pkg.shared.views" })
    static class ServiceBConfiguration {
    }
}
