/*
 * Copyright 2000-2016 Vaadin Ltd.
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

package com.vaadin.annotations;

import java.lang.annotation.Annotation;
import java.util.Optional;

import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.UI;

/**
 * Helper class for reading annotation data.
 *
 */
public class AnnotationReader {

    private AnnotationReader() {
        // Utility class with only static methods
    }

    /**
     * Returns the title for the given class, specified with
     * {@link Title @Title} annotation.
     *
     * @param classWithTitle
     *            the class with the title
     * @return the title or <code>null</code> if no title specified
     */
    public static String getPageTitle(Class<?> classWithTitle) {
        return getAnnotationFor(classWithTitle, Title.class).map(Title::value)
                .orElse(null);
    }

    /**
     * Finds the {@link PushMode} to use for a specific UI. If no specific push
     * mode is required, <code>null</code> is returned.
     *
     * @param uiClass
     *            the UI to search for the Push annotation.
     * @return the push mode to use, or <code>null</code> if no push mode is
     *         defined
     *
     */
    public static PushMode getPushMode(Class<? extends UI> uiClass) {
        return getAnnotationFor(uiClass, Push.class).map(Push::value)
                .orElse(null);
    }

    /**
     * Finds the {@link Transport} to use for a specific UI. If no transport is
     * defined, <code>null</code> is returned.
     *
     * @param uiClass
     *            the UI to search for the Push annotation
     * @return the transport type to use, or <code>null</code> if no transport
     *         is defined
     */
    public static Transport getPushTransport(Class<?> uiClass) {
        return getAnnotationFor(uiClass, Push.class).map(Push::transport)
                .orElse(null);
    }

    /**
     * Helper to get an annotation for a class. If the annotation is not present
     * on the target class, its super classes and implemented interfaces are
     * also searched for the annotation.
     *
     * @param clazz
     *            the class from which the annotation should be found
     * @param annotationType
     *            the annotation type to look for
     * @return an <code>Optional</code> annotation of the given type
     */
    public static <T extends Annotation> Optional<T> getAnnotationFor(
            Class<?> clazz, Class<T> annotationType) {
        // Find from the class hierarchy
        Class<?> currentType = clazz;
        T annotation = null;
        while (currentType != Object.class) {
            annotation = currentType.getAnnotation(annotationType);
            if (annotation != null) {
                return Optional.of(annotation);
            } else {
                currentType = currentType.getSuperclass();
            }
        }

        // Find from an implemented interface
        for (Class<?> iface : clazz.getInterfaces()) {
            annotation = iface.getAnnotation(annotationType);
            if (annotation != null) {
                return Optional.of(annotation);
            }
        }

        return Optional.empty();
    }
}
