package com.vaadin.flow.spring;

import java.util.function.Function;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Helpers for authentication related tasks.
 */
public class AuthenticationUtil {

    /**
     * Gets the authenticated user from the Spring SecurityContextHolder.
     *
     * @return the authenticated user or {@code null}
     */
    public static Authentication getSecurityHolderAuthentication() {
        Authentication authentication = SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication instanceof AnonymousAuthenticationToken) {
            return null;
        }

        return authentication;

    }

    /**
     * Gets a function for checking if the authenticated user from the Spring
     * SecurityContextHolder is in a given role. Given role is always prefixed
     * with 'ROLE_'.
     *
     * @return a function for checking if the given user has the given role
     */
    public static Function<String, Boolean> getSecurityHolderRoleChecker() {
        return getSecurityHolderRoleChecker("ROLE_");
    }

    /**
     * Gets a function for checking if the authenticated user from the Spring
     * SecurityContextHolder is in a given role.
     *
     * @param rolePrefix
     *            Prefix for the given role.
     * @return a function for checking if the given user has the given role
     */
    public static Function<String, Boolean> getSecurityHolderRoleChecker(
            String rolePrefix) {
        Authentication authentication = getSecurityHolderAuthentication();
        if (authentication == null) {
            return role -> false;
        }

        return role -> {
            String roleWithPrefix;
            if (rolePrefix != null && role != null
                    && !role.startsWith(rolePrefix)) {
                roleWithPrefix = rolePrefix + role;
            } else {
                roleWithPrefix = role;
            }
            return authentication.getAuthorities().stream()
                    .anyMatch(grantedAuthority -> grantedAuthority
                            .getAuthority().equals(roleWithPrefix));
        };
    }
}
