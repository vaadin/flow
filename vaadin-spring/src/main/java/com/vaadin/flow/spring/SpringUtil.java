/*
 * Copyright 2000-2025 Vaadin Ltd.
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

/** Helpers related to Spring. */
public class SpringUtil {

    /**
     * Checks if this is Spring Boot and not plain Spring.
     *
     * @return true if this is Spring Boot, false if it is Spring without Boot
     */
    public static boolean isSpringBoot() {
        Class<?> resourcesClass = resolveClass(
                SpringVaadinServletService.SPRING_BOOT_WEBPROPERTIES_CLASS);
        return (resourcesClass != null);
    }

    private static Class<?> resolveClass(String clazzName) {
        try {
            return Class.forName(clazzName, false,
                    SpringVaadinServletService.class.getClassLoader());
        } catch (LinkageError | ClassNotFoundException e) {
            return null;
        }
    }

}
