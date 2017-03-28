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

import org.springframework.util.ClassUtils;

import com.vaadin.spring.annotation.SpringView;

/**
 * Internal utility class for deriving automatic mappings for view names.
 *
 * @author Henri Sara (hesara@vaadin.com)
 */
public final class Conventions {

    private Conventions() {
        // utility class, no instances of this class should be created
    }

    public static String deriveMappingForView(Class<?> beanClass,
            SpringView annotation) {
        if (annotation != null
                && !SpringView.USE_CONVENTIONS.equals(annotation.name())) {
            return annotation.name();
        } else {
            // derive mapping from classname
            // do not use proxy class names
            Class<?> realBeanClass = ClassUtils.getUserClass(beanClass);
            String mapping = realBeanClass.getSimpleName().replaceFirst(
                    "View$", "");
            return upperCamelToLowerHyphen(mapping);
        }
    }

    public static String upperCamelToLowerHyphen(String string) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            char c = string.charAt(i);
            if (Character.isUpperCase(c)) {
                c = Character.toLowerCase(c);
                if (shouldPrependHyphen(string, i)) {
                    sb.append('-');
                }
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private static boolean shouldPrependHyphen(String string, int i) {
        if (i == 0) {
            // Never put a hyphen at the beginning
            return false;
        } else if (!Character.isUpperCase(string.charAt(i - 1))) {
            // Append if previous char wasn't upper case
            return true;
        } else if (i + 1 < string.length()
                && !Character.isUpperCase(string.charAt(i + 1))) {
            // Append if next char isn't upper case
            return true;
        } else {
            return false;
        }
    }
}
