package com.vaadin.flow.spring.security;

import java.security.Principal;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Predicate;

import com.vaadin.flow.di.Lookup;
import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.NavigationAccessChecker;
import com.vaadin.flow.server.auth.NavigationAccessControl;
import com.vaadin.flow.spring.AuthenticationUtil;

/**
 * A Spring specific navigation access control that falls back to Spring
 * mechanisms for user retrieval and role checking, when the generic mechanisms
 * do not work.
 */
public class SpringNavigationAccessControl extends NavigationAccessControl {

    public SpringNavigationAccessControl() {
    }

    public SpringNavigationAccessControl(
            Collection<NavigationAccessChecker> checkerList,
            NavigationAccessChecker.DecisionResolver decisionResolver) {
        super(checkerList, decisionResolver);
    }

    @Override
    protected Principal getPrincipal(VaadinRequest request) {
        if (request == null) {
            return AuthenticationUtil.getSecurityHolderAuthentication();
        }
        return super.getPrincipal(request);
    }

    @Override
    protected Predicate<String> getRolesChecker(VaadinRequest request) {
        if (request == null) {
            return Optional.ofNullable(VaadinService.getCurrent())
                    .map(service -> service.getContext()
                            .getAttribute(Lookup.class))
                    .map(lookup -> lookup.lookup(VaadinRolePrefixHolder.class))
                    .map(VaadinRolePrefixHolder::getRolePrefix)
                    .map(AuthenticationUtil::getSecurityHolderRoleChecker)
                    .orElseGet(
                            AuthenticationUtil::getSecurityHolderRoleChecker)::apply;
        }

        // Update active role prefix if it's not set yet.
        Optional.ofNullable(VaadinService.getCurrent())
                .map(service -> service.getContext().getAttribute(Lookup.class))
                .map(lookup -> lookup.lookup(VaadinRolePrefixHolder.class))
                .filter(prefixHolder -> !prefixHolder.isSet()).ifPresent(
                        prefixHolder -> prefixHolder.resetRolePrefix(request));

        return super.getRolesChecker(request);
    }

}
