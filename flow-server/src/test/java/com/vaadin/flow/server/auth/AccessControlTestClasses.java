package com.vaadin.flow.server.auth;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;

public class AccessControlTestClasses {

    public static class NoAnnotationClass {

        public void noAnnotation() {

        }

        @AnonymousAllowed
        public void anonymousAllowed() {

        }

        @PermitAll
        public void permitAll() {

        }

        @DenyAll
        public void denyAll() {

        }

        @RolesAllowed("user")
        public void rolesAllowedUser() {

        }

        @RolesAllowed("admin")
        public void rolesAllowedAdmin() {

        }

        @RolesAllowed({ "user", "admin" })
        public void rolesAllowedUserAdmin() {
        }

    }

    @AnonymousAllowed
    public static class AnonymousAllowedClass {

        public void noAnnotation() {

        }

        @AnonymousAllowed
        public void anonymousAllowed() {

        }

        @PermitAll
        public void permitAll() {

        }

        @DenyAll
        public void denyAll() {

        }

        @RolesAllowed("user")
        public void rolesAllowedUser() {

        }

        @RolesAllowed("admin")
        public void rolesAllowedAdmin() {

        }

        @RolesAllowed({ "user", "admin" })
        public void rolesAllowedUserAdmin() {
        }

    }

    @PermitAll
    public static class PermitAllClass {

        public void noAnnotation() {

        }

        @AnonymousAllowed
        public void anonymousAllowed() {

        }

        @PermitAll
        public void permitAll() {

        }

        @DenyAll
        public void denyAll() {

        }

        @RolesAllowed("user")
        public void rolesAllowedUser() {

        }

        @RolesAllowed("admin")
        public void rolesAllowedAdmin() {

        }

        @RolesAllowed({ "user", "admin" })
        public void rolesAllowedUserAdmin() {
        }

    }

    @DenyAll
    public static class DenyAllClass {

        public void noAnnotation() {

        }

        @AnonymousAllowed
        public void anonymousAllowed() {

        }

        @PermitAll
        public void permitAll() {

        }

        @DenyAll
        public void denyAll() {

        }

        @RolesAllowed("user")
        public void rolesAllowedUser() {

        }

        @RolesAllowed("admin")
        public void rolesAllowedAdmin() {

        }

        @RolesAllowed({ "user", "admin" })
        public void rolesAllowedUserAdmin() {
        }

    }

    @RolesAllowed("user")
    public static class RolesAllowedUserClass {

        public void noAnnotation() {

        }

        @AnonymousAllowed
        public void anonymousAllowed() {

        }

        @PermitAll
        public void permitAll() {

        }

        @DenyAll
        public void denyAll() {

        }

        @RolesAllowed("user")
        public void rolesAllowedUser() {

        }

        @RolesAllowed("admin")
        public void rolesAllowedAdmin() {

        }

        @RolesAllowed({ "user", "admin" })
        public void rolesAllowedUserAdmin() {
        }

    }

    @RolesAllowed("admin")
    public static class RolesAllowedAdminClass {

        public void noAnnotation() {

        }

        @AnonymousAllowed
        public void anonymousAllowed() {

        }

        @PermitAll
        public void permitAll() {

        }

        @DenyAll
        public void denyAll() {

        }

        @RolesAllowed("user")
        public void rolesAllowedUser() {

        }

        @RolesAllowed("admin")
        public void rolesAllowedAdmin() {

        }

        @RolesAllowed({ "user", "admin" })
        public void rolesAllowedUserAdmin() {
        }

    }
}
