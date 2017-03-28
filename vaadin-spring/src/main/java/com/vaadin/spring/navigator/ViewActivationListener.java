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
package com.vaadin.spring.navigator;

import java.io.Serializable;
import java.util.EventObject;

import com.vaadin.navigator.ViewChangeListener;

/**
 * Invoked when a view is activated or deactivated. View activation listeners
 * match Spring scope changes better than {@link ViewChangeListener}, and should
 * be used instead of them.
 * <p>
 * On each view change, there are typically two events fired: first a
 * deactivation event for the old view (if any) and then an activation event for
 * the new view.
 *
 * @author lorenzo
 */
public interface ViewActivationListener extends Serializable {

    /**
     * Event received by the listener for attempted and executed view changes.
     */
    @SuppressWarnings("serial")
    public static class ViewActivationEvent extends EventObject {

        private boolean activated;
        private String viewName;

        public ViewActivationEvent(Object source, boolean activated,
                String viewName) {
            super(source);
            this.activated = activated;
            this.viewName = viewName;
        }

        /**
         * Returns <code>true</code> is the view is activated, false if it's
         * deactivated
         *
         * @return
         */
        public boolean isActivated() {
            return activated;
        }

        /**
         * Returns the view name
         *
         * @return
         */
        public String getViewName() {
            return viewName;
        }

    }

    /**
     * Invoked when a view is activated or deactivated.
     *
     * @param event
     */
    void viewActivated(ViewActivationEvent event);

}
