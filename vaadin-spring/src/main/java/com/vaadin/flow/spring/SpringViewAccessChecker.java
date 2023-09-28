package com.vaadin.flow.spring;

import java.security.Principal;
import java.util.Optional;
import java.util.function.Function;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import com.vaadin.flow.server.auth.ViewAccessChecker;
import com.vaadin.flow.spring.security.VaadinRolePrefixHolder;

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
            return Optional
                    .ofNullable(VaadinService.getCurrent().getContext()
                            .getAttribute(Lookup.class))
                    .map(lookup -> lookup.lookup(VaadinRolePrefixHolder.class))
                    .map(VaadinRolePrefixHolder::getRolePrefix)
                    .map(AuthenticationUtil::getSecurityHolderRoleChecker)
                    .orElseGet(
                            AuthenticationUtil::getSecurityHolderRoleChecker);
        }

        // Update active role prefix if it's not set yet.
        Optional.ofNullable(VaadinService.getCurrent().getContext()
                .getAttribute(Lookup.class))
                .map(lookup -> lookup.lookup(VaadinRolePrefixHolder.class))
                .filter(prefixHolder -> prefixHolder.getRolePrefix() == null)
                .ifPresent(
                        prefixHolder -> prefixHolder.resetRolePrefix(request));

        return super.getRolesChecker(request);
    }

}
