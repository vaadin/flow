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
package com.vaadin.flow.spring.springnative;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.core.env.ConfigurableEnvironment;

import com.vaadin.flow.spring.VaadinConfigurationProperties;

/**
 * Computes the packages that the Vaadin AOT processors scan for types that need
 * native image hints.
 */
final class AotScanPackages {

    private AotScanPackages() {
        // Utility class
    }

    /**
     * Gets the list of packages to scan for Vaadin components.
     * <p>
     * This method returns a list of packages that includes:
     * <ul>
     * <li>The com.vaadin package</li>
     * <li>Auto-configuration packages from Spring Boot</li>
     * <li>Allowed packages from vaadin.allowed-packages configuration
     * property</li>
     * </ul>
     *
     * @param beanFactory
     *            the bean factory
     * @return set of packages to scan
     */
    static Collection<String> getPackagesToScan(
            ConfigurableListableBeanFactory beanFactory) {
        List<String> packages = new ArrayList<>();
        packages.add("com.vaadin");
        packages.addAll(AutoConfigurationPackages.get(beanFactory));

        // Add allowed packages from the configuration if set
        ConfigurableEnvironment environment = beanFactory
                .getBean(ConfigurableEnvironment.class);
        List<String> allowedPackages = VaadinConfigurationProperties
                .getAllowedPackages(environment);
        if (allowedPackages != null && !allowedPackages.isEmpty()) {
            packages.addAll(allowedPackages);
        }

        // Remove duplicates and redundant packages (e.g. ignore com.vaadin.xyz
        // if com.vaadin is already registered)
        packages.sort(Comparator.comparingInt(String::length));
        Set<String> result = new LinkedHashSet<>();
        for (String pkg : packages) {
            if (result.isEmpty() || result.stream().noneMatch(
                    registeredPkg -> pkg.startsWith(registeredPkg + "."))) {
                result.add(pkg);
            }
        }
        return result;
    }
}
