package com.vaadin.flow.spring;

import java.security.Principal;
import java.util.function.Function;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.ViewAccessChecker;

/**
 * A Spring specific view access checker that falls back to Spring mechanisms
 * when the generic mechanisms do not work.
 */
public class SpringViewAccessChecker extends ViewAccessChecker {

    /**
     * Creates an instance with the given annotation checker.
     *
     * The created instance is disabled by default.
     *
     * @param accessAnnotationChecker
     *            the annotation checker to use
     */
    public SpringViewAccessChecker(
            AccessAnnotationChecker accessAnnotationChecker) {
        super(accessAnnotationChecker);
    }

    @Override
    protected Principal getPrincipal(VaadinRequest request) {
        if (request == null) {
            return AuthenticationUtil.getSecurityHolderAuthentication();
        }
        return super.getPrincipal(request);
    }

    @Override
    protected Function<String, Boolean> getRolesChecker(VaadinRequest request) {
        if (request == null) {
            return AuthenticationUtil.getSecurityHolderRoleChecker();
        }
        return super.getRolesChecker(request);
    }

}
