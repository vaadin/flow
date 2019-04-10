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
 *
 */

package com.vaadin.flow.server.frontend;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Extens the default <code>AnnotationValuesExtractor</code> to add Node
 * specific features.
 */
class AnnotationValuesCache extends AnnotationValuesExtractor {

    private Map<String, Map<Class<?>, Set<String>>> annotatedClassesMapCache = new HashMap<>();

    /**
     * Prepares the class to extract annotations with the class finder
     * specified.
     *
     * @param finder
     *            the specific ClassFinder to use
     */
    public AnnotationValuesCache(ClassFinder finder) {
        super(finder);
    }

    @Override
    public Map<Class<?>, Set<String>> getAnnotatedClasses(
            Class<? extends Annotation> annotationClass,
            String valueGetterMethodName) {

        String key = annotationClass.getName() + "#" + valueGetterMethodName;

        Map<Class<?>, Set<String>> value = annotatedClassesMapCache.get(key);

        if (value == null) {
            value = super.getAnnotatedClasses(annotationClass,
                    valueGetterMethodName);
            annotatedClassesMapCache.put(key, value);
        }

        return value;
    }
}
