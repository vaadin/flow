package com.vaadin.flow.spring.security;

import java.security.Principal;
import java.util.Collection;
import java.util.function.Predicate;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.auth.AccessCheckDecisionResolver;
import com.vaadin.flow.server.auth.AnnotatedViewAccessChecker;
import com.vaadin.flow.server.auth.DefaultAccessCheckDecisionResolver;
import com.vaadin.flow.server.auth.NavigationAccessChecker;
import com.vaadin.flow.server.auth.NavigationAccessControl;

/**
 * A Spring specific navigation access control that falls back to Spring
 * mechanisms for user retrieval and role checking, when the generic mechanisms
 * do not work.
 * <p>
 * </p>
 * In Spring Boot application, a {@link SpringNavigationAccessControl} is
 * provided by default, but its behavior can be configured by defining a
 * {@link NavigationAccessControlConfigurer} bean.
 *
 * @see NavigationAccessControlConfigurer
 */
public class SpringNavigationAccessControl extends NavigationAccessControl {

    /**
     * Create a new instance with the default view annotation checker and
     * decision resolver.
     *
     * @see AnnotatedViewAccessChecker
     * @see DefaultAccessCheckDecisionResolver
     */
    public SpringNavigationAccessControl() {
    }

    /**
     * Create a new instance with given checkers and decision resolver.
     *
     * @param checkerList
     *            collection of navigation access checker.
     * @param decisionResolver
     *            the decision resolver.
     */
    public SpringNavigationAccessControl(
            Collection<NavigationAccessChecker> checkerList,
            AccessCheckDecisionResolver decisionResolver) {
        super(checkerList, decisionResolver);
    }

    @Override
    protected Principal getPrincipal(VaadinRequest request) {
        return SecurityUtil.getPrincipal(request);
    }

    @Override
    protected Predicate<String> getRolesChecker(VaadinRequest request) {
        return SecurityUtil.getRolesChecker(request);
    }

}
