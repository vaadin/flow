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

import java.util.Locale;

import org.springframework.core.env.Environment;

import com.vaadin.flow.shared.util.SharedUtil;

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

    /**
     * Reads the value of a Vaadin parameter from the Spring environment, where
     * it is set with the {@code vaadin.} prefix.
     * <p>
     * The dash-separated form of the name (e.g.
     * {@code vaadin.devmode.session-serialization.enabled}) is looked up first,
     * then the name as is (e.g.
     * {@code vaadin.devmode.sessionSerialization.enabled}). Spring Boot only
     * applies relaxed binding to lower case names, so the dash-separated lookup
     * is the one that finds every supported spelling of the property.
     *
     * @param env
     *            the Spring environment
     * @param parameterName
     *            the name of the Vaadin parameter, without the {@code vaadin.}
     *            prefix
     * @return the value of the property, or {@code null} if it is not set
     */
    static String getVaadinProperty(Environment env, String parameterName) {
        String property = "vaadin." + parameterName;
        String value = env.getProperty(upperCaseToDashSeparated(property));
        if (value == null) {
            value = env.getProperty(property);
        }
        return value;
    }

    private static String upperCaseToDashSeparated(String value) {
        String result = value;
        while (!result.equals(result.toLowerCase(Locale.ENGLISH))) {
            result = SharedUtil.camelCaseToDashSeparated(result);
        }
        return result;
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
