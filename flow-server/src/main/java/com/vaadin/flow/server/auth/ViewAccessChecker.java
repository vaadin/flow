/*
 * Copyright 2000-2022 Vaadin Ltd.
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.server.VaadinServletRequest;

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
     * Creates an instance.
     * <p>
     * Note that the access checker is enabled by default. If this isn't
     * desired, one can use {@link #ViewAccessChecker(boolean)} with {@code
     * enabled=false} and call {@link #enable()} later on whenever appropriate.
     */
    public ViewAccessChecker() {
        this(true);
    }

    /**
     * Creates an instance and enables access checker depending on the given
     * flag.
     * 
     * @param enabled
     *            {@code false} for disabling the access checker, {@code
     * true} for enabling the access checker.
     */
    public ViewAccessChecker(boolean enabled) {
        this(new AccessAnnotationChecker());
        this.enabled = enabled;
    }

    /**
     * Creates an instance using the given checker.
     * <p>
     * Note that the access checker is disabled by default and can be enabled
     * using {@link #enable()}. You should also set the login view to use using
     * {@link #setLoginView(Class)} or {@link #setLoginView(String)}
     * 
     * @param accessAnnotationChecker
     *            the checker to use
     */
    protected ViewAccessChecker(
            AccessAnnotationChecker accessAnnotationChecker) {
        this.accessAnnotationChecker = accessAnnotationChecker;
    }

    /**
     * Enables the access checker.
     * <p>
     * This must be called for the access checker to perform any checks. By
     * default the access checker is disabled.
     */
    public void enable() {
        this.enabled = true;
    }

    /**
     * Sets the Flow login view to use.
     * <p>
     * The login view can only be set once and cannot be changed afterwards.
     * <p>
     * Note that the access checker needs to be separately enabled using
     * {@link #enable()}
     * 
     * @param loginView
     *            the Flow view to use as login view
     */
    public void setLoginView(Class<? extends Component> loginView) {
        throwIfLoginViewSet();
        this.loginView = loginView;
    }

    /**
     * Sets the Fusion login view to use.
     * <p>
     * The login view can only be set once and cannot be changed afterwards.
     * <p>
     * Note that the access checker needs to be separately enabled using
     * {@link #enable()}
     * 
     * @param loginUrl
     *            the Fusion view to use as login view
     */
    public void setLoginView(String loginUrl) {
        throwIfLoginViewSet();
        this.loginUrl = loginUrl;
    }

    private void throwIfLoginViewSet() {
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
        if (!enabled) {
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
        boolean hasAccess = accessAnnotationChecker.hasAccess(targetView,
                httpServletRequest);

        if (hasAccess) {
            getLogger().debug("Allowed access to view {}",
                    targetView.getName());
            return;
        }

        getLogger().debug("Denied access to view {}", targetView.getName());
        if (httpServletRequest.getUserPrincipal() == null) {
            httpServletRequest.getSession()
                    .setAttribute(SESSION_STORED_REDIRECT, beforeEnterEvent
                            .getLocation().getPathWithQueryParameters());
            if (loginView != null) {
                beforeEnterEvent.forwardTo(loginView);
            } else {
                // Prevent the view from being ceated
                beforeEnterEvent.rerouteToError(NotFoundException.class);

                if (loginUrl != null) {
                    beforeEnterEvent.getUI().getPage().setLocation(loginUrl);
                }
            }
        } else if (isProductionMode(beforeEnterEvent)) {
            // Intentionally does not reveal if the route exists
            beforeEnterEvent.rerouteToError(NotFoundException.class);
        } else {
            beforeEnterEvent.rerouteToError(NotFoundException.class,
                    "Access denied");
        }
    }

    private boolean isProductionMode(BeforeEnterEvent beforeEnterEvent) {
        return beforeEnterEvent.getUI().getSession().getConfiguration()
                .isProductionMode();
    }

    private Logger getLogger() {
        return LoggerFactory.getLogger(getClass());
    }
}
