package com.vaadin.flow.spring.security;

import org.springframework.security.core.Authentication;

final class VaadinUserFactory {

    private VaadinUserFactory() {
    }

    static VaadinUser createVaadinUser(Authentication authentication) {
        var principal = authentication.getPrincipal();
        if (principal instanceof VaadinUser vaadinPrincipal) {
            return vaadinPrincipal;
        } else if (isOidcPrincipal(principal)) {
            return new OidcVaadinUser(authentication);
        } else {
            return new BasicVaadinUser(authentication.getName());
        }
    }

    private static boolean isOidcPrincipal(Object principal) {
        // Not all Vaadin applications have the OIDC dependency on their classpath.
        try {
            var claimAccessorClass = Class.forName("org.springframework.security.oauth2.core.oidc.StandardClaimAccessor");
            return claimAccessorClass.isInstance(principal);
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private static class BasicVaadinUser implements VaadinUser {

        private final String userId;

        public BasicVaadinUser(String userId) {
            this.userId = userId;
        }

        @Override
        public String getUserId() {
            return userId;
        }
    }
}
