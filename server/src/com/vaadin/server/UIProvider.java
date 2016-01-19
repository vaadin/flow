/*
 * Copyright 2000-2014 Vaadin Ltd.
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

package com.vaadin.server;

import java.io.Serializable;
import java.lang.annotation.Annotation;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Title;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.ui.Transport;
import com.vaadin.ui.UI;

public abstract class UIProvider implements Serializable {
    public abstract Class<? extends UI> getUIClass(UIClassSelectionEvent event);

    public UI createInstance(UICreateEvent event) {
        try {
            return event.getUIClass().newInstance();
        } catch (InstantiationException e) {
            throw new RuntimeException("Could not instantiate UI class", e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException("Could not access UI class", e);
        }
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
     * @return an annotation of the given type, or <code>null</code> if the
     *         annotation is not present on the class
     */
    protected static <T extends Annotation> T getAnnotationFor(Class<?> clazz,
            Class<T> annotationType) {
        // Find from the class hierarchy
        Class<?> currentType = clazz;
        while (currentType != Object.class) {
            T annotation = currentType.getAnnotation(annotationType);
            if (annotation != null) {
                return annotation;
            } else {
                currentType = currentType.getSuperclass();
            }
        }

        // Find from an implemented interface
        for (Class<?> iface : clazz.getInterfaces()) {
            T annotation = iface.getAnnotation(annotationType);
            if (annotation != null) {
                return annotation;
            }
        }

        return null;
    }

    public String getPageTitle(UICreateEvent event) {
        Title titleAnnotation = getAnnotationFor(event.getUIClass(),
                Title.class);
        if (titleAnnotation == null) {
            return null;
        } else {
            return titleAnnotation.value();
        }
    }

    /**
     * Finds the {@link PushMode} to use for a specific UI. If no specific push
     * mode is required, <code>null</code> is returned.
     * <p>
     * The default implementation uses the @{@link Push} annotation if it's
     * defined for the UI class.
     * 
     * @param event
     *            the UI create event with information about the UI and the
     *            current request.
     * @return the push mode to use, or <code>null</code> if the default push
     *         mode should be used
     * 
     */
    public PushMode getPushMode(UICreateEvent event) {
        Push push = getAnnotationFor(event.getUIClass(), Push.class);
        if (push == null) {
            return null;
        } else {
            return push.value();
        }
    }

    /**
     * Finds the {@link Transport} to use for a specific UI. If no transport is
     * defined, <code>null</code> is returned.
     * <p>
     * The default implementation uses the @{@link Push} annotation if it's
     * defined for the UI class.
     * 
     * @param event
     *            the UI create event with information about the UI and the
     *            current request.
     * @return the transport type to use, or <code>null</code> if the default
     *         transport type should be used
     */
    public Transport getPushTransport(UICreateEvent event) {
        Push push = getAnnotationFor(event.getUIClass(), Push.class);
        if (push == null) {
            return null;
        } else {
            return push.transport();
        }
    }

}
