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
package com.vaadin.spring.access;

import com.vaadin.navigator.View;
import com.vaadin.ui.UI;

/**
 * Interface to be implemented by Spring beans that will be consulted by the
 * {@link com.vaadin.spring.navigator.SpringViewProvider} after creating a view
 * instance but before providing it for navigation. If any of the view access
 * controls deny access, the view provider will act like no such view ever
 * existed, or show an
 * {@link com.vaadin.spring.navigator.SpringViewProvider#setAccessDeniedViewClass(Class)
 * access denied view}.
 *
 * Unless contextual information from the view instance is needed, a
 * {@link ViewAccessControl} should be used instead of this interface.
 * {@code ViewAccessControl} beans are called before a view instance is created
 * but can access the annotations on the view bean through the application
 * context. If any {@code ViewAccessControl} denies access to the view, beans
 * implementing this interface are not called for the view.
 *
 * @author Petter Holmström (petter@vaadin.com)
 * @author Henri Sara (hesara@vaadin.com)
 */
public interface ViewInstanceAccessControl {

    /**
     * Checks if the current user has access to the specified view instance and
     * UI. This method is invoked after
     * {@link ViewAccessControl#isAccessGranted(com.vaadin.ui.UI, String)}, when
     * the view instance has already been created, but before it has been
     * returned by the view provider.
     *
     * @param ui
     *            the UI, never {@code null}.
     * @param beanName
     *            the bean name of the view, never {@code null}.
     * @param view
     *            the view instance, never {@code null}.
     * @return true if access is granted, false if access is denied.
     */
    boolean isAccessGranted(UI ui, String beanName, View view);
}