/*
 * Copyright 2000-2020 Vaadin Ltd.
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
package com.vaadin.flow.server.auth;

import javax.servlet.http.HttpServletRequest;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.server.VaadinServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks access to views using an {@link AccessAnnotationChecker}.
 * <p>
 * An instance of this class should be added as a {@link BeforeEnterListener} to
 * the {@link com.vaadin.flow.component.UI} of interest.
 */
public class ViewAccessChecker implements BeforeEnterListener {

    public static final String SESSION_STORED_REDIRECT = ViewAccessChecker.class
            .getName() + ".redirect";
    private final AccessAnnotationChecker accessAnnotationChecker;
    private Class<? extends Component> loginView;
    private String loginUrl;
    private boolean enabled = false;

    /**
     * Create an instance.
     */
    public ViewAccessChecker() {
        this(new AccessAnnotationChecker());
    }

    /**
     * Creates an intstance using the given checker.
     * <p>
     * Note that the access checker is disabled by default and can be enabled
     * using {@link #setEnabled(boolean)}. You should also set the login view to
     * use using {@link #setLoginView(Class)} or {@link #setLoginView(String)}
     * 
     * @param accessAnnotationChecker
     *            the checker to use
     */
    protected ViewAccessChecker(
            AccessAnnotationChecker accessAnnotationChecker) {
        this.accessAnnotationChecker = accessAnnotationChecker;
    }

    /**
     * Enables or disables the access checker.
     * 
     * @param enabled
     *            {@code true} to enable, {@code false} to disable
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Checks if the access checker is enabled.
     * 
     * @return {@code true} if enabled, false otherwise
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the Flow login view to use.
     * <p>
     * Note that the access checker needs to be separately enabled using
     * {@link #setEnabled(boolean)}
     * 
     * @param loginView
     *            the Flow view to use as login view
     */
    public void setLoginView(Class<? extends Component> loginView) {
        throwIfEnabled();
        this.loginView = loginView;
    }

    /**
     * Sets the Fusion login view to use.
     * <p>
     * Note that the access checker needs to be separately enabled using
     * {@link #setEnabled(boolean)}
     * 
     * @param loginUrl
     *            the Fusion view to use as login view
     */
    public void setLoginView(String loginUrl) {
        throwIfEnabled();
        this.loginUrl = loginUrl;
    }

    private void throwIfEnabled() {
        if (this.loginUrl != null) {
            throw new IllegalStateException(
                    "Already using " + this.loginUrl + " as the login view");
        }
        if (this.loginView != null) {
            throw new IllegalStateException("Already using "
                    + this.loginView.getName() + " as the login view");
        }
    }

    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        if (!isEnabled()) {
            return;
        }
        Class<?> targetView = beforeEnterEvent.getNavigationTarget();

        VaadinServletRequest vaadinServletRequest = VaadinServletRequest
                .getCurrent();
        if (vaadinServletRequest == null) {
            // This is in a background thread and we cannot access the request
            // to check access
            getLogger().warn("Preventing navigation to " + targetView.getName()
                    + " because no HTTP request is available for checking access.");
            beforeEnterEvent.rerouteToError(NotFoundException.class);
            return;
        }

        HttpServletRequest httpServletRequest = vaadinServletRequest
                .getHttpServletRequest();
        getLogger().debug("Checking access for view {}", targetView.getName());
        if (loginView != null && targetView == loginView) {
            getLogger().debug("Allowing access for login view {}",
                    targetView.getName());
            return;
        }
        boolean ok = accessAnnotationChecker.hasAccess(targetView,
                httpServletRequest);

        if (ok) {
            getLogger().debug("Allowed access to view {}",
                    targetView.getName());
            return;
        } else {
            getLogger().debug("Denied access to view {}", targetView.getName());
            if (httpServletRequest.getUserPrincipal() == null) {
                httpServletRequest.getSession()
                        .setAttribute(SESSION_STORED_REDIRECT, beforeEnterEvent.getLocation().getPathWithQueryParameters());
                if (loginView != null) {
                    beforeEnterEvent.forwardTo(loginView);
                } else {
                    beforeEnterEvent.getUI().getPage().setLocation(loginUrl);
                }
            } else {
                beforeEnterEvent.rerouteToError(NotFoundException.class);
            }
        }
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }
}
