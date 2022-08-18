package com.vaadin.flow.server.auth;

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.router.Route;

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

    @Route("login")
    public static class TestLoginView extends Component {

    }

    @Route("noannotation")
    public static class NoAnnotationView extends Component {

    }

    @AnonymousAllowed
    @Route("anon")
    public static class AnonymousAllowedView extends Component {
    }

    @PermitAll
    @Route("permitall")
    public static class PermitAllView extends Component {
    }

    @DenyAll
    @Route("denyall")
    public static class DenyAllView extends Component {
    }

    @RolesAllowed("user")
    @Route("user")
    public static class RolesAllowedUserView extends Component {
    }

    @RolesAllowed("admin")
    @Route("admin")
    public static class RolesAllowedAdminView extends Component {
    }

    @AnonymousAllowed
    public static class AnonymousAllowedParentView extends Component {
    }

    @Route("no-annotation-anonymous-by-parent")
    public static class NoAnnotationAnonymousAllowedByParentView
            extends AnonymousAllowedParentView {
    }

    @Route("no-annotation-anonymous-by-grandparent")
    public static class NoAnnotationAnonymousAllowedByGrandParentView
            extends NoAnnotationAnonymousAllowedByParentView {
    }

    @PermitAll
    public static class PermitAllGrandParentView {
    }

    public static class PermitAllParentView extends PermitAllGrandParentView {
    }

    @Route("no-annotation-permitall-by-grandparent")
    public static class NoAnnotationPermitAllByGrandParentView
            extends PermitAllParentView {
    }

    @RolesAllowed("user")
    public static class RolesAllowedUserGrandParentView {
    }

    public static class RolesAllowedUserParentView
            extends RolesAllowedUserGrandParentView {
    }

    @Route("no-annotation-roles-allowed-user-by-grandparent")
    public static class NoAnnotationRolesAllowedUserByGrandParentView
            extends RolesAllowedUserParentView {
    }

    @RolesAllowed("admin")
    public static class RolesAllowedAdminGrandParentView {
    }

    public static class RolesAllowedAdminParentView
            extends RolesAllowedAdminGrandParentView {
    }

    @Route("no-annotation-roles-allowed-admin-by-grandparent")
    public static class NoAnnotationRolesAllowedAdminByGrandParentView
            extends RolesAllowedAdminParentView {
    }

    @DenyAll
    public static class DenyAllGrandParentView {
    }

    public static class DenyAllParentView extends DenyAllGrandParentView {
    }

    @Route("no-annotation-denyall-by-grandparent")
    public static class NoAnnotationDenyAllByGrandParentView
            extends DenyAllParentView {
    }

    @AnonymousAllowed
    public interface CustomComponent {
    }

    @Route("no-annotation-denyall-as-interfaces-ignored")
    public static class NoAnnotationDenyAllAsInterfacesIgnoredView
            implements CustomComponent {
    }

    @Route("no-annotation-permitall-from-parent-and-interfaces-ignored")
    public static class NoAnnotationPermitAllByGrandParentAsInterfacesIgnoredView
            extends PermitAllParentView implements CustomComponent {
    }
}
