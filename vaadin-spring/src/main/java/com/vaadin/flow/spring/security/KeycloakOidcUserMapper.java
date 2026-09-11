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

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserSource;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.StringUtils;

/**
 * Maps Keycloak realm and client roles to Spring Security granted authorities.
 * <p>
 * Keycloak puts the roles of a user into the access token rather than into the
 * ID token, so they are not part of the {@link OidcUser} that
 * {@link OidcUserService} builds by default. This converter decodes the access
 * token and adds
 * <ul>
 * <li>the realm roles from the {@code realm_access} claim,</li>
 * <li>the roles that the {@code resource_access} claim grants for the client id
 * of the current client registration, and</li>
 * <li>the scopes of the access token, prefixed with {@code SCOPE_}.</li>
 * </ul>
 * Realm and client roles both become role authorities, using the role prefix of
 * the application ({@code ROLE_} unless a
 * {@link org.springframework.security.config.core.GrantedAuthorityDefaults}
 * bean says otherwise), so that {@code @RolesAllowed("admin")} and
 * {@code hasRole("admin")} match a Keycloak role named {@code admin}. Roles
 * that {@code resource_access} grants for other clients are ignored.
 * <p>
 * An access token that is not a JWT, or that this application is not allowed to
 * decode, is not an error: the user is mapped without any role authorities, as
 * the default {@link OidcUserService} would do.
 * <p>
 * The recommended way to use this converter is
 * {@link VaadinSecurityConfigurer#keycloakRoleMapping()}, which installs it for
 * a single security filter chain. Applications that build their own
 * {@link OidcUserService} can install it directly:
 *
 * <pre>
 * <code>
 * var oidcUserService = new OidcUserService();
 * oidcUserService.setOidcUserConverter(new KeycloakOidcUserMapper());
 * </code>
 * </pre>
 *
 * @author Vaadin Ltd
 * @since 25.4
 */
public class KeycloakOidcUserMapper
        implements Converter<OidcUserSource, OidcUser> {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(KeycloakOidcUserMapper.class);

    private static final String REALM_ACCESS_CLAIM = "realm_access";

    private static final String RESOURCE_ACCESS_CLAIM = "resource_access";

    private static final String ROLES_CLAIM = "roles";

    private static final String DEFAULT_ROLE_PREFIX = "ROLE_";

    private static final String SCOPE_PREFIX = "SCOPE_";

    private final Supplier<String> rolePrefix;

    private final JwtDecoderFactory<ClientRegistration> decoderFactory;

    private final Map<String, JwtDecoder> decoders = new ConcurrentHashMap<>();

    /**
     * Creates a mapper that prefixes roles with {@code ROLE_}.
     */
    public KeycloakOidcUserMapper() {
        this((String) null);
    }

    /**
     * Creates a mapper that prefixes roles with the given prefix.
     *
     * @param rolePrefix
     *            the prefix to add to a Keycloak role name, or {@code null} to
     *            use {@code ROLE_}
     */
    public KeycloakOidcUserMapper(String rolePrefix) {
        this(() -> rolePrefix, KeycloakOidcUserMapper::createDecoder);
    }

    /**
     * Creates a mapper that looks up the role prefix when it maps a user, for a
     * caller that only knows the prefix after the security filter chain has
     * been configured.
     */
    static KeycloakOidcUserMapper withRolePrefixSupplier(
            Supplier<String> rolePrefix) {
        return new KeycloakOidcUserMapper(rolePrefix,
                KeycloakOidcUserMapper::createDecoder);
    }

    KeycloakOidcUserMapper(Supplier<String> rolePrefix,
            JwtDecoderFactory<ClientRegistration> decoderFactory) {
        this.rolePrefix = rolePrefix != null ? rolePrefix : () -> null;
        this.decoderFactory = decoderFactory;
    }

    @Override
    public OidcUser convert(OidcUserSource userSource) {
        var userRequest = userSource.getUserRequest();
        var userInfo = userSource.getUserInfo();
        var idToken = userRequest.getIdToken();
        var accessToken = userRequest.getAccessToken();
        var clientRegistration = userRequest.getClientRegistration();
        var authorities = new LinkedHashSet<GrantedAuthority>();
        accessToken.getScopes().stream()
                .map(scope -> new SimpleGrantedAuthority(SCOPE_PREFIX + scope))
                .forEach(authorities::add);
        decodeAccessToken(clientRegistration, accessToken.getTokenValue())
                .ifPresent(jwt -> collectRoles(jwt,
                        clientRegistration.getClientId(), authorities));
        var userNameAttributeName = clientRegistration.getProviderDetails()
                .getUserInfoEndpoint().getUserNameAttributeName();
        if (StringUtils.hasText(userNameAttributeName)) {
            authorities.add(new OidcUserAuthority(idToken, userInfo,
                    userNameAttributeName));
            return new DefaultOidcUser(authorities, idToken, userInfo,
                    userNameAttributeName);
        }
        authorities.add(new OidcUserAuthority(idToken, userInfo));
        return new DefaultOidcUser(authorities, idToken, userInfo);
    }

    private void collectRoles(Jwt accessToken, String clientId,
            Set<GrantedAuthority> authorities) {
        var claims = accessToken.getClaims();
        var resourceAccess = asMap(claims.get(RESOURCE_ACCESS_CLAIM));
        Stream.of(asMap(claims.get(REALM_ACCESS_CLAIM)),
                asMap(resourceAccess.get(clientId)))
                .flatMap(access -> extractRoles(access).stream())
                .map(this::toRoleAuthority).forEach(authorities::add);
    }

    /**
     * Decodes the access token with the decoder of the given client
     * registration, which is created on first use and then reused.
     */
    private Optional<Jwt> decodeAccessToken(ClientRegistration registration,
            String tokenValue) {
        try {
            var decoder = decoders.computeIfAbsent(
                    registration.getRegistrationId(),
                    id -> decoderFactory.createDecoder(registration));
            return Optional.of(decoder.decode(tokenValue));
        } catch (JwtException e) {
            LOGGER.debug(
                    "The access token of client registration '{}' could not be "
                            + "decoded as a JWT, so no Keycloak roles are "
                            + "mapped for it",
                    registration.getRegistrationId(), e);
            return Optional.empty();
        }
    }

    private GrantedAuthority toRoleAuthority(String role) {
        return new SimpleGrantedAuthority(rolePrefix() + role);
    }

    /**
     * Returns the role prefix in use, resolved on every call so that a prefix
     * that is only known once the security filter chain is fully configured is
     * picked up.
     */
    String rolePrefix() {
        var prefix = rolePrefix.get();
        return prefix != null ? prefix : DEFAULT_ROLE_PREFIX;
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractRoles(Map<String, Object> access) {
        var roles = access.get(ROLES_CLAIM);
        return roles instanceof List<?> ? (List<String>) roles : List.of();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return value instanceof Map<?, ?> map ? (Map<String, Object>) map
                : Collections.emptyMap();
    }

    private static JwtDecoder createDecoder(ClientRegistration registration) {
        var providerDetails = registration.getProviderDetails();
        var decoder = NimbusJwtDecoder
                .withJwkSetUri(providerDetails.getJwkSetUri()).build();
        decoder.setJwtValidator(JwtValidators
                .createDefaultWithIssuer(providerDetails.getIssuerUri()));
        return decoder;
    }
}
