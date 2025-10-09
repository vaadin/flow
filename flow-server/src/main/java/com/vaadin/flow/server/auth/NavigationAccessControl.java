/*
 * Copyright 2000-2025 Vaadin Ltd.
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

import java.lang.reflect.AnnotatedElement;
import java.security.Principal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.AccessDeniedException;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterListener;
import com.vaadin.flow.router.Location;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.RouteParameters;
import com.vaadin.flow.router.internal.PathUtil;
import com.vaadin.flow.server.Constants;
import com.vaadin.flow.server.HandlerHelper;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.WrappedSession;

/**
 * A {@link BeforeEnterListener} implementation that contains logic to perform
 * access checks before entering a view.
 * <p>
 * Access rules are defined by providing one or more
 * {@link NavigationAccessChecker} instances, that are responsible for deciding
 * if a navigation should be allowed or not. The final navigation grant decision
 * is taken by a pluggable {@link AccessCheckDecisionResolver} component, based
 * on the results of all access checkers.
 * <p>
 * If access is allowed, the navigation continues to the target component.
 * Otherwise, for not authenticated requests, a redirect is performed to the
 * login page (if configured) or to the {@literal not found} error view.
 * <p>
 * In development mode, the access check failure reason is forwarded to the
 * {@literal not found} error view, for debugging purpose. In addition, an
 * exception will be thrown if the decision resolver determines the navigation
 * should be rejected because of misconfigurations.
 * <p>
 * In production mode, for security reasons, the failure message is never
 * exposed and rejection is treated as a normal deny, without any exception
 * being thrown.
 * <p>
 * Before redirecting to the login page, the route path and its absolute URL are
 * stored in the HTTP session, to allow the authentication logic to access the
 * requested resource ( {@link #SESSION_STORED_REDIRECT},
 * {@link #SESSION_STORED_REDIRECT_ABSOLUTE} ).
 * <p>
 * The default constructor create an instance pre-configured with
 * {@link AnnotatedViewAccessChecker}.
 * <p>
 * For internal use only. May be renamed or removed in a future release.
 *
 * @see NavigationAccessChecker
 * @see AccessCheckDecisionResolver
 * @see AccessCheckDecision
 * @see AnnotatedViewAccessChecker
 * @see #setLoginView(String)
 * @see #setLoginView(Class)
 */
public class NavigationAccessControl implements BeforeEnterListener {

    /**
     * Attribute name used to store the route path before redirecting to the
     * login view.
     */
    public static final String SESSION_STORED_REDIRECT = NavigationAccessControl.class
            .getName() + ".redirect";
    /**
     * Attribute name used to store the route absolute URL before redirecting to
     * the login view.
     */
    public static final String SESSION_STORED_REDIRECT_ABSOLUTE = NavigationAccessControl.class
            .getName() + ".redirectAbsolute";

    private final List<NavigationAccessChecker> checkerList;

    private final AccessCheckDecisionResolver decisionResolver;

    private Class<? extends Component> loginView;
    private String loginUrl;

    private boolean enabled = true;

    /**
     * Create a new instance with the default view annotation checker and
     * decision resolver.
     *
     * @see AnnotatedViewAccessChecker
     * @see DefaultAccessCheckDecisionResolver
     */
    public NavigationAccessControl() {
        this(List.of(new AnnotatedViewAccessChecker()));
    }

    /**
     * Create a new instance with given checkers and decision resolver.
     *
     * @param checkerList
     *            collection of navigation access checker.
     * @param decisionResolver
     *            the decision resolver.
     */
    public NavigationAccessControl(
            Collection<NavigationAccessChecker> checkerList,
            AccessCheckDecisionResolver decisionResolver) {
        this.decisionResolver = Objects.requireNonNull(decisionResolver,
                "decision resolver must not be null");
        this.checkerList = List.copyOf(checkerList);
    }

    /**
     * Create a new instance with given checkers and the default decision
     * resolver.
     *
     * @param checkerList
     *            collection of navigation access checker.
     * @see DefaultAccessCheckDecisionResolver
     */
    protected NavigationAccessControl(
            Collection<NavigationAccessChecker> checkerList) {
        this(checkerList, new DefaultAccessCheckDecisionResolver());
    }

    /**
     * Enables or disables the navigation access control.
     *
     * <p>
     * By disabling the access control, navigation to any route is allowed.
     * <p>
     * By default, the access checker is enabled.
     *
     * @param enabled
     *            {@literal true} to enable access control, {@literal false} to
     *            disable the checks and allow navigation to all routes.
     */
    public final void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /**
     * Gets if the access control is enabled or not.
     *
     * @return {@literal true} if the access control is enabled, otherwise
     *         {@literal false}.
     */
    public final boolean isEnabled() {
        return enabled;
    }

    /**
     * Sets the Flow login view to use.
     * <p>
     * The login view can only be set once and cannot be changed afterwards.
     * <p>
     *
     * @param loginView
     *            the Flow view to use as login view
     */
    public final void setLoginView(Class<? extends Component> loginView) {
        if (loginView == this.loginView) {
            // Probably hot reload
            return;
        }
        throwIfLoginViewSet();
        this.loginView = loginView;
    }

    /**
     * Gets the Flow login view.
     *
     * @return the Flow login view, or {@literal null} if not set
     */
    protected Class<? extends Component> getLoginView() {
        return loginView;
    }

    /**
     * Sets the frontend login view to use.
     * <p>
     * The login view can only be set once and cannot be changed afterwards.
     * <p>
     *
     * @param loginUrl
     *            the frontend view to use as login view
     */
    public void setLoginView(String loginUrl) {
        if (Objects.equals(loginUrl, this.loginUrl)) {
            // Probably hot reload
            return;
        }

        throwIfLoginViewSet();
        this.loginUrl = loginUrl;
    }

    /**
     * Gets the frontend login view.
     *
     * @return the frontend login view, or {@literal null} if not set
     */
    protected String getLoginUrl() {
        return loginUrl;
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
    public void beforeEnter(BeforeEnterEvent event) {
        VaadinRequest request = VaadinRequest.getCurrent();

        NavigationContext context = new NavigationContext(event,
                getPrincipal(request), getRolesChecker(request));
        AccessCheckResult result = checkAccess(context,
                isProductionMode(event));
        if (result.decision() != AccessCheckDecision.ALLOW) {
            if (context.getPrincipal() == null) {
                storeRedirectURL(event, request);
                if (loginView != null) {
                    event.forwardTo(loginView, true);
                } else {
                    if (loginUrl != null) {
                        event.forwardToUrl(loginUrl);
                    } else {
                        event.rerouteToError(NotFoundException.class,
                                result.reason());
                    }
                }
            } else {
                event.rerouteToError(
                        getAccessDeniedException(AccessAnnotationChecker
                                .securityTarget(event.getNavigationTarget())),
                        result.reason());
            }
        }
    }

    public AccessCheckResult checkAccess(NavigationContext context,
            boolean productionMode) {
        Class<?> navigationTarget = context.getNavigationTarget();
        if (!enabled) {
            getLogger().trace(
                    "Navigation to view {} allowed because navigation access control is disable",
                    navigationTarget.getName());
            return context.allow();
        }

        if (checkerList.isEmpty()) {
            getLogger().debug(
                    "Navigation to view {} allowed because there are no navigation checkers configured",
                    navigationTarget.getName());
            return context.allow();
        }

        getLogger().debug("Checking access for view {}",
                navigationTarget.getName());
        if (loginView != null && navigationTarget == loginView) {
            getLogger().debug("Allowing access for login view {}",
                    navigationTarget.getName());
            return context.allow();
        } else if (loginUrl != null && PathUtil.trimPath(loginUrl)
                .equals(context.getLocation().getPath())) {
            getLogger().debug("Allowing access for login URL {}", loginUrl);
            return AccessCheckResult.allow();
        }

        List<AccessCheckResult> results = checkerList.stream()
                .map(checker -> checker.check(context)).toList();
        AccessCheckResult decision = decisionResolver.resolve(results, context);
        getLogger().debug("Decision against {} checker results: {}",
                results.size(), decision);

        if (decision.decision() == AccessCheckDecision.REJECT
                && !productionMode) {
            throw new IllegalStateException(decision.reason());
        }
        if (productionMode) {
            // Intentionally do not reveal the real denial reason,
            // for example if the route exists but access is denied for some
            // other reason
            decision = new AccessCheckResult(decision.decision(), "");
        }
        return decision;
    }

    /**
     * Gets the principal for the currently logged in user.
     *
     * @param request
     *            the current request or {@code null} if no request is in
     *            progress (e.g. in a background thread)
     * @return a representation of the currently logged in user or {@code null}
     *         if no user is currently logged in
     */
    protected Principal getPrincipal(VaadinRequest request) {
        if (request == null) {
            return null;
        }
        return request.getUserPrincipal();
    }

    /**
     * Gets a function for checking roles for the currently logged in user.
     *
     * @param request
     *            the current request or {@code null} if no request is in
     *            progress (e.g. in a background thread)
     * @return a function which takes a role name and returns {@code true} if
     *         the user is included in that role
     */
    protected Predicate<String> getRolesChecker(VaadinRequest request) {
        if (request == null) {
            return role -> false;
        }

        return request::isUserInRole;
    }

    /**
     * Gets the URL the client used to make the request.
     *
     * The returned URL contains a protocol, server name, port number, and
     * server path, but it does not include query string parameters.
     *
     * Returns an empty string if the URL cannot be extracted from the current
     * request.
     *
     * @param vaadinRequest
     *            current request
     * @return the URL the client used to make the request
     */
    protected String getRequestURL(VaadinRequest vaadinRequest) {
        if (vaadinRequest instanceof VaadinServletRequest httpRequest) {
            String url = httpRequest.getRequestURL().toString();
            if (HandlerHelper.isRequestType(vaadinRequest,
                    HandlerHelper.RequestType.PUSH)
                    && url.endsWith(Constants.PUSH_MAPPING)) {
                url = url.substring(0, url.indexOf(Constants.PUSH_MAPPING));
            }
            return url;
        }
        return "";
    }

    private void storeRedirectURL(BeforeEnterEvent event,
            VaadinRequest request) {
        WrappedSession session = request != null ? request.getWrappedSession()
                : null;

        if (session != null) {
            String servletHostAndPath = getRequestURL(request);
            String viewPathAndParameters = event.getLocation()
                    .getPathWithQueryParameters();
            session.setAttribute(SESSION_STORED_REDIRECT,
                    viewPathAndParameters);
            session.setAttribute(SESSION_STORED_REDIRECT_ABSOLUTE,
                    servletHostAndPath + viewPathAndParameters);
        } else {
            if (request == null) {
                getLogger().debug(
                        "Unable to store redirect in session because no request is available");
            }
        }
    }

    protected Class<? extends RuntimeException> getAccessDeniedException(
            AnnotatedElement securedClass) {
        if (securedClass.isAnnotationPresent(AccessDeniedErrorRouter.class)) {
            return securedClass.getAnnotation(AccessDeniedErrorRouter.class)
                    .rerouteToError();
        }
        return AccessDeniedException.class;
    }

    private boolean isProductionMode(BeforeEnterEvent beforeEnterEvent) {
        return beforeEnterEvent.getUI().getSession().getConfiguration()
                .isProductionMode();
    }

    private static Logger getLogger() {
        return LoggerFactory.getLogger(NavigationAccessControl.class);
    }

    /**
     * Checks if an access checker of the given type is in use.
     *
     * @return {@code true} if a checker is in use, {@code false} otherwise
     */
    public boolean hasAccessChecker(
            Class<? extends NavigationAccessChecker> type) {
        return checkerList.stream()
                .anyMatch(checker -> type.isAssignableFrom(checker.getClass()));
    }

    /**
     * Creates a new {@link NavigationContext} instance based on the given route
     * data and Vaadin service and request.
     *
     * @param navigationTarget
     *            the navigation target class. Not null.
     * @param path
     *            the path to the navigation target. Not null.
     * @param vaadinService
     *            the Vaadin service. Not null.
     * @param vaadinRequest
     *            the Vaadin request.
     * @return a new navigation context instance.
     */
    public NavigationContext createNavigationContext(Class<?> navigationTarget,
            String path, VaadinService vaadinService,
            VaadinRequest vaadinRequest) {
        Objects.requireNonNull(navigationTarget);
        Objects.requireNonNull(path);
        Objects.requireNonNull(vaadinService);
        return new NavigationContext(vaadinService.getRouter(),
                navigationTarget, new Location(path), RouteParameters.empty(),
                getPrincipal(vaadinRequest), getRolesChecker(vaadinRequest),
                false);
    }
}
