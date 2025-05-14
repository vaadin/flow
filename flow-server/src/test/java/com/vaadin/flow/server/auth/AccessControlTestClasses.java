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

import jakarta.annotation.security.DenyAll;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.ErrorParameter;
import com.vaadin.flow.router.HasErrorParameter;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.router.NotFoundException;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;
import com.vaadin.flow.router.RoutePrefix;
import com.vaadin.flow.router.RouterLayout;

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

    @AccessDeniedErrorRouter(rerouteToError = NotFoundException.class)
    @Route("customaccessdenied")
    public static class CustomAccessDeniedView extends Component {

    }

    @AnonymousAllowed
    @Route("anon")
    @RouteAlias("anon-alias")
    @RouteAlias("anon-alias-wildcard/:path*")
    @RouteAlias("anon-alias-template/:identifier/:category?/resource/:id([0-9]*)")
    public static class AnonymousAllowedView extends Component {
    }

    @AnonymousAllowed
    @Route("anon-url-parameter")
    public static class AnonymousAllowedUrlParameterView extends Component
            implements HasUrlParameter<String> {
        @Override
        public void setParameter(BeforeEvent event, String parameter) {

        }
    }

    @AnonymousAllowed
    @Route("anon-wildcard/:path*")
    public static class AnonymousAllowedWildcardView extends Component {
    }

    @AnonymousAllowed
    @Route("anon-template/:identifier/:category?/resource/:id([0-9]*)")
    @RouteAlias("anon-template-same-params/:identifier/:category?/resource/:id([0-9]*)")
    public static class AnonymousAllowedTemplateView extends Component {
    }

    @Tag(Tag.DIV)
    @RoutePrefix("parent")
    public static class RoutePrefixParent extends Component
            implements RouterLayout {
    }

    @AnonymousAllowed
    @Route(value = "anon-with-parent", layout = RoutePrefixParent.class)
    @RouteAlias(value = "alias-with-parent", layout = RoutePrefixParent.class)
    public static class AnonymousAllowedWithParent extends Component {
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

    @AccessDeniedErrorRouter(rerouteToError = NotFoundException.class)
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

    @Tag(Tag.DIV)
    @AnonymousAllowed
    public static class CustomErrorView
            implements HasErrorParameter<NotFoundException> {

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<NotFoundException> parameter) {
            return 0;
        }
    }

    @Tag(Tag.DIV)
    public static class NotAnnotatedCustomErrorView
            implements HasErrorParameter<NotFoundException> {

        @Override
        public int setErrorParameter(BeforeEnterEvent event,
                ErrorParameter<NotFoundException> parameter) {
            return 0;
        }
    }

    @Tag(Tag.DIV)
    @Layout
    public static class NoAuthLayout extends Component implements RouterLayout {
    }

    @Tag(Tag.DIV)
    @Layout
    @AnonymousAllowed
    public static class AnonymousAllowedLayout extends Component
            implements RouterLayout {
    }

}
