/*
 * Copyright 2000-2026 Vaadin Ltd.
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
package com.vaadin.flow.spring.security;

import java.util.function.Supplier;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.oauth2.client.OAuth2LoginConfigurer;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;

/**
 * Installs {@link KeycloakOidcUserMapper} into the {@link OidcUserService} of a
 * security filter chain.
 * <p>
 * This lives apart from {@link VaadinSecurityConfigurer} on purpose. The
 * {@code spring-security-oauth2-client} dependency that {@link OidcUserService}
 * comes from is optional, and a class is verified as a whole when it is loaded,
 * so a reference to a type of that dependency in
 * {@code VaadinSecurityConfigurer} would break every application that
 * configures Vaadin security without it. Keeping the reference here means the
 * class is only loaded by an application that asks for Keycloak role mapping,
 * which needs the dependency anyway.
 */
final class KeycloakRoleMapping {

    private KeycloakRoleMapping() {
        // Only static utility methods
    }

    /**
     * Makes the given OAuth2 login use an {@link OidcUserService} that maps
     * Keycloak roles.
     *
     * @param loginConfigurer
     *            the OAuth2 login configurer to customize
     * @param http
     *            the security builder to take a shared {@link OidcUserService}
     *            from, and to share the configured one with
     * @param rolePrefix
     *            supplies the role prefix to use, resolved when a user is
     *            mapped
     */
    static void apply(OAuth2LoginConfigurer<HttpSecurity> loginConfigurer,
            HttpSecurity http, Supplier<String> rolePrefix) {
        var oidcUserService = http.getSharedObject(OidcUserService.class);
        if (oidcUserService == null) {
            oidcUserService = new OidcUserService();
        }
        oidcUserService.setOidcUserConverter(
                KeycloakOidcUserMapper.withRolePrefixSupplier(rolePrefix));
        http.setSharedObject(OidcUserService.class, oidcUserService);
        var userService = oidcUserService;
        loginConfigurer.userInfoEndpoint(userInfoEndpoint -> userInfoEndpoint
                .oidcUserService(userService));
    }
}
