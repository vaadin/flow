/*
 * Copyright 2000-2019 Vaadin Ltd.
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
package com.vaadin.flow.server.connect.generator;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

/**
 * A set of utility methods used in CCDM generators, so as flow do not depend on
 * external libraries for these operations.
 */
abstract class GeneratorUtils {
    
    static boolean equals(String a, String b) {
        return a == b || a != null && a.equals(b);
    }

    static int compare(String a, String b) {
        return equals(a, b) ? 0 : a == null ? -1 : b == null ? 1 : a.compareTo(b); 
    }

    static String capitalize(String s) {
        return s == null ? s : s.substring(0, 1).toUpperCase() + s.substring(1);
    }

    static boolean isBlank(String cs) {
        return cs == null || cs.replaceAll("\\s+","").isEmpty();
    }

    static boolean isNotBlank(String cs) {
        return !isBlank(cs);
    }

    static String firstNonBlank(String... values) {
        return Arrays.stream(values).filter(s -> isNotBlank(s)).findFirst().orElse(null);
    }

    static boolean isTrue(Boolean bool) {
        return Boolean.TRUE.equals(bool);
    }

    static boolean isNotTrue(Boolean b) {
        return !isTrue(b);
    }

    static <T> T defaultIfNull(T o, T def) {
        return o != null ? o : def;
    }
    
    static String replaceChars(String s, char a, final char b) {
        return s == null ? s : s.replace(a, b);
    }
    
    static boolean contains(String s, String b) {
        return isNotBlank(s) && isNotBlank(b) && s.contains(b);
    }

    static String substringAfter(String s, String p) {
        return contains(s, p) ? s.substring(s.indexOf(p) + p.length()) : "";
    }

    static String substringAfterLast(String s, String p) {
        return contains(s, p) ? s.substring(s.lastIndexOf(p) + p.length()) : "";
    }

    static String substringBeforeLast(String s, String p) {
        return contains(s, p) ? s.substring(0, s.lastIndexOf(p)) : s;
    }

    static boolean endsWith(String s, String p) {
        return contains(s, p) && s.length() == p.length() + s.lastIndexOf(p);
    }

    static String removeEnd(String s, String p) {
        return endsWith(s, p) ? s.substring(0, s.lastIndexOf(p)) : s;
    }

}
